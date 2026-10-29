package com.iafabric.pixie;

import com.iafabric.IafMod;
import com.iafabric.entity.EntityPixie;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.inventory.PixieMenu;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.message.MessagePixieBondSync;
import com.iafabric.network.IafNetwork;
import com.iafabric.util.EntityNbt;
import com.iafabric.util.EntityUtil;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pixie bonds: a player bonds one tamed pixie of each color and has one of them out at a time. The pixie out gives
 * its element's effect (levels 1 to 3) or stats (levels 4 and 5), cannot be hurt (its player takes a part of the
 * damage, less as it grows, and none of the kind its orb blocks; its shield takes some first) and grows with what
 * its player kills near it and with pixie dust offered at an altar.
 */
public final class PixieBonding {
	public static final AttachmentType<PixieBonds> BONDS = AttachmentRegistry.create(IafMod.stored("pixie_bonds"), builder -> builder
		.persistent(CompoundTag.CODEC.xmap(PixieBonds::fromTag, PixieBonds::toTag))
		.copyOnDeath()
		.initializer(PixieBonds::new));
	public static final ResourceKey<DamageType> BOND_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, IafMod.id("pixie_bond"));
	/** How near its player a pixie out has to be to give its gifts and to grow. */
	private static final double NEAR = 32;
	/** Experience a pixie dust gives at an altar. */
	public static final int DUST_XP = 25;
	private static final Map<UUID, Long> LAST_HIT = new HashMap<>();

	private PixieBonding() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(PixieBonding::tick);
		ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
			if (source.getEntity() instanceof ServerPlayer player && !(victim instanceof EntityPixie)) {
				EntityPixie out = activePixie(player);
				if (out != null && out.distanceTo(player) <= NEAR) {
					addXp(player, get(player).active, Math.max(1, Math.min(50, (int) (victim.getMaxHealth() / 2))));
				}
			}
		});
		// on the server thread, before the player is saved (a connection closes on the network thread)
		ServerPlayerEvents.LEAVE.register(player -> rest(player, true));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			PixieBonds bonds = get(handler.player);
			if (bonds.active >= 0) {
				summon(handler.player, bonds.active);
			}
		});
	}

	public static PixieBonds get(ServerPlayer player) {
		return player.getAttachedOrCreate(BONDS);
	}

	private static void save(ServerPlayer player, PixieBonds bonds) {
		player.setAttached(BONDS, bonds);
	}

	public static void sync(ServerPlayer player) {
		IafNetwork.sendToPlayer(new MessagePixieBondSync(get(player).toTag()), player);
	}

	/** The pixie out with the player, if it is in the player's world. */
	public static EntityPixie activePixie(ServerPlayer player) {
		PixieBonds bonds = get(player);
		if (bonds.active < 0 || bonds.activeId == null) {
			return null;
		}
		Entity entity = player.level().getEntity(bonds.activeId);
		return entity instanceof EntityPixie pixie && pixie.isAlive() ? pixie : null;
	}

	/** Bonds the nearest tamed pixie of that color: it becomes the one out. */
	public static boolean bond(ServerPlayer player, int color) {
		PixieBonds bonds = get(player);
		PixieBonds.Bond bond = bonds.get(color);
		if (bond.bonded) {
			return false;
		}
		EntityPixie found = null;
		for (EntityPixie pixie : player.level().getEntitiesOfClass(EntityPixie.class, player.getBoundingBox().inflate(16),
			p -> p.isAlive() && p.isTame() && p.isOwnedBy(player) && p.getColor() == color && !p.isBonded() && p.oracleTicks < 0
				&& !(p instanceof com.iafabric.entity.EntityExiledPixie))) {
			if (found == null || pixie.distanceTo(player) < found.distanceTo(player)) {
				found = pixie;
			}
		}
		if (found == null) {
			return false;
		}
		rest(player, false);
		bonds = get(player);
		bond = bonds.get(color);
		bond.bonded = true;
		bond.level = 1;
		bond.xp = 0;
		bond.shield = bond.maxShield();
		bonds.active = color;
		bonds.activeId = found.getUUID();
		bonds.token = player.getRandom().nextLong();
		found.setBonded(true);
		found.bondToken = bonds.token;
		found.standUp();
		found.setPersistenceRequired();
		save(player, bonds);
		player.sendSystemMessage(Component.translatable("iceandfire_fabric.pixie_bond.bonded", found.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
		sparkle((ServerLevel) player.level(), found, 20);
		sync(player);
		ExiledTalk.bonded(player);
		return true;
	}

	/** The bonded pixie of that color comes out (the one out goes to rest). */
	public static boolean summon(ServerPlayer player, int color) {
		PixieBonds bonds = get(player);
		PixieBonds.Bond bond = bonds.get(color);
		if (!bond.bonded) {
			return false;
		}
		if (bonds.active == color && activePixie(player) != null) {
			return true;
		}
		rest(player, false);
		bonds = get(player);
		bond = bonds.get(color);
		ServerLevel level = (ServerLevel) player.level();
		EntityPixie pixie = IafEntityRegistry.PIXIE.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (pixie == null) {
			return false;
		}
		if (!bond.pixie.isEmpty()) {
			CompoundTag tag = bond.pixie.copy();
			tag.remove("UUID");
			tag.remove("Pos");
			tag.remove("Motion");
			EntityNbt.load(pixie, tag);
		}
		pixie.setColor(color);
		pixie.setTame(true, true);
		EntityUtil.setOwnerUUID(pixie, player.getUUID());
		// out again it follows its player, even if it was sitting when it went to rest
		pixie.standUp();
		bonds.token = player.getRandom().nextLong();
		pixie.setBonded(true);
		pixie.bondToken = bonds.token;
		pixie.setPersistenceRequired();
		// behind their head: it flies into sight, it does not appear in their face
		double yaw = Math.toRadians(player.getYRot());
		pixie.snapTo(player.getX() + Math.sin(yaw) * 1.4, player.getEyeY() + 0.5, player.getZ() - Math.cos(yaw) * 1.4, player.getYRot(), 0);
		level.addFreshEntity(pixie);
		bonds.active = color;
		bonds.activeId = pixie.getUUID();
		save(player, bonds);
		sparkle(level, pixie, 15);
		sync(player);
		return true;
	}

	/**
	 * The pixie out goes to rest (it is kept as it is, to come out again). When the player only leaves the game it
	 * stays the one to come out when they come back.
	 */
	public static void rest(ServerPlayer player, boolean leaving) {
		PixieBonds bonds = get(player);
		if (bonds.active < 0) {
			return;
		}
		EntityPixie pixie = activePixie(player);
		if (pixie != null) {
			bonds.get(bonds.active).pixie = EntityNbt.save(pixie);
			sparkle((ServerLevel) player.level(), pixie, 10);
			pixie.discard();
		}
		bonds.token = player.getRandom().nextLong();
		bonds.activeId = null;
		if (!leaving) {
			bonds.active = -1;
		}
		save(player, bonds);
		if (!leaving) {
			sync(player);
		}
	}

	/** Opens the menu of the bonded pixie of that color (its orbs, things to give it, its abilities). */
	public static void openMenu(ServerPlayer player, int color) {
		PixieBonds.Bond bond = get(player).get(color);
		if (!bond.bonded) {
			return;
		}
		Component title = Component.translatable("gui.iceandfire_fabric.pixie_menu", Component.translatable("pixie.iceandfire_fabric.element." + color));
		player.openMenu(new ExtendedMenuProvider<Integer>() {
			@Override
			public Integer getScreenOpeningData(ServerPlayer opener) {
				return color;
			}

			@Override
			public Component getDisplayName() {
				return title;
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory inventory, Player opener) {
				return new PixieMenu(id, inventory, color, orbContainer(player, color));
			}
		});
	}

	/** The pixie's orb slots as a container: what is put in or taken out is kept in its bond at once. */
	private static SimpleContainer orbContainer(ServerPlayer player, int color) {
		PixieBonds.Bond bond = get(player).get(color);
		SimpleContainer orbs = new SimpleContainer(bond.orbs.length) {
			@Override
			public void setChanged() {
				super.setChanged();
				PixieBonds bonds = get(player);
				PixieBonds.Bond kept = bonds.get(color);
				for (int i = 0; i < kept.orbs.length; i++) {
					kept.orbs[i] = this.getItem(i).copy();
				}
				save(player, bonds);
				sync(player);
			}
		};
		for (int i = 0; i < bond.orbs.length; i++) {
			orbs.getItems().set(i, bond.orbs[i].copy());
		}
		return orbs;
	}

	/** Pixie dust (experience) or a thing that teaches a pixie a spell. */
	public static boolean canFeed(ItemStack stack) {
		return stack.is(IafItemRegistry.PIXIE_DUST) || PixieAbility.teachesAny(stack.getItem());
	}

	/** Gives the pixie what is in the menu's slot: all the pixie dust, or one thing that teaches it an ability. */
	public static void feed(ServerPlayer player, int color, Container slot) {
		ItemStack stack = slot.getItem(0);
		PixieBonds bonds = get(player);
		PixieBonds.Bond bond = bonds.get(color);
		if (stack.isEmpty() || !bond.bonded) {
			return;
		}
		if (stack.is(IafItemRegistry.PIXIE_DUST)) {
			// only what it still needs to reach its last level: the rest stays in the slot
			int needed = dustToMax(bond);
			if (needed <= 0) {
				player.sendOverlayMessage(Component.translatable("iceandfire_fabric.pixie_bond.max").withStyle(ChatFormatting.GOLD));
				return;
			}
			int count = Math.min(stack.getCount(), needed);
			stack.shrink(count);
			slot.setItem(0, stack.isEmpty() ? ItemStack.EMPTY : stack);
			slot.setChanged();
			player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1F, 1.4F);
			addXp(player, color, count * DUST_XP);
			player.sendOverlayMessage(Component.translatable("iceandfire_fabric.pixie_bond.fed", count, count * DUST_XP).withStyle(ChatFormatting.LIGHT_PURPLE));
			return;
		}
		PixieAbility ability = PixieAbility.taughtBy(color, stack.getItem());
		if (ability == null) {
			// it teaches a pixie of another color
			for (int other = 0; other < PixieBonds.COLORS; other++) {
				if (PixieAbility.taughtBy(other, stack.getItem()) != null) {
					player.sendOverlayMessage(Component.translatable("iceandfire_fabric.pixie_bond.other_color", Component.translatable("pixie.iceandfire_fabric.element." + other))
						.withStyle(ChatFormatting.GRAY));
				}
			}
			return;
		}
		Component name = Component.translatable("pixie.iceandfire_fabric.ability." + ability.name);
		if (bond.knows(ability)) {
			player.sendOverlayMessage(Component.translatable("iceandfire_fabric.pixie_bond.knows", name).withStyle(ChatFormatting.GRAY));
			return;
		}
		stack.shrink(1);
		slot.setChanged();
		bond.known |= ability.bit();
		if (bond.selected() == null) {
			bond.spell = ability.ordinal();
		}
		save(player, bonds);
		sync(player);
		player.level().playSound(null, player.blockPosition(), SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.PLAYERS, 1F, 1.2F);
		player.sendSystemMessage(Component.translatable("iceandfire_fabric.pixie_bond.learnt", Component.translatable("pixie.iceandfire_fabric.element." + color), name)
			.withStyle(ChatFormatting.LIGHT_PURPLE));
		EntityPixie pixie = activePixie(player);
		if (pixie != null && pixie.getColor() == color) {
			sparkle((ServerLevel) player.level(), pixie, 25);
		}
	}

	/** Chooses the spell the magic key calls (one of its color's that it knows). */
	public static void selectSpell(ServerPlayer player, int color, int id) {
		if (id < 0 || id >= PixieAbility.values().length) {
			return;
		}
		PixieAbility ability = PixieAbility.of(id);
		PixieBonds bonds = get(player);
		PixieBonds.Bond bond = bonds.get(color);
		if (!bond.bonded || ability.support() || ability.color != color) {
			return;
		}
		if (!bond.knows(ability)) {
			player.sendOverlayMessage(Component.translatable("iceandfire_fabric.pixie_bond.not_learnt", Component.translatable("pixie.iceandfire_fabric.ability." + ability.name),
				ability.teacher().getHoverName()).withStyle(ChatFormatting.GRAY));
			return;
		}
		bond.spell = ability.ordinal();
		save(player, bonds);
		sync(player);
	}

	/** How much pixie dust a pixie still takes to reach its last level (0 at it). */
	/**
	 * For tests and game masters: the pixie of that color bonded at its last level, all its spells known (its first on
	 * the magic key), its shield full, and out with the player (in place of another one out).
	 */
	public static void grantMaxBond(ServerPlayer player, int color) {
		PixieBonds bonds = get(player);
		PixieBonds.Bond bond = bonds.get(color);
		bond.bonded = true;
		bond.level = 5;
		bond.xp = Math.max(bond.xp, PixieBonds.LEVEL_XP[PixieBonds.LEVEL_XP.length - 1]);
		for (PixieAbility spell : PixieAbility.spellsOf(color)) {
			bond.known |= spell.bit();
		}
		PixieAbility selected = bond.selected();
		if (selected == null || selected.color != Math.floorMod(color, PixieBonds.COLORS)) {
			bond.spell = PixieAbility.spellsOf(color).get(0).ordinal();
		}
		bond.shield = bond.maxShield();
		save(player, bonds);
		summon(player, color);
		sync(player);
	}

	public static int dustToMax(PixieBonds.Bond bond) {
		int missing = PixieBonds.LEVEL_XP[PixieBonds.LEVEL_XP.length - 1] - bond.xp;
		return bond.level >= 5 || missing <= 0 ? 0 : (missing + DUST_XP - 1) / DUST_XP;
	}

	/** Experience for the bonded pixie of that color, with its levels. */
	public static void addXp(ServerPlayer player, int color, int amount) {
		PixieBonds bonds = get(player);
		PixieBonds.Bond bond = bonds.get(color);
		if (!bond.bonded || amount <= 0) {
			return;
		}
		bond.xp += amount;
		while (bond.level < 5 && bond.xp >= PixieBonds.LEVEL_XP[bond.level - 1]) {
			bond.level++;
			bond.shield = bond.maxShield();
			player.sendSystemMessage(Component.translatable("iceandfire_fabric.pixie_bond.level_up",
				Component.translatable("pixie.iceandfire_fabric.element." + color), bond.level).withStyle(ChatFormatting.LIGHT_PURPLE));
			player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.6F);
			EntityPixie pixie = activePixie(player);
			if (pixie != null) {
				sparkle((ServerLevel) player.level(), pixie, 30);
			}
		}
		save(player, bonds);
		sync(player);
	}

	/**
	 * A bonded pixie hit: it takes nothing; its player takes a part of it (none of the kind its orb blocks), after
	 * what its shield takes.
	 */
	public static void onBondedPixieHurt(EntityPixie pixie, ServerLevel level, DamageSource source, float amount) {
		if (!(pixie.getOwner() instanceof ServerPlayer owner) || source.getEntity() == owner) {
			return;
		}
		PixieBonds bonds = get(owner);
		PixieBonds.Bond bond = bonds.get(pixie.getColor());
		if (!bond.bonded) {
			return;
		}
		if (bond.blocks(OrbKind.of(source))) {
			level.sendParticles(ParticleTypes.ENCHANT, pixie.getX(), pixie.getY() + 0.3, pixie.getZ(), 8, 0.2, 0.2, 0.2, 0.2);
			return;
		}
		float share = amount * bond.share();
		float absorbed = Math.min(bond.shield, share);
		bond.shield -= absorbed;
		share -= absorbed;
		LAST_HIT.put(owner.getUUID(), level.getGameTime());
		if (absorbed > 0) {
			level.sendParticles(ParticleTypes.END_ROD, pixie.getX(), pixie.getY() + 0.3, pixie.getZ(), 6, 0.25, 0.25, 0.25, 0.02);
		}
		save(owner, bonds);
		if (share > 0.01F) {
			Entity attacker = source.getEntity();
			owner.hurtServer(level, new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(BOND_DAMAGE), pixie, attacker), share);
		}
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % 5 == 0) {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				EntityPixie out = activePixie(player);
				PixieBonds bonds = get(player);
				if (out != null && bonds.active >= 0 && player.isAlive() && !player.isSpectator()) {
					PixieCombat.tick(player, out, bonds.get(bonds.active));
				}
			}
		}
		if (server.getTickCount() % 20 != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (FairyVision.inMemory(player)) {
				// (her exiled pixie's memory is hers: the pixie out waits where it was, its gifts as they were)
				continue;
			}
			PixieBonds bonds = get(player);
			EntityPixie out = activePixie(player);
			if (bonds.active >= 0 && (out == null || out.level() != player.level() || out.distanceTo(player) > 64)) {
				// lost on the way (a new world, a far place): it comes back to its player
				summon(player, bonds.active);
				out = activePixie(player);
			}
			boolean near = out != null && out.distanceTo(player) <= NEAR;
			gifts(player, bonds, near);
			if (bonds.active >= 0) {
				PixieBonds.Bond bond = bonds.get(bonds.active);
				long lastHit = LAST_HIT.getOrDefault(player.getUUID(), 0L);
				if (bond.shield < bond.maxShield() && player.level().getGameTime() - lastHit > 60) {
					bond.shield = Math.min(bond.maxShield(), bond.shield + 0.2F);
				}
			}
		}
	}

	/** The pixie out's gifts: its effect (levels 1 to 3) or its stats (4 and 5); the stats of the others go. */
	private static void gifts(ServerPlayer player, PixieBonds bonds, boolean near) {
		for (int color = 0; color < PixieBonds.COLORS; color++) {
			PixieElement element = PixieElement.of(color);
			PixieBonds.Bond bond = bonds.get(color);
			boolean giving = near && bonds.active == color && bond.bonded;
			int amplifier = PixieElement.amplifier(bond.level);
			if (giving && amplifier >= 0) {
				player.addEffect(new MobEffectInstance(element.effect, 60, amplifier, true, false, true));
			}
			for (PixieElement.Bonus bonus : element.bonuses) {
				AttributeInstance instance = player.getAttribute(bonus.attribute());
				if (instance == null) {
					continue;
				}
				Identifier id = IafMod.id("pixie_bond_" + color + "_" + bonus.attribute().unwrapKey().map(k -> k.identifier().getPath()).orElse("stat"));
				double value = giving ? bonus.at(bond.level) : 0;
				AttributeModifier current = instance.getModifier(id);
				if (value == 0) {
					if (current != null) {
						instance.removeModifier(id);
					}
				} else if (current == null || current.amount() != value) {
					instance.addOrUpdateTransientModifier(new AttributeModifier(id, value, bonus.operation()));
				}
			}
		}
	}

	static void sparkle(ServerLevel level, LivingEntity pixie, int count) {
		level.sendParticles(ParticleTypes.END_ROD, pixie.getX(), pixie.getY() + 0.3, pixie.getZ(), count, 0.3, 0.3, 0.3, 0.03);
	}
}
