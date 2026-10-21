package com.iafabric.mixin.client;

import com.iafabric.IafConfig;
import com.iafabric.IafMod;
import com.iafabric.entity.props.SirenProperties;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/** A player charmed by a siren sees the world through a pink veil (post_effect/siren.json), like the 1.18 siren shader. */
@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
	private static final Identifier SIREN = IafMod.id("siren");

	@Inject(method = "getActivePostEffects", at = @At("RETURN"), cancellable = true)
	private void iceandfire_fabric$sirenVeil(CallbackInfoReturnable<List<Identifier>> cir) {
		LocalPlayer self = (LocalPlayer) (Object) this;
		if (IafConfig.sirenShader && SirenProperties.isCharmed(self)) {
			List<Identifier> effects = new ArrayList<>(cir.getReturnValue());
			effects.add(SIREN);
			cir.setReturnValue(effects);
		}
	}
}
