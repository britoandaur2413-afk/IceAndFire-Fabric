package com.iafabric.client.render.item;

import com.iafabric.block.BlockPixieHouse;
import com.iafabric.client.model.ModelDeathWormGauntlet;
import com.iafabric.client.model.ModelGorgonHead;
import com.iafabric.client.model.ModelGorgonHeadActive;
import com.iafabric.client.model.ModelTideTrident;
import com.iafabric.client.model.ModelTrollWeapon;
import com.iafabric.client.render.BufferSource;
import com.iafabric.client.render.entity.RenderDeathWorm;
import com.iafabric.client.render.entity.RenderTideTrident;
import com.iafabric.client.render.tile.RenderDreadPortal;
import com.iafabric.client.render.tile.RenderGhostChest;
import com.iafabric.client.render.tile.RenderPixieHouse;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.item.ItemTrollWeapon;
import com.iafabric.util.ItemNbt;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * The items the 1.18 mod drew with a model instead of a picture (troll weapons, death worm gauntlets,
 * the gorgon head, the tide trident in hand, pixie houses, the ghost chest and the dread portal).
 * Their item models have the parent builtin/entity.
 */
public final class IafSpecialItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
	private static final ResourceLocation GORGON_ACTIVE = ResourceLocation.parse("iceandfire_fabric:textures/models/gorgon/head_active.png");
	private static final ResourceLocation GORGON_INACTIVE = ResourceLocation.parse("iceandfire_fabric:textures/models/gorgon/head_inactive.png");
	private static final ModelTrollWeapon TROLL_WEAPON = new ModelTrollWeapon();
	private static final ModelDeathWormGauntlet GAUNTLET = new ModelDeathWormGauntlet();
	private static final ModelGorgonHead GORGON_HEAD = new ModelGorgonHead();
	private static final ModelGorgonHeadActive GORGON_HEAD_ACTIVE = new ModelGorgonHeadActive();
	private static final ModelTideTrident TRIDENT = new ModelTideTrident();
	/** Which items are drawn by which kind. */
	private static final Map<String, List<String>> ITEMS = Map.of(
		"troll_weapon", List.of("troll_weapon_axe", "troll_weapon_column", "troll_weapon_column_forest", "troll_weapon_column_frost", "troll_weapon_hammer",
			"troll_weapon_trunk", "troll_weapon_trunk_frost"),
		"deathworm_gauntlet", List.of("deathworm_gauntlet_red", "deathworm_gauntlet_white", "deathworm_gauntlet_yellow"),
		"gorgon_head", List.of("gorgon_head"),
		"tide_trident", List.of("tide_trident"),
		"pixie_house", List.of("pixie_house_birch", "pixie_house_dark_oak", "pixie_house_mushroom_brown", "pixie_house_mushroom_red", "pixie_house_oak",
			"pixie_house_spruce"),
		"ghost_chest", List.of("ghost_chest"),
		"dread_portal", List.of("dread_portal"));

	private final String kind;
	private ModelPart[] chest;
	private RenderPixieHouse<?> pixieHouse;

	private IafSpecialItemRenderer(String kind) {
		this.kind = kind;
	}

	public static void init() {
		for (Map.Entry<String, List<String>> entry : ITEMS.entrySet()) {
			IafSpecialItemRenderer renderer = new IafSpecialItemRenderer(entry.getKey());
			for (String name : entry.getValue()) {
				Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("iceandfire_fabric", name));
				BuiltinItemRendererRegistry.INSTANCE.register(item, renderer);
			}
		}
	}

	@Override
	public void render(ItemStack stack, ItemDisplayContext mode, PoseStack poseStack, MultiBufferSource multiBuffers, int light, int overlay) {
		BufferSource buffers = new BufferSource(multiBuffers);
		float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
		poseStack.pushPose();
		switch (kind) {
			case "troll_weapon" -> {
				if (stack.getItem() instanceof ItemTrollWeapon weapon) {
					poseStack.translate(0.5F, -0.75F, 0.5F);
					TROLL_WEAPON.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(weapon.weapon.TEXTURE)), light, overlay, 0xFFFFFFFF);
				}
			}
			case "deathworm_gauntlet" -> {
				ResourceLocation texture = stack.getItem() == IafItemRegistry.DEATHWORM_GAUNTLET_RED ? RenderDeathWorm.TEXTURE_RED
					: stack.getItem() == IafItemRegistry.DEATHWORM_GAUNTLET_WHITE ? RenderDeathWorm.TEXTURE_WHITE : RenderDeathWorm.TEXTURE_YELLOW;
				poseStack.translate(0.5F, 0.5F, 0.5F);
				GAUNTLET.animate(stack, partialTick);
				GAUNTLET.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, overlay, 0xFFFFFFFF);
			}
			case "gorgon_head" -> {
				boolean active = ItemNbt.getOrEmpty(stack).iceandfire_fabric$getBooleanOr("Active", false);
				poseStack.translate(0.5F, active ? 1.5F : 1.25F, 0.5F);
				(active ? GORGON_HEAD_ACTIVE : GORGON_HEAD).renderToBuffer(poseStack,
					buffers.getBuffer(RenderType.entityCutoutNoCull(active ? GORGON_ACTIVE : GORGON_INACTIVE)), light, overlay, 0xFFFFFFFF);
			}
			case "tide_trident" -> {
				poseStack.scale(1.0F, -1.0F, -1.0F);
				TRIDENT.renderToBuffer(poseStack, net.minecraft.client.renderer.entity.ItemRenderer.getFoilBufferDirect(multiBuffers,
					RenderType.entitySolid(RenderTideTrident.TRIDENT), false, stack.hasFoil()), light, overlay, 0xFFFFFFFF);
			}
			case "pixie_house" -> {
				if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof BlockPixieHouse) {
					if (pixieHouse == null) {
						pixieHouse = new RenderPixieHouse<>(null);
					}
					pixieHouse.metaOverride = blockItem;
					pixieHouse.render(null, partialTick, poseStack, buffers, light, overlay);
				}
			}
			case "ghost_chest" -> {
				if (chest == null) {
					chest = RenderGhostChest.parts(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.CHEST));
				}
				RenderGhostChest.renderParts(poseStack, RenderGhostChest.GHOST_CHEST.buffer(multiBuffers, RenderType::entityCutoutNoCull), chest, 0.0F, light, overlay);
			}
			case "dread_portal" -> RenderDreadPortal.renderPortalCube(poseStack, buffers);
			default -> {
			}
		}
		poseStack.popPose();
	}
}
