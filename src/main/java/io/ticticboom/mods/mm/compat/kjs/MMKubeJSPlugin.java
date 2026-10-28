package io.ticticboom.mods.mm.compat.kjs;

import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import io.ticticboom.mods.mm.compat.interop.MMInteropManager;
import io.ticticboom.mods.mm.compat.interop.MMKubeJSInterop;

/**
 * KubeJSPlugin moved package and became an interface in KubeJS 2101, and registerEvents now
 * receives the registry to register into.
 */
public class MMKubeJSPlugin implements KubeJSPlugin {

    public MMKubeJSPlugin() {
        MMInteropManager.setKubeJS(new MMKubeJSInterop());
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        MMKubeEvents.register(registry);
    }
}
