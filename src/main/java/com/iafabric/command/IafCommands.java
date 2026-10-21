package com.iafabric.command;

import com.iafabric.IafMod;
import com.iafabric.pixie.ExiledPixies;
import com.iafabric.pixie.PixieBonding;
import com.iafabric.pixie.PixieElement;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * /iceandfire find &lt;place&gt;: the nearest of the mod's places (a dragon's nest, a dragons' cave, a dragon hall, a
 * pillager kingdom, a pixie sanctuary or cavern, a dread spire, Cryona's throne, a mausoleum...), with its distance
 * and a click to go there. For players allowed to cheat.
 */
public final class IafCommands {
	/** The mod's command: /iceandfire find, fairy (game masters), duel (the altar's buttons). */
	public static final String COMMAND = "iceandfire";

	/** The places, by the name used in the command. */
	private static final Map<String, String> PLACES = new LinkedHashMap<>();

	static {
		PLACES.put("nest", "dragon_roost");
		PLACES.put("cave", "dragon_cave");
		PLACES.put("hall", "dragon_hall");
		PLACES.put("kingdom", "pillager_kingdom");
		PLACES.put("sanctuary", "pixie_sanctuary");
		PLACES.put("cavern", "pixie_cavern");
		PLACES.put("witches", "witch_cottage");
		PLACES.put("hydra", "hydra_cave");
		PLACES.put("spire", "dread_spire");
		PLACES.put("throne", "dread_throne");
		PLACES.put("mausoleum", "mausoleum");
		PLACES.put("graveyard", "graveyard");
		PLACES.put("gorgon_temple", "gorgon_temple");
	}

	private IafCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			LiteralArgumentBuilder<CommandSourceStack> find = Commands.literal("find");
			for (Map.Entry<String, String> place : PLACES.entrySet()) {
				find.then(Commands.literal(place.getKey()).executes(context -> find(context, place.getKey(), place.getValue())));
			}
			// /iceandfire fairy reset: forget one's exiled pixie (the next tree brings a new one); meet: meet her here and now
			LiteralArgumentBuilder<CommandSourceStack> fairy = Commands.literal("fairy")
				.then(Commands.literal("reset").executes(context -> {
					com.iafabric.pixie.ExiledPixies.reset(context.getSource().getPlayerOrException());
					context.getSource().sendSuccess(() -> Component.translatable("iceandfire_fabric.fairy.reset"), false);
					return 1;
				}))
				.then(Commands.literal("meet").executes(context -> {
					var player = context.getSource().getPlayerOrException();
					com.iafabric.pixie.ExiledPixies.reset(player);
					com.iafabric.pixie.ExiledPixies.meet(player, player.blockPosition().relative(player.getDirection(), 2).above());
					return 1;
				}));
			// /iceandfire fairy complete [strength|jump|speed|luck|haste]: all her quests done, her bond at its last level,
			// and a pixie of that color (strength by default: more damage) bonded at level 5 with all its spells, out
			LiteralArgumentBuilder<CommandSourceStack> complete = Commands.literal("complete").executes(context -> complete(context, 0));
			for (PixieElement element : PixieElement.values()) {
				complete.then(Commands.literal(element.name().toLowerCase(Locale.ROOT)).executes(context -> complete(context, element.ordinal())));
			}
			fairy.then(complete);
			// (its duel, for everyone, is ThroneFight's)
			dispatcher.register(Commands.literal(COMMAND).then(find.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
				.then(fairy.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))));
		});
	}

	private static int complete(CommandContext<CommandSourceStack> context, int color) throws CommandSyntaxException {
		var player = context.getSource().getPlayerOrException();
		ExiledPixies.completeAll(player);
		PixieBonding.grantMaxBond(player, color);
		context.getSource().sendSuccess(() -> Component.translatable("iceandfire_fabric.fairy.complete", Component.translatable("pixie.iceandfire_fabric.element." + color)), false);
		return 1;
	}

	private static int find(CommandContext<CommandSourceStack> context, String name, String structure) {
		CommandSourceStack source = context.getSource();
		ServerLevel level = source.getLevel();
		var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		var holder = registry.get(net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE, IafMod.id(structure)));
		if (holder.isEmpty()) {
			source.sendFailure(Component.translatable("iceandfire_fabric.find.none", Component.translatable("iceandfire_fabric.find." + name)));
			return 0;
		}
		BlockPos from = BlockPos.containing(source.getPosition());
		Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
			.findNearestMapStructure(level, HolderSet.direct(holder.get()), from, 100, false);
		if (found == null) {
			source.sendFailure(Component.translatable("iceandfire_fabric.find.none", Component.translatable("iceandfire_fabric.find." + name)));
			return 0;
		}
		BlockPos at = found.getFirst();
		int distance = Mth.floor(Math.sqrt(at.distToCenterSqr(from.getX(), at.getY(), from.getZ())));
		int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, at.getX(), at.getZ());
		String tp = "/tp @s " + at.getX() + " " + Math.max(y + 2, level.getSeaLevel() + 30) + " " + at.getZ();
		Component coords = ComponentUtils.wrapInSquareBrackets(Component.literal(at.getX() + ", " + at.getZ()))
			.withStyle(style -> style.withColor(ChatFormatting.GREEN)
				.withClickEvent(new ClickEvent.SuggestCommand(tp))
				.withHoverEvent(new HoverEvent.ShowText(Component.translatable("iceandfire_fabric.find.click"))));
		Component go = ComponentUtils.wrapInSquareBrackets(Component.translatable("iceandfire_fabric.find.go"))
			.withStyle(style -> style.withColor(ChatFormatting.AQUA)
				.withClickEvent(new ClickEvent.RunCommand(tp))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal(tp))));
		source.sendSuccess(() -> Component.translatable("iceandfire_fabric.find.found", Component.translatable("iceandfire_fabric.find." + name), coords, distance, go), false);
		return distance;
	}
}
