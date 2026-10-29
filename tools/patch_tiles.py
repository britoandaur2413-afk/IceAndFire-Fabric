"""Block entity one-off patches for 1.21.1: removal side effects, the dread spawner."""
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parent.parent / "src"


def patch(rel, pairs):
    p = ROOT / rel
    t = p.read_text(encoding="utf-8")
    for old, new in pairs:
        if old not in t:
            print("MISSING in", rel, ":", old[:80])
            continue
        t = t.replace(old, new)
    p.write_text(t, encoding="utf-8")


M = "main/java/com/iafabric/"
# 26.x block entities had preRemoveSideEffects; in 1.21.1 the block's onRemove calls them
for tile, block in (("TileEntityJar", "BlockJar"), ("TileEntityPixieHouse", "BlockPixieHouse")):
    patch(M + f"entity/tile/{tile}.java", [("""    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
""", """    public void onBroken() {
""")])
    p = ROOT / (M + f"block/{block}.java")
    t = p.read_text(encoding="utf-8")
    if "onRemove(" not in t:
        end = t.rstrip().rfind("}")
        t = t[:end].rstrip() + f"""

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {{
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof com.iafabric.entity.tile.{tile} tile) {{
            tile.onBroken();
        }}
        super.onRemove(state, level, pos, newState, movedByPiston);
    }}
}}
"""
        p.write_text(t, encoding="utf-8")

patch(M + "entity/tile/TileEntityDreadSpawner.java", [
    ("""    @Override
    public void setEntityData(TypedEntityData<EntityType<?>> data, RandomSource random) {
        this.spawner.setEntityData(data, this.level, random, this.worldPosition);
        this.setChanged();
    }

""", ""),
    ("import net.minecraft.world.item.component.TypedEntityData;\n", ""),
    ("this.spawner.load(this.level, this.worldPosition, p_155760_);", "this.spawner.load(this.level, this.worldPosition, p_155760_Tag);"),
    ("this.spawner.save(output);", "this.spawner.save(outputTag);"),
])
patch(M + "entity/tile/DreadSpawnerBaseLogic.java", [("public double getOSpin() {", "public double getoSpin() {")])
for f in (ROOT / "client/java").rglob("*.java"):
    t = f.read_text(encoding="utf-8")
    if ".getOSpin()" in t:
        f.write_text(t.replace(".getOSpin()", ".getoSpin()"), encoding="utf-8")
        print("oSpin:", f.name)
print("done")
