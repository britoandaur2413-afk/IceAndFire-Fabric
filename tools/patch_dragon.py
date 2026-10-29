"""EntityDragonBase and friends: one-off 1.21.1 patches, plus global renames found with it."""
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parent.parent / "src"
M = "main/java/com/iafabric/"


def patch(rel, pairs):
    p = ROOT / rel
    t = p.read_text(encoding="utf-8")
    for old, new in pairs:
        if old not in t:
            print("MISSING in", rel, ":", old[:80])
            continue
        t = t.replace(old, new)
    p.write_text(t, encoding="utf-8")


patch(M + "entity/EntityDragonBase.java", [
    ("""    public boolean shouldDropLoot() {
        return getDragonStage() >= 4;
    }""", """    public boolean isOldEnoughToDropLoot() {
        return getDragonStage() >= 4;
    }"""),
    ("""    protected boolean shouldDropLoot() {
    	ServerLevel level = (ServerLevel) this.level();
        return this.shouldDropLoot() && super.shouldDropLoot();""", """    protected boolean shouldDropLoot() {
        return this.isOldEnoughToDropLoot() && super.shouldDropLoot();"""),
    ("if (this.isFood(stack) && this.shouldDropLoot()) {", "if (this.isFood(stack) && this.isOldEnoughToDropLoot()) {"),
    ("""    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult harvest = this.harvestDead(player, hand);
        if (harvest != InteractionResult.PASS) {
            return harvest;
        }
        return super.interact(player, hand, location);""", """    public InteractionResult interactAt(Player player, Vec3 location, InteractionHand hand) {
        InteractionResult harvest = this.harvestDead(player, hand);
        if (harvest != InteractionResult.PASS) {
            return harvest;
        }
        return super.interactAt(player, location, hand);"""),
    ("""    protected void tickHeadTurn(float yBodyRot) {""", """    protected float tickHeadTurn(float yBodyRot, float animStep) {"""),
    ("""                this.yHeadRot = this.getYRot();
            }
            return;
        }
        super.tickHeadTurn(yBodyRot);""", """                this.yHeadRot = this.getYRot();
            }
            return animStep;
        }
        return super.tickHeadTurn(yBodyRot, animStep);"""),
    ("""    public boolean save(ValueOutput compound) {""", """    public boolean save(CompoundTag compound) {"""),
    ("""    @Override
    public void onRemoval(Entity.RemovalReason reason) {""", """    /** When it leaves its level (unloaded or gone): a dragon bound to a summoning crystal is remembered where it was. */
    public void onUnloaded() {"""),
    ("""        super.onRemoval(reason);
""", ""),
    ("Profiler.get()", "this.level().getProfiler()"),
    ("import net.minecraft.util.profiling.Profiler;\n", ""),
])

patch(M + "IafMod.java", [
    ("		LOGGER.info(\"Ice and Fire Fabric loaded\");", """		net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			if (entity instanceof com.iafabric.entity.EntityDragonBase dragon) {
				dragon.onUnloaded();
			}
		});
		LOGGER.info("Ice and Fire Fabric loaded");"""),
])

for f in (ROOT / "main/java").rglob("*.java"):
    t = f.read_text(encoding="utf-8")
    o = t
    t = t.replace("net.minecraft.world.level.new ChunkPos(", "new net.minecraft.world.level.ChunkPos(")
    t = re.sub(r"\bprotected boolean isAlliedTo\(", "public boolean isAlliedTo(", t)
    t = re.sub(r"\bpublic int getRestrictRadius\(\)", "public float getRestrictRadius()", t)
    t = re.sub(r"(boolean killedEntity\(ServerLevel \w+, LivingEntity \w+), DamageSource \w+\)", r"\1)", t)
    t = re.sub(r"\bisLocalInstanceAuthoritative\(\)", "isControlledByLocalInstance()", t)
    if t != o:
        f.write_text(t, encoding="utf-8")
        print("global:", f.name)
print("done")
