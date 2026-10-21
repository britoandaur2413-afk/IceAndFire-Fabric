package com.iafabric.client.render.item;

import com.iafabric.IafMod;
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
import com.iafabric.client.render.tile.RenderPixieHouse;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.item.ItemTrollWeapon;
import com.iafabric.util.ItemNbt;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The items the 1.18 mod drew with a model instead of a picture (troll weapons, death worm gauntlets,
 * the gorgon head, the tide trident in hand, pixie houses, the ghost chest and the dread portal).
 * Item definitions use it as {"type": "minecraft:special", "model": {"type": "iceandfire_fabric:special", "kind": ...}}.
 */
public final class IafSpecialItemRenderer implements SpecialModelRenderer<ItemStack> {
	private static final Identifier GORGON_ACTIVE = Identifier.parse("iceandfire_fabric:textures/models/gorgon/head_active.png");
	private static final Identifier GORGON_INACTIVE = Identifier.parse("iceandfire_fabric:textures/models/gorgon/head_inactive.png");
	private static final SpriteId GHOST_CHEST = new SpriteId(Sheets.CHEST_SHEET, IafMod.id("entity/chest/ghost_chest"));
	private static final ModelTrollWeapon TROLL_WEAPON = new ModelTrollWeapon();
	private static final ModelDeathWormGauntlet GAUNTLET = new ModelDeathWormGauntlet();
	private static final ModelGorgonHead GORGON_HEAD = new ModelGorgonHead();
	private static final ModelGorgonHeadActive GORGON_HEAD_ACTIVE = new ModelGorgonHeadActive();
	private static final ModelTideTrident TRIDENT = new ModelTideTrident();

	private final String kind;
	private final @Nullable ChestModel chest;
	private final SpriteGetter sprites;
	private RenderPixieHouse<?> pixieHouse;

	private IafSpecialItemRenderer(String kind, BakingContext context) {
		this.kind = kind;
		this.sprites = context.sprites();
		this.chest = kind.equals("ghost_chest") ? new ChestModel(context.entityModelSet().bakeLayer(ModelLayers.CHEST)) : null;
	}

	public static void init() {
		SpecialModelRenderers.ID_MAPPER.put(IafMod.id("special"), Unbaked.MAP_CODEC);
	}

	@Override
	public @Nullable ItemStack extractArgument(ItemStack stack) {
		return stack;
	}

	@Override
	public void submit(@Nullable ItemStack stack, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
		if (stack == null) {
			return;
		}
		BufferSource buffers = new BufferSource(collector);
		float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		poseStack.pushPose();
		switch (kind) {
			case "troll_weapon" -> {
				if (stack.getItem() instanceof ItemTrollWeapon weapon) {
					poseStack.translate(0.5F, -0.75F, 0.5F);
					TROLL_WEAPON.renderToBuffer(poseStack, buffers.getBuffer(RenderTypes.entityCutout(weapon.weapon.TEXTURE)), light, overlay, 0xFFFFFFFF);
				}
			}
			case "deathworm_gauntlet" -> {
				Identifier texture = stack.getItem() == IafItemRegistry.DEATHWORM_GAUNTLET_RED ? RenderDeathWorm.TEXTURE_RED
					: stack.getItem() == IafItemRegistry.DEATHWORM_GAUNTLET_WHITE ? RenderDeathWorm.TEXTURE_WHITE : RenderDeathWorm.TEXTURE_YELLOW;
				poseStack.translate(0.5F, 0.5F, 0.5F);
				GAUNTLET.animate(stack, partialTick);
				GAUNTLET.renderToBuffer(poseStack, buffers.getBuffer(RenderTypes.entityCutout(texture)), light, overlay, 0xFFFFFFFF);
			}
			case "gorgon_head" -> {
				boolean active = ItemNbt.getOrEmpty(stack).getBooleanOr("Active", false);
				poseStack.translate(0.5F, active ? 1.5F : 1.25F, 0.5F);
				(active ? GORGON_HEAD_ACTIVE : GORGON_HEAD).renderToBuffer(poseStack,
					buffers.getBuffer(RenderTypes.entityCutout(active ? GORGON_ACTIVE : GORGON_INACTIVE)), light, overlay, 0xFFFFFFFF);
			}
			case "tide_trident" -> {
				TRIDENT.renderToBuffer(poseStack, buffers.getBuffer(hasFoil ? RenderTypes.entitySolidGlint(RenderTideTrident.TRIDENT)
					: RenderTypes.entitySolid(RenderTideTrident.TRIDENT)), light, overlay, 0xFFFFFFFF);
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
				if (chest != null) {
					collector.submitModel(chest, 0.0F, poseStack, light, overlay, -1, GHOST_CHEST, sprites, outlineColor);
				}
			}
			case "dread_portal" -> RenderDreadPortal.renderPortalCube(poseStack, buffers);
			default -> {
			}
		}
		poseStack.popPose();
		buffers.flush();
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		for (float x : new float[]{0, 1}) {
			for (float y : new float[]{0, 1}) {
				for (float z : new float[]{0, 1}) {
					output.accept(new Vector3f(x, y, z));
				}
			}
		}
	}

	public record Unbaked(String kind) implements SpecialModelRenderer.Unbaked<ItemStack> {
		public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("kind").forGetter(Unbaked::kind)).apply(i, Unbaked::new));

		@Override
		public SpecialModelRenderer<ItemStack> bake(BakingContext context) {
			return new IafSpecialItemRenderer(kind, context);
		}

		@Override
		public MapCodec<Unbaked> type() {
			return MAP_CODEC;
		}
	}
}
