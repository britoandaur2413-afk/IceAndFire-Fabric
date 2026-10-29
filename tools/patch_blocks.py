"""Block methods whose 1.21.1 signatures differ from 26.x (one-off patches)."""
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parent.parent / "src/main/java/com/iafabric"


def patch(rel, pairs, regex=()):
    p = ROOT / rel
    t = p.read_text(encoding="utf-8")
    for old, new in pairs:
        if old not in t:
            print("MISSING in", rel, ":", old[:70])
            continue
        t = t.replace(old, new)
    for pat, rep in regex:
        t, n = re.subn(pat, rep, t)
        if not n:
            print("NO MATCH in", rel, ":", pat[:70])
    p.write_text(t, encoding="utf-8")


UPDATE_OLD = "protected BlockState updateShape(BlockState stateIn, LevelReader worldIn, ScheduledTickAccess tickAccess, BlockPos currentPos, Direction facing, BlockPos facingPos, BlockState facingState, RandomSource random) {"
UPDATE_NEW = "protected BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {"
SUPER_OLD = "super.updateShape(stateIn, worldIn, tickAccess, currentPos, facing, facingPos, facingState, random)"
SUPER_NEW = "super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos)"
for rel in ["block/BlockGoldPile.java", "block/BlockIceSpikes.java", "block/BlockMyrmexBiolight.java", "block/BlockMyrmexConnectedResin.java"]:
    patch(rel, [(UPDATE_OLD, UPDATE_NEW), ("import net.minecraft.world.level.ScheduledTickAccess;", "import net.minecraft.world.level.LevelAccessor;")],
          [(re.escape(SUPER_OLD), SUPER_NEW.replace("\\", "\\\\")), (r"\btickAccess\.", "worldIn.")])

patch("block/BlockIceSpikes.java", [("protected boolean propagatesSkylightDown(@NotNull BlockState state) {",
                                     "protected boolean propagatesSkylightDown(@NotNull BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {")])
for rel in ["block/BlockDreadPortal.java", "block/BlockMyrmexResin.java"]:
    patch(rel, [("public void entityInside(BlockState state, Level worldIn, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean intersects) {",
                 "public void entityInside(BlockState state, Level worldIn, BlockPos pos, Entity entity) {"),
                ("import net.minecraft.world.entity.InsideBlockEffectApplier;\n", "")])
patch("block/BlockDragonforgeInput.java", [
    ("protected void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, @Nullable Orientation orientation, boolean isMoving) {",
     "protected void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {"),
    ("import net.minecraft.world.level.redstone.Orientation;\n", "")])
patch("block/BlockDragonforgeCore.java", [("protected int getAnalogOutputSignal(BlockState blockState, Level worldIn, BlockPos pos, Direction direction) {",
                                           "protected int getAnalogOutputSignal(BlockState blockState, Level worldIn, BlockPos pos) {")])
patch("block/BlockEggInIce.java", [("public void playerDestroy(net.minecraft.server.level.ServerLevel worldIn, net.minecraft.server.level.ServerPlayer player, BlockPos pos, BlockState state, BlockEntity te, ItemStack stack) {",
                                    "public void playerDestroy(Level worldIn, net.minecraft.world.entity.player.Player player, BlockPos pos, BlockState state, BlockEntity te, ItemStack stack) {")])
patch("block/BlockGhostChest.java", [("""        super(() -> IafTileEntityRegistry.GHOST_CHEST, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE,
            IafBlockRegistry.props().mapColor(MapColor.WOOD)
                .strength(2.5F)
                .sound(SoundType.WOOD));""", """        super(IafBlockRegistry.props().mapColor(MapColor.WOOD)
                .strength(2.5F)
                .sound(SoundType.WOOD), () -> IafTileEntityRegistry.GHOST_CHEST);""")])
for rel in ["block/BlockBurntTorchWall.java", "block/BlockDreadTorchWall.java", "block/BlockJar.java"]:
    patch(rel, [], [(r"\.overrideLootTable\((IafBlockRegistry\.\w+)\.getLootTable\(\)\)", r".dropsLike(\1)")])
print("done")
