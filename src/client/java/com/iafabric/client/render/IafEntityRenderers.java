package com.iafabric.client.render;

import com.iafabric.IafMod;
import com.iafabric.citadel.client.model.TabulaModel;
import com.iafabric.client.model.ModelMyrmexQueen;
import com.iafabric.client.model.ModelMyrmexRoyal;
import com.iafabric.client.model.ModelMyrmexSentinel;
import com.iafabric.client.model.ModelMyrmexSoldier;
import com.iafabric.client.model.ModelMyrmexWorker;
import com.iafabric.client.model.animator.FireDragonTabulaModelAnimator;
import com.iafabric.client.model.animator.IceDragonTabulaModelAnimator;
import com.iafabric.client.model.animator.LightningTabulaDragonAnimator;
import com.iafabric.client.model.animator.SeaSerpentTabulaModelAnimator;
import com.iafabric.client.model.util.DragonAnimationsLibrary;
import com.iafabric.client.model.util.EnumDragonModelTypes;
import com.iafabric.client.model.util.EnumDragonPoses;
import com.iafabric.client.model.util.EnumSeaSerpentAnimations;
import com.iafabric.client.model.util.TabulaModelHandlerHelper;
import com.iafabric.client.render.entity.*;
import com.iafabric.client.render.tile.*;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.entity.tile.IafTileEntityRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/** The renderers of the mod's entities and block entities. */
public final class IafEntityRenderers {
	private static final Set<EntityType<?>> REGISTERED = new HashSet<>();

	public static TabulaModel FIRE_DRAGON_BASE_MODEL;
	public static TabulaModel ICE_DRAGON_BASE_MODEL;
	public static TabulaModel LIGHTNING_DRAGON_BASE_MODEL;
	public static TabulaModel SEA_SERPENT_BASE_MODEL;

	private IafEntityRenderers() {
	}

	public static void init() {
		DragonAnimationsLibrary.register(EnumDragonPoses.values(), EnumDragonModelTypes.values());
		EnumSeaSerpentAnimations.initializeSerpentModels();
		try {
			FIRE_DRAGON_BASE_MODEL = new TabulaModel(TabulaModelHandlerHelper.loadTabulaModel("/assets/iceandfire_fabric/models/tabula/firedragon/firedragon_ground"), new FireDragonTabulaModelAnimator());
			ICE_DRAGON_BASE_MODEL = new TabulaModel(TabulaModelHandlerHelper.loadTabulaModel("/assets/iceandfire_fabric/models/tabula/icedragon/icedragon_ground"), new IceDragonTabulaModelAnimator());
			LIGHTNING_DRAGON_BASE_MODEL = new TabulaModel(TabulaModelHandlerHelper.loadTabulaModel("/assets/iceandfire_fabric/models/tabula/lightningdragon/lightningdragon_ground"), new LightningTabulaDragonAnimator());
			SEA_SERPENT_BASE_MODEL = new TabulaModel(TabulaModelHandlerHelper.loadTabulaModel("/assets/iceandfire_fabric/models/tabula/seaserpent/seaserpent"), new SeaSerpentTabulaModelAnimator());
		} catch (IOException e) {
			throw new IllegalStateException("Couldn't load the Tabula models", e);
		}
		register(IafEntityRegistry.FIRE_DRAGON, context -> new RenderDragonBase(context, FIRE_DRAGON_BASE_MODEL, 0));
		register(IafEntityRegistry.ICE_DRAGON, context -> new RenderDragonBase(context, ICE_DRAGON_BASE_MODEL, 1));
		register(IafEntityRegistry.LIGHTNING_DRAGON, context -> new RenderLightningDragon(context, LIGHTNING_DRAGON_BASE_MODEL, 2));
		register(IafEntityRegistry.DRAGON_EGG, RenderDragonEgg::new);
		register(IafEntityRegistry.DRAGON_ARROW, RenderDragonArrow::new);
		register(IafEntityRegistry.DRAGON_SKULL, context -> new RenderDragonSkull(context, FIRE_DRAGON_BASE_MODEL, ICE_DRAGON_BASE_MODEL, LIGHTNING_DRAGON_BASE_MODEL));
		register(IafEntityRegistry.FIRE_DRAGON_CHARGE, context -> new RenderDragonFireCharge(context, true));
		register(IafEntityRegistry.ICE_DRAGON_CHARGE, context -> new RenderDragonFireCharge(context, false));
		register(IafEntityRegistry.LIGHTNING_DRAGON_CHARGE, RenderDragonLightningCharge::new);
		register(IafEntityRegistry.HIPPOGRYPH_EGG, ThrownItemRenderer::new);
		register(IafEntityRegistry.HIPPOGRYPH, RenderHippogryph::new);
		register(IafEntityRegistry.STONE_STATUE, RenderStoneStatue::new);
		register(IafEntityRegistry.GORGON, RenderGorgon::new);
		register(IafEntityRegistry.PIXIE, RenderPixie::new);
		register(IafEntityRegistry.EXILED_PIXIE, com.iafabric.client.render.entity.RenderExiledPixie::new);
		register(IafEntityRegistry.CYCLOPS, RenderCyclops::new);
		register(IafEntityRegistry.SIREN, RenderSiren::new);
		register(IafEntityRegistry.HIPPOCAMPUS, RenderHippocampus::new);
		register(IafEntityRegistry.DEATH_WORM, RenderDeathWorm::new);
		register(IafEntityRegistry.DEATH_WORM_EGG, ThrownItemRenderer::new);
		register(IafEntityRegistry.COCKATRICE, RenderCockatrice::new);
		register(IafEntityRegistry.COCKATRICE_EGG, ThrownItemRenderer::new);
		register(IafEntityRegistry.STYMPHALIAN_BIRD, RenderStymphalianBird::new);
		register(IafEntityRegistry.STYMPHALIAN_FEATHER, RenderStymphalianFeather::new);
		register(IafEntityRegistry.STYMPHALIAN_ARROW, RenderStymphalianArrow::new);
		register(IafEntityRegistry.TROLL, RenderTroll::new);
		register(IafEntityRegistry.MYRMEX_WORKER, context -> new RenderMyrmexBase(context, new ModelMyrmexWorker(), 0.5F));
		register(IafEntityRegistry.MYRMEX_SOLDIER, context -> new RenderMyrmexBase(context, new ModelMyrmexSoldier(), 0.75F));
		register(IafEntityRegistry.MYRMEX_QUEEN, context -> new RenderMyrmexBase(context, new ModelMyrmexQueen(), 1.25F));
		register(IafEntityRegistry.MYRMEX_EGG, RenderMyrmexEgg::new);
		register(IafEntityRegistry.MYRMEX_SENTINEL, context -> new RenderMyrmexBase(context, new ModelMyrmexSentinel(), 0.85F));
		register(IafEntityRegistry.MYRMEX_ROYAL, context -> new RenderMyrmexBase(context, new ModelMyrmexRoyal(), 0.75F));
		register(IafEntityRegistry.MYRMEX_SWARMER, context -> new RenderMyrmexBase(context, new ModelMyrmexRoyal(), 0.25F));
		register(IafEntityRegistry.AMPHITHERE, RenderAmphithere::new);
		register(IafEntityRegistry.AMPHITHERE_ARROW, RenderAmphithereArrow::new);
		register(IafEntityRegistry.SEA_SERPENT, context -> new RenderSeaSerpent(context, SEA_SERPENT_BASE_MODEL));
		register(IafEntityRegistry.SEA_SERPENT_BUBBLES, RenderNothing::new);
		register(IafEntityRegistry.SEA_SERPENT_ARROW, RenderSeaSerpentArrow::new);
		register(IafEntityRegistry.CHAIN_TIE, RenderChainTie::new);
		register(IafEntityRegistry.PIXIE_CHARGE, RenderNothing::new);
		register(IafEntityRegistry.TIDE_TRIDENT, RenderTideTrident::new);
		register(IafEntityRegistry.MOB_SKULL, context -> new RenderMobSkull(context, SEA_SERPENT_BASE_MODEL));
		register(IafEntityRegistry.DREAD_SCUTTLER, RenderDreadScuttler::new);
		register(IafEntityRegistry.DREAD_GHOUL, RenderDreadGhoul::new);
		register(IafEntityRegistry.DREAD_BEAST, RenderDreadBeast::new);
		register(IafEntityRegistry.DREAD_THRALL, RenderDreadThrall::new);
		register(IafEntityRegistry.DREAD_LICH, RenderDreadLich::new);
		register(IafEntityRegistry.DREAD_LICH_SKULL, RenderDreadLichSkull::new);
		register(IafEntityRegistry.DREAD_KNIGHT, RenderDreadKnight::new);
		register(IafEntityRegistry.DREAD_HORSE, RenderDreadHorse::new);
		register(IafEntityRegistry.DREAD_QUEEN, RenderDreadQueen::new);
		register(IafEntityRegistry.CRYONA_ASLEEP, RenderCryonaAsleep::new);
		register(IafEntityRegistry.BLACK_FROST_GUARD, context -> new RenderDragonBase(context, ICE_DRAGON_BASE_MODEL, 1));
		register(IafEntityRegistry.HYDRA, RenderHydra::new);
		register(IafEntityRegistry.HYDRA_BREATH, RenderNothing::new);
		register(IafEntityRegistry.HYDRA_ARROW, RenderHydraArrow::new);
		register(IafEntityRegistry.SLOW_MULTIPART, RenderPart::new);
		register(IafEntityRegistry.DRAGON_MULTIPART, RenderPart::new);
		register(IafEntityRegistry.VFX, com.iafabric.client.render.entity.RenderVfx::new);
		register(IafEntityRegistry.CYCLOPS_MULTIPART, RenderPart::new);
		register(IafEntityRegistry.HYDRA_MULTIPART, RenderPart::new);
		register(IafEntityRegistry.GHOST, RenderGhost::new);
		register(IafEntityRegistry.GHOST_SWORD, RenderGhostSword::new);
		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			if (IafMod.MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace()) && !REGISTERED.contains(type)) {
				IafMod.LOGGER.warn("No renderer for {}", BuiltInRegistries.ENTITY_TYPE.getKey(type));
				EntityRendererRegistry.register(type, NoopRenderer::new);
			}
		}

		BlockEntityRendererRegistry.register(IafTileEntityRegistry.PODIUM, RenderPodium::new);
		BlockEntityRendererRegistry.register(IafTileEntityRegistry.IAF_LECTERN, RenderLectern::new);
		BlockEntityRendererRegistry.register(IafTileEntityRegistry.EGG_IN_ICE, RenderEggInIce::new);
		BlockEntityRendererRegistry.register(IafTileEntityRegistry.PIXIE_HOUSE, RenderPixieHouse::new);
		BlockEntityRendererRegistry.register(IafTileEntityRegistry.PIXIE_JAR, RenderJar::new);
		BlockEntityRendererRegistry.register(IafTileEntityRegistry.DREAD_PORTAL, RenderDreadPortal::new);
		BlockEntityRendererRegistry.register(IafTileEntityRegistry.DREAD_SPAWNER, RenderDreadSpawner::new);
		BlockEntityRendererRegistry.register(IafTileEntityRegistry.GHOST_CHEST, RenderGhostChest::new);
	}

	private static <E extends Entity> void register(EntityType<? extends E> type, EntityRendererProvider<E> provider) {
		REGISTERED.add(type);
		EntityRendererRegistry.register(type, provider);
	}
}
