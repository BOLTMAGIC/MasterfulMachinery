package io.ticticboom.mods.mm.compat.claim;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Runtime-guarded provider for FTBChunks claim checks.
 * Tries several possible API entrypoints and method names used across versions (including 1.20.1 builds).
 * Returns Boolean.TRUE / Boolean.FALSE when decisive, or null when neutral or on error so fallback can apply.
 */
public class FTBChunksClaimProvider implements IClaimPermissionProvider {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    private final Object apiInstance;
    private final Method isClaimedMethod;
    private final Method getClaimMethod;
    private final Method claimGetOwnerMethod;
    private final Method claimIsTeamMemberMethod;
    private final Constructor<?> chunkPosCtor; // if available

    public static boolean isAvailable() {
        return ModList.get().isLoaded("ftbchunks");
    }

    public FTBChunksClaimProvider() throws Exception {
        Object api = null;
        Method mIsClaimed = null;
        Method mGetClaim = null;
        Method mClaimGetOwner = null;
        Method mClaimIsTeam = null;
        Constructor<?> cChunkPos = null;

        // Try common possible API classes
        String[] apiClassNames = new String[]{
                "dev.ftb.mods.ftbchunks.FTBChunksAPI",
                "dev.ftb.mods.ftbchunks.api.FTBChunksAPI",
                "com.feed_the_beast.ftbchunks.FTBChunksAPI",
                "dev.ftb.mods.ftbchunks.FTBChunks"
        };

        for (String clsName : apiClassNames) {
            try {
                Class<?> apiClass = Class.forName(clsName);
                // try static getManager / getInstance
                Method getManager = null;
                try {
                    getManager = apiClass.getMethod("getManager");
                } catch (NoSuchMethodException ignored) {}
                if (getManager == null) {
                    try {
                        getManager = apiClass.getMethod("getInstance");
                    } catch (NoSuchMethodException ignored) {}
                }
                Object manager = null;
                if (getManager != null) {
                    manager = getManager.invoke(null);
                } else {
                    // maybe apiClass itself is the manager
                    manager = apiClass;
                }

                if (manager != null) {
                    // Try common manager->getClaimService / getClaimManager
                    Method getClaimService = null;
                    try {
                        getClaimService = manager.getClass().getMethod("getClaimService");
                    } catch (NoSuchMethodException ignored) {}
                    try {
                        if (getClaimService == null) getClaimService = manager.getClass().getMethod("getClaimManager");
                    } catch (NoSuchMethodException ignored) {}

                    Object claimService = null;
                    if (getClaimService != null) {
                        claimService = getClaimService.invoke(manager);
                    }

                    // Try common methods on claimService: isClaimed(chunkX, chunkZ) or isClaimed(world, x, z) or getClaimAt(chunkX, chunkZ)
                    if (claimService != null) {
                        Method isClaimed = null;
                        Method getClaim = null;
                        // try various signatures
                        for (Method m : claimService.getClass().getMethods()) {
                            String name = m.getName().toLowerCase();
                            var params = m.getParameterTypes();
                            if (isClaimed == null && (name.contains("isclaimed") || name.contains("ischunkclaimed") || name.contains("isclaimedat"))) {
                                // accept signatures like (int,int) or (ChunkPos) or (level,chunkpos)
                                if (params.length == 2 && (params[0] == int.class || params[0] == long.class)) isClaimed = m;
                                if (params.length == 1) isClaimed = m; // might accept a chunkpos object
                                if (params.length == 3) isClaimed = m; // e.g., (level,x,z)
                            }
                            if (getClaim == null && (name.contains("getclaim") || name.contains("getclaimat") || name.contains("getclaimatpos"))) {
                                if (params.length == 2 && (params[0] == int.class || params[0] == long.class)) getClaim = m;
                                if (params.length == 1) getClaim = m;
                                if (params.length == 3) getClaim = m;
                            }
                        }

                        // assign whichever found
                        mIsClaimed = isClaimed;
                        mGetClaim = getClaim;

                        if (mGetClaim != null) {
                            try {
                                Object sample = null;
                                try { sample = mGetClaim.invoke(claimService, 0, 0); } catch (Throwable ignore) {}
                                if (sample == null) try { sample = mGetClaim.invoke(claimService, 0, 0, 0); } catch (Throwable ignore) {}
                                if (sample != null) {
                                    for (Method cm : sample.getClass().getMethods()) {
                                        String cn = cm.getName().toLowerCase();
                                        if (mClaimGetOwner == null && (cn.contains("getowner") || cn.contains("owner") || cn.contains("getownerid"))) mClaimGetOwner = cm;
                                        if (mClaimIsTeam == null && (cn.contains("isteam") || cn.contains("isplayerinteam") || cn.contains("isteammember") || (cn.contains("is") && cn.contains("member")))) mClaimIsTeam = cm;
                                    }
                                }
                            } catch (Throwable ignored) {}
                        }

                        // try to find ChunkPos constructor to use when methods expect a chunk-pos object
                        try {
                            Class<?> chunkPosClass = Class.forName("net.minecraft.world.level.ChunkPos");
                            cChunkPos = chunkPosClass.getConstructor(int.class, int.class);
                        } catch (Throwable ignored) {}

                        if (mIsClaimed != null || mGetClaim != null) {
                            api = claimService;
                            break;
                        }
                    }
                }
            } catch (ClassNotFoundException ignored) {
                // try next
            }
        }

        this.apiInstance = api;
        this.isClaimedMethod = mIsClaimed;
        this.getClaimMethod = mGetClaim;
        this.claimGetOwnerMethod = mClaimGetOwner;
        this.claimIsTeamMemberMethod = mClaimIsTeam;
        this.chunkPosCtor = cChunkPos;

        LOGGER.info("FTBChunksClaimProvider reflection init: api={}, isClaimed={}, getClaim={}, ownerMethod={}, teamMethod={}, chunkPosCtor={}",
                apiInstance != null, isClaimedMethod != null, getClaimMethod != null, claimGetOwnerMethod != null, claimIsTeamMemberMethod != null, chunkPosCtor != null);
    }

    @Override
    public Boolean canModify(ServerPlayer player, BlockPos pos) {
        if (!isAvailable()) return null;
        try {
            if (apiInstance == null) return null;

            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;

            // Try isClaimed first
            if (isClaimedMethod != null) {
                try {
                    Object res;
                    var params = isClaimedMethod.getParameterTypes();
                    if (params.length == 1 && chunkPosCtor != null) {
                        Object cp = chunkPosCtor.newInstance(chunkX, chunkZ);
                        res = isClaimedMethod.invoke(apiInstance, cp);
                    } else if (params.length == 2) {
                        res = isClaimedMethod.invoke(apiInstance, chunkX, chunkZ);
                    } else if (params.length == 3) {
                        // attempt null level param + x + z
                        res = isClaimedMethod.invoke(apiInstance, null, chunkX, chunkZ);
                    } else {
                        res = isClaimedMethod.invoke(apiInstance, chunkX, chunkZ);
                    }
                    if (res instanceof Boolean) {
                        boolean claimed = (Boolean) res;
                        if (!claimed) return null;
                    }
                } catch (Throwable ignore) {}
            }

            Object claimObj = null;
            if (getClaimMethod != null) {
                try {
                    var params = getClaimMethod.getParameterTypes();
                    if (params.length == 1 && chunkPosCtor != null) {
                        Object cp = chunkPosCtor.newInstance(chunkX, chunkZ);
                        claimObj = getClaimMethod.invoke(apiInstance, cp);
                    } else if (params.length == 2) {
                        claimObj = getClaimMethod.invoke(apiInstance, chunkX, chunkZ);
                    } else if (params.length == 3) {
                        claimObj = getClaimMethod.invoke(apiInstance, null, chunkX, chunkZ);
                    } else {
                        claimObj = getClaimMethod.invoke(apiInstance, chunkX, chunkZ);
                    }
                } catch (Throwable ignore) {}
            }

            if (claimObj == null) {
                return null;
            }

            if (claimGetOwnerMethod != null) {
                try {
                    Object owner = claimGetOwnerMethod.invoke(claimObj);
                    if (owner != null) {
                        String ownerName = owner.toString();
                        if (ownerName.equalsIgnoreCase(player.getName().getString())) return true;
                    }
                } catch (Throwable ignored) {}
            }

            if (claimIsTeamMemberMethod != null) {
                try {
                    try {
                        Object mres = claimIsTeamMemberMethod.invoke(claimObj, player);
                        if (mres instanceof Boolean && (Boolean) mres) return true;
                    } catch (IllegalArgumentException ia) {
                        try {
                            Object mres = claimIsTeamMemberMethod.invoke(claimObj, player.getName().getString());
                            if (mres instanceof Boolean && (Boolean) mres) return true;
                        } catch (Throwable ignored) {}
                    }
                } catch (Throwable ignored) {}
            }

            return false;
        } catch (Throwable t) {
            LOGGER.debug("FTBChunks provider failed via reflection", t);
            return null;
        }
    }
}
