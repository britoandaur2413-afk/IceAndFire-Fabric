package com.iafabric.test;

/** IAF_ONLY=pixie (or riding, scenes, world; several with commas) runs only those client tests, to try a change faster. */
final class IafTestOnly {
	private IafTestOnly() {
	}

	static boolean skip(String name) {
		String only = System.getenv("IAF_ONLY");
		return only != null && !only.isBlank() && !java.util.Arrays.asList(only.split(",")).contains(name);
	}
}
