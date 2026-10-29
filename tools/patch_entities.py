"""Entities, mixins and world helpers: one-off and global 1.21.1 patches."""
import pathlib
import re

SRC = pathlib.Path(__file__).resolve().parent.parent / "src"
M = SRC / "main/java/com/iafabric"


def patch(rel, pairs, base=M):
    p = base / rel
    t = p.read_text(encoding="utf-8")
    for old, new in pairs:
        if old not in t:
            print("MISSING in", rel, ":", old[:90])
            continue
        t = t.replace(old, new)
    p.write_text(t, encoding="utf-8")


def write(rel, text, base=M):
    (base / rel).write_text(text, encoding="utf-8")


# --- global ---------------------------------------------------------------------------------------------
GLOBAL = [
    (r"ContainerHelper\.(saveAllItems|loadAllItems)\((\w+), ([\w.()]+)\)", r"ContainerHelper.\1(\2.tag(), \3, \2.registries())"),
    (r"\bnew NearestAttackableTargetGoal\(", "new NearestAttackableTargetGoal<>("),
    (r"(new NearestAttackableTargetGoal<>\([^;]*?)\((\w+),\s*\w+\)\s*->", r"\1\2 ->"),
    (r"TargetingConditions\.Selector (\w+) = \((\w+),\s*\w+\)\s*->", r"java.util.function.Predicate<net.minecraft.world.entity.LivingEntity> \1 = \2 ->"),
    (r"(\w+(?:\(\))?)\.is\((EntityTypeTags\.\w+)\)", r"\1.getType().is(\2)"),
    (r"\.is\(TagKey\.create\(Registries\.ENTITY_TYPE,", ".getType().is(TagKey.create(Registries.ENTITY_TYPE,"),
    (r"this\.dropFromLootTable\((\w+), (\w+), (ResourceKey\.create\([^;]*\))\);", r"com.iafabric.compat.LootDrops.drop(this, \1, \2, \3);"),
    (r"\bnew ServerBossEvent\(\s*(?:java\.util\.)?UUID\.randomUUID\(\),\s*", "new ServerBossEvent("),
    (r"\.walkAnimation\.update\(([^,;]+),([^,;]+),[^,;)]+\)", r".walkAnimation.update(\1,\2)"),
    (r"(\w+(?:\.\w+(?:\(\))?)*)\.getBlockState\((\w+)\)\.isSolidRender\(\)", r"\1.getBlockState(\2).isSolidRender(\1, \2)"),
    (r"(\w+(?:\.\w+\(\))*)\.has\(DataComponents\.BLOCKS_ATTACKS\)", r"(\1.getItem() instanceof net.minecraft.world.item.ShieldItem)"),
    (r"\bPathType\.FIRE_IN_NEIGHBOR\b", "PathType.DANGER_FIRE"),
    (r"\bPathType\.FIRE\b", "PathType.DAMAGE_FIRE"),
    (r"\bClientboundGameEventPacket\.PLAY_ARROW_HIT_SOUND\b", "ClientboundGameEventPacket.ARROW_HIT_PLAYER"),
    (r"\bPushReaction\.IGNORE_ENTITY\b", "PushReaction.IGNORE"),
    (r"\.entityTags\(\)", ".getTags()"),
    (r"\.setInvulnerableTime\((\w+)\)", r".invulnerableTime = \1"),
    (r"\.damageCooldownTime\b", ".invulnerableTime"),
    (r"\bMobSpawnType\.LOAD\b", "MobSpawnType.TRIGGERED"),
    (r"\bthis\.lastHurtByPlayerMemoryTime\b", "this.lastHurtByPlayerTime"),
    (r"\bthis\.getLastHurtByPlayer\(\)", "this.lastHurtByPlayer"),
    (r"\bResourceKey::identifier\b", "ResourceKey::location"),
    (r"\.listElements\(\)", ".holders()"),
    (r"\bnet\.minecraft\.world\.level\.ChunkPos\.unpack\(", "new net.minecraft.world.level.ChunkPos("),
    (r"\bChunkPos\.unpack\(", "new ChunkPos("),
    (r"\.lookupOrThrow\(Registries\.(\w+)\)\.getValue\(", r".registryOrThrow(Registries.\1).get("),
    (r"\bCommands\.hasPermission\(Commands\.LEVEL_GAMEMASTERS\)", "source -> source.hasPermission(2)"),
    (r"new ClickEvent\.SuggestCommand\(", "new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "),
    (r"new ClickEvent\.RunCommand\(", "new ClickEvent(ClickEvent.Action.RUN_COMMAND, "),
    (r"new HoverEvent\.ShowText\(", "new HoverEvent(HoverEvent.Action.SHOW_TEXT, "),
    (r"\bimport net\.minecraft\.world\.entity\.monster\.Enderman;", "import net.minecraft.world.entity.monster.EnderMan;"),
    (r"\bEnderman\b", "EnderMan"),
]
for f in (SRC / "main/java").rglob("*.java"):
    t = f.read_text(encoding="utf-8")
    o = t
    for pat, rep in GLOBAL:
        t = re.sub(pat, rep, t)
    if t != o:
        f.write_text(t, encoding="utf-8")

# --- water breathing is a tag in 1.21.1 (canBreatheUnderwater is final) -----------------------------------
breathers = []
for name in ["EntityDragonSkull", "EntityHippocampus", "EntityIceDragon", "EntityMobSkull", "EntitySeaSerpent", "EntitySiren", "EntityStoneStatue", "EntityMutlipartPart"]:
    p = M / "entity" / f"{name}.java"
    t = p.read_text(encoding="utf-8")
    new = re.sub(r"(?:\s*@Override)?\s*public boolean canBreatheUnderwater\(\)\s*\{\s*return true;\s*\}\n", "\n", t)
    if new != t:
        p.write_text(new, encoding="utf-8")
        breathers.append(name)
print("breathers:", breathers)

# --- one-off ----------------------------------------------------------------------------------------------
patch("entity/props/ChickenProperties.java", [
    ("import net.minecraft.world.entity.animal.chicken.ChickenSoundVariants;\n", ""),
    ("SoundEvents.CHICKEN_SOUNDS.get(ChickenSoundVariants.SoundSet.CLASSIC).adultSounds().hurtSound().value()", "SoundEvents.CHICKEN_HURT"),
])
p = M / "entity/EntityDreadBeast.java"
t = p.read_text(encoding="utf-8")
t = t.replace("import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;\n", "")
for kind, sound in (("growlSound", "WOLF_GROWL"), ("hurtSound", "WOLF_HURT"), ("deathSound", "WOLF_DEATH")):
    t = t.replace(f"SoundEvents.WOLF_SOUNDS.get(WolfSoundVariants.SoundSet.CLASSIC).adultSounds().{kind}().value()", f"SoundEvents.{sound}")
p.write_text(t, encoding="utf-8")

patch("mixin/JigsawStructureAccessor.java", [("	JigsawStructure.MaxDistance iceandfire_fabric$maxDistanceFromCenter();", "	int iceandfire_fabric$maxDistanceFromCenter();")])
patch("world/structure/StructureRoom.java", [
    ("			wide = Math.min(reach.horizontal(), JIGSAW_REACH);\n			down = Math.min(reach.vertical(), JIGSAW_HEIGHT);",
     "			wide = Math.min(reach, JIGSAW_REACH);\n			down = Math.min(reach, JIGSAW_HEIGHT);"),
])

write("mixin/ServerExplosionMixin.java", """package com.iafabric.mixin;

import com.iafabric.boss.CastleWard;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No blast breaks the stones of Cryona's citadel while it holds (CastleWard). */
@Mixin(Explosion.class)
public class ServerExplosionMixin {
	@Shadow
	@Final
	private Level level;
	@Shadow
	@Final
	private double x;
	@Shadow
	@Final
	private double y;
	@Shadow
	@Final
	private double z;
	@Shadow
	@Final
	private ObjectArrayList<BlockPos> toBlow;

	@Inject(method = "explode", at = @At("TAIL"))
	private void iceandfire_fabric$citadelHolds(CallbackInfo ci) {
		if (!(this.level instanceof ServerLevel server) || this.toBlow.isEmpty()) {
			return;
		}
		StructureStart start = CastleWard.citadelAt(server, BlockPos.containing(this.x, this.y, this.z));
		if (start == null || CastleWard.freed(server, start)) {
			return;
		}
		this.toBlow.removeIf(pos -> start.getBoundingBox().isInside(pos));
	}
}
""")
# 1.21.1 servers set a rider's zza/xxa from its input packet themselves
(M / "mixin/ServerPlayerMixin.java").unlink()
mx = SRC / "main/resources/iceandfire_fabric.mixins.json"
mx.write_text(mx.read_text(encoding="utf-8").replace('\t\t"ServerPlayerMixin",\n', ""), encoding="utf-8")

patch("pathfinding/raycoms/ChunkCache.java", [("""    public net.minecraft.world.attribute.EnvironmentAttributeReader environmentAttributes() {
        return this.world.environmentAttributes();
    }
""", "")])

patch("util/EntityUtil.java", [
    ("""		EntityReference<LivingEntity> ref = null;
		if (entity instanceof TamableAnimal tamable) {
			ref = tamable.getOwnerReference();
		} else if (entity instanceof AbstractHorse horse) {
			ref = horse.getOwnerReference();
		} else if (entity instanceof OwnableEntity ownable) {
			LivingEntity owner = ownable.getOwner();
			return owner == null ? null : owner.getUUID();
		}
		return ref == null ? null : ref.getUUID();""", """		if (entity instanceof TamableAnimal tamable) {
			return tamable.getOwnerUUID();
		} else if (entity instanceof AbstractHorse horse) {
			return horse.getOwnerUUID();
		} else if (entity instanceof OwnableEntity ownable) {
			return ownable.getOwnerUUID();
		}
		return null;"""),
    ("""		EntityReference<LivingEntity> ref = uuid == null ? null : EntityReference.of(uuid);
		if (entity instanceof TamableAnimal tamable) {
			tamable.setOwnerReference(ref);
		}""", """		if (entity instanceof TamableAnimal tamable) {
			tamable.setOwnerUUID(uuid);
		}"""),
    ("import net.minecraft.world.entity.EntityReference;\n", ""),
])

patch("world/CustomBiomeFilter.java", [
    ("public class CustomBiomeFilter implements PlacementFilter {", "public class CustomBiomeFilter extends PlacementFilter {"),
    ("    public static final MapCodec<CustomBiomeFilter> CODEC = MapCodec.unit(() -> INSTANCE);",
     "    public static final MapCodec<CustomBiomeFilter> CODEC = MapCodec.unit(() -> INSTANCE);\n    /** Registered by IafWorldRegistry.init. */\n    public static PlacementModifierType<CustomBiomeFilter> TYPE;"),
    ("    public boolean shouldPlace(final PlacementContext context,", "    @Override\n    protected boolean shouldPlace(final PlacementContext context,"),
    ("""    @Override
    public MapCodec<CustomBiomeFilter> codec() {
        return CODEC;
    }""", """    @Override
    public PlacementModifierType<?> type() {
        return TYPE;
    }"""),
    ("import net.minecraft.world.level.levelgen.placement.PlacementModifier;", "import net.minecraft.world.level.levelgen.placement.PlacementModifierType;"),
])
patch("world/IafWorldRegistry.java", [
    ("Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, IafMod.id(\"biome_extended\"), CustomBiomeFilter.CODEC);",
     "CustomBiomeFilter.TYPE = Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, IafMod.id(\"biome_extended\"), () -> CustomBiomeFilter.CODEC);"),
])
patch("entity/EntityDragonBase.java", [
    ("""    @Override
    public void knockback(double strength, double x, double z, DamageSource source, float amount, boolean b) {
        if (!com.iafabric.boss.BossLimits.immovable(this)) {
            super.knockback(strength, x, z);
        }
    }

    @Override
    public void knockback(double strength, double x, double z, DamageSource source, float amount) {""", """    @Override
    public void knockback(double strength, double x, double z) {"""),
])
patch("entity/EntityMutlipartPart.java", [
    ("""    public InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand, Vec3 location) {
        // (the game sends the use of a part to the server itself, where it reaches the body once)
        Entity parent = getParent();
        return parent != null ? parent.interact(player, hand, location) : InteractionResult.PASS;""", """    public InteractionResult interactAt(@NotNull Player player, Vec3 location, @NotNull InteractionHand hand) {
        // (the game sends the use of a part to the server itself, where it reaches the body once)
        Entity parent = getParent();
        return parent != null ? parent.interactAt(player, location, hand) : InteractionResult.PASS;"""),
    ("|| this.isInvulnerableToBase(source);", "|| super.isInvulnerableTo(source);"),
    ("""        UUID id = getParentId();
        return id != null ? level().getEntity(id) : null;""", """        UUID id = getParentId();
        return id != null ? com.iafabric.compat.Entities.byUuid(level(), id) : null;"""),
])
patch("boss/ThroneFight.java", [("Vfx.later(player.level(), 2, () -> sendHome(player, null));", "Vfx.later(player.serverLevel(), 2, () -> sendHome(player, null));")])
patch("entity/util/MyrmexHive.java", [
    ("this.world.getServer().services().nameToIdCache().get(name).map(NameAndId::id)", "this.world.getServer().getProfileCache().get(name).map(com.mojang.authlib.GameProfile::getId)"),
    ("import net.minecraft.server.players.NameAndId;\n", ""),
])
print("done")
