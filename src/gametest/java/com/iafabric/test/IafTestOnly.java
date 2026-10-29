package com.iafabric.test;

/**
 * gradlew runClienttest -Ponly=pixie (or riding, scenes, world; several with commas; IAF_ONLY too) runs only those
 * client tests, to try a change faster.
 */
final class IafTestOnly {
	private IafTestOnly() {
	}

	static boolean skip(String name) {
		String only = System.getProperty("iceandfire_fabric.test.only", "");
		if (only.isBlank()) {
			only = System.getenv("IAF_ONLY");
		}
		return only != null && !only.isBlank() && !java.util.Arrays.asList(only.split(",")).contains(name);
	}
}
