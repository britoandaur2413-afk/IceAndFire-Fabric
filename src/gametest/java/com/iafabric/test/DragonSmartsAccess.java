package com.iafabric.test;

import com.iafabric.entity.DragonSmarts;
import com.iafabric.entity.EntityDragonBase;

final class DragonSmartsAccess {
	private DragonSmartsAccess() {
	}

	static boolean underRoof(EntityDragonBase dragon) {
		return DragonSmarts.isUnderRoof(dragon);
	}
}
