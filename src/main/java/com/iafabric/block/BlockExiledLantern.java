package com.iafabric.block;

import com.iafabric.IafMod;
import com.iafabric.enums.EnumParticles;
import com.iafabric.pixie.ExiledVariants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * An exiled pixie's lantern: a crystal in her color on a mossy stone, in the middle of a ring of candles of her color
 * (her mark where a quest leads: {@link com.iafabric.pixie.FairyCheckpoints}). Lit, it shines and her dust rises
 * from it; once its place was reached it stays as a quiet mark, its crystal grey.
 */
public class BlockExiledLantern extends Block implements INoTab {
	public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, ExiledVariants.COUNT - 1);
	public static final BooleanProperty LIT = BooleanProperty.create("lit");
	private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 14, 13);

	public BlockExiledLantern() {
		super(IafBlockRegistry.props().mapColor(MapColor.QUARTZ)
			.strength(0.6F, 6F)
			.sound(SoundType.AMETHYST)
			.noOcclusion()
			.lightLevel(state -> state.getValue(LIT) ? 14 : 6));
		this.registerDefaultState(this.getStateDefinition().any().setValue(VARIANT, 0).setValue(LIT, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(VARIANT, LIT);
	}

	@Override
	protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
		return SHAPE;
	}

	/** Lit: her dust rising from the crystal and now and then a spark up into the air; out: a speck now and then. */
	@Override
	public void animateTick(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull RandomSource random) {
		float[] dust = ExiledVariants.dust(state.getValue(VARIANT));
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.75;
		double z = pos.getZ() + 0.5;
		if (!state.getValue(LIT)) {
			if (random.nextInt(4) == 0) {
				IafMod.PROXY.spawnParticle(EnumParticles.If_Pixie, x + random.nextGaussian() * 0.2, y, z + random.nextGaussian() * 0.2, dust[0], dust[1], dust[2]);
			}
			return;
		}
		for (int i = 0; i < 2; i++) {
			IafMod.PROXY.spawnParticle(EnumParticles.If_Pixie, x + random.nextGaussian() * 0.25, y + random.nextFloat() * 0.6, z + random.nextGaussian() * 0.25,
				dust[0], dust[1], dust[2]);
		}
		if (random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.END_ROD, x, y + 0.4, z, random.nextGaussian() * 0.01, 0.06 + random.nextFloat() * 0.05, random.nextGaussian() * 0.01);
		}
	}

	@Override
	public boolean shouldBeInTab() {
		return false;
	}
}
