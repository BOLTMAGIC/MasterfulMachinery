package io.ticticboom.mods.mm.port;

public interface IPortStorageModel {
	/**
	 * Optional numeric rank for the storage model used for compatibility checks.
	 * Default is 0 for models that don't declare a tier.
	 * <p>
	 * Higher numbers indicate "higher tier" and can be accepted where a lower
	 * minimum tier is required.
	 */
	default int getTierRank() {
		return 0;
	}
}
