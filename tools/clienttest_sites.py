"""Hand fixes for the client tests where 1.21.1 calls differ from 26.x (runs again without harm)."""
import pathlib
import re

D = pathlib.Path(__file__).resolve().parent.parent / "src/gametest/java/com/iafabric/test"

GENERAL = [
    (r"\.entityTags\(\)", ".getTags()"),
    (r"\b(level|realm|world|s\.overworld\(\))\.getMinY\(\)", r"\1.getMinBuildHeight()"),
    (r"\b(level|realm|world|s\.overworld\(\))\.getMaxY\(\)", r"\1.getMaxBuildHeight()"),
    (r"\bsound\.getIdentifier\(\)", "sound.getLocation()"),
    (r"\bmc\.gui\.hud\.getBossOverlay\(\)", "mc.gui.getBossOverlay()"),
    (r"mc -> mc\.gui\.hud\.toggle\(\)\)", "mc -> mc.options.hideGui = !mc.options.hideGui)"),
    (r"BuiltInRegistries\.(\w+)\.getValue\(", r"BuiltInRegistries.\1.get("),
    (r"\bregistry\.getValue\(", "registry.get("),
    (r"\.getCooldowns\(\)\.isOnCooldown\(mc\.player\.getMainHandItem\(\)\)", ".getCooldowns().isOnCooldown(mc.player.getMainHandItem().getItem())"),
    (r"\b(HALL|CAVERN)\.x\(\)", r"\1.x"), (r"\b(HALL|CAVERN)\.z\(\)", r"\1.z"),
    (r"\.getChunkPos\(\)\.x\(\)", ".getChunkPos().x"), (r"\.getChunkPos\(\)\.z\(\)", ".getChunkPos().z"),
    (r"(\w+)\.interact\((\w+), (?:net\.minecraft\.world\.)?InteractionHand\.MAIN_HAND, \1\.position\(\)\)", r"IafClick.click(\1, \2)"),
    # entity hitboxes: 1.21.1 has them on the render dispatcher, not as a debug screen entry
    (r"mc\.debugEntries\.setStatus\(net\.minecraft\.client\.gui\.components\.debug\.DebugScreenEntries\.ENTITY_HITBOXES,\s*"
     r"net\.minecraft\.client\.gui\.components\.debug\.DebugScreenEntryStatus\.ALWAYS_ON\)",
     "mc.getEntityRenderDispatcher().setRenderHitBoxes(true)"),
    (r"mc\.debugEntries\.setStatus\(net\.minecraft\.client\.gui\.components\.debug\.DebugScreenEntries\.ENTITY_HITBOXES,\s*"
     r"net\.minecraft\.client\.gui\.components\.debug\.DebugScreenEntryStatus\.NEVER\)",
     "mc.getEntityRenderDispatcher().setRenderHitBoxes(false)"),
    (r"mc\.debugEntries\.isCurrentlyEnabled\(net\.minecraft\.client\.gui\.components\.debug\.DebugScreenEntries\.ENTITY_HITBOXES\)",
     "mc.getEntityRenderDispatcher().shouldRenderHitBoxes()"),
    (r"\.shouldRender\(part, frustum, camera\.x, camera\.y, camera\.z, 1F\)", ".shouldRender(part, frustum, camera.x, camera.y, camera.z)"),
    (r"var dragon = mc\.level\.getEntity\(made\[0\]\);", "var dragon = com.iafabric.compat.Entities.byUuid(mc.level, made[0]);"),
    (r"mc\.gameMode\.interact\(mc\.player, queen, new net\.minecraft\.world\.phys\.EntityHitResult\(queen\), net\.minecraft\.world\.InteractionHand\.MAIN_HAND\);",
     "if (!mc.gameMode.interactAt(mc.player, queen, new net.minecraft.world.phys.EntityHitResult(queen), net.minecraft.world.InteractionHand.MAIN_HAND).consumesAction()) {\n"
     "\t\t\t\t\t\tmc.gameMode.interact(mc.player, queen, net.minecraft.world.InteractionHand.MAIN_HAND);\n\t\t\t\t\t}"),
    (r"net\.minecraft\.world\.level\.block\.Blocks\.WOOL\.pick\(", "IafClick.wool("),
]

for f in sorted(D.glob("*.java")):
    t = f.read_text(encoding="utf-8")
    o = t
    for a, b in GENERAL:
        t = re.sub(a, b, t)
    if t != o:
        f.write_text(t, encoding="utf-8")
        print("sites:", f.name)
