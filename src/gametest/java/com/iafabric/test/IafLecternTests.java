package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.enums.EnumBestiaryPages;
import com.iafabric.item.IafItemRegistry;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * IAF_ONLY=lectern. Players: the bestiary did not take the manuscripts' pages at the lectern. A survival player puts a
 * bestiary and six manuscripts in the lectern and clicks a page as the screen does: the book has one page more and
 * three manuscripts are gone; then a second page.
 */
public class IafLecternTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (IafTestOnly.skip("lectern")) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> {
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
			settings.setAllowCommands(true);
		}).create()) {
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule spawn_mobs false");
			BlockPos pos = world.getServer().computeOnServer(server -> {
				var player = server.getPlayerList().getPlayers().get(0);
				BlockPos at = player.blockPosition().offset(2, 0, 0);
				server.overworld().setBlock(at, IafBlockRegistry.LECTERN.defaultBlockState(), 3);
				return at;
			});
			context.waitTicks(5);
			int[] before = {0};
			for (int click = 0; click < 2; click++) {
				world.getServer().runOnServer(server -> {
					var player = server.getPlayerList().getPlayers().get(0);
					var lectern = (com.iafabric.entity.tile.TileEntityLectern) server.overworld().getBlockEntity(pos);
					if (lectern.getItem(0).isEmpty()) {
						lectern.setItem(0, new ItemStack(IafItemRegistry.BESTIARY));
						lectern.setItem(1, new ItemStack(IafItemRegistry.MANUSCRIPT, 6));
					}
					before[0] = pages(lectern.getItem(0));
					player.openMenu((net.minecraft.world.MenuProvider) lectern);
				});
				context.runOnClient(mc -> IafMod.PROXY.setRefrencedTE(mc.level.getBlockEntity(pos)));
				context.waitTicks(20);
				int slot = click;
				boolean took = context.computeOnClient(mc -> {
					var menu = mc.player.containerMenu;
					if (!(menu instanceof com.iafabric.inventory.ContainerLectern lecternMenu)) {
						throw new AssertionError("the lectern screen did not open: " + menu);
					}
					// as GuiLectern.mouseClicked
					boolean ok = lecternMenu.clickMenuButton(mc.player, slot);
					if (ok) {
						mc.gameMode.handleInventoryButtonClick(menu.containerId, slot);
					}
					return ok;
				});
				context.waitTicks(20);
				int[] after = world.getServer().computeOnServer(server -> {
					var lectern = (com.iafabric.entity.tile.TileEntityLectern) server.overworld().getBlockEntity(pos);
					return new int[]{pages(lectern.getItem(0)), lectern.getItem(1).getCount()};
				});
				IafMod.LOGGER.info("[IAFLECTERN] click {}: the screen took it {}, pages {} -> {}, manuscripts left {}", click, took, before[0], after[0], after[1]);
				if (after[0] != before[0] + 1) {
					throw new AssertionError("click " + click + ": the bestiary has " + after[0] + " pages, it had " + before[0]);
				}
				if (after[1] != 6 - 3 * (click + 1)) {
					throw new AssertionError("click " + click + ": " + after[1] + " manuscripts left");
				}
				context.takeScreenshot("lectern_click_" + click);
				if (click == 1) {
					// the book taken out (shift-click), as a player does: its pages go with it, on both sides
					context.runOnClient(mc -> mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, 0, 0,
						net.minecraft.world.inventory.ContainerInput.QUICK_MOVE, mc.player));
					context.waitTicks(20);
					int[] held = world.getServer().computeOnServer(server -> {
						var inv = server.getPlayerList().getPlayers().get(0).getInventory();
						for (int i = 0; i < inv.getContainerSize(); i++) {
							if (inv.getItem(i).is(IafItemRegistry.BESTIARY)) return new int[]{pages(inv.getItem(i))};
						}
						return new int[]{-1};
					});
					int clientPages = context.computeOnClient(mc -> {
						var inv = mc.player.getInventory();
						for (int i = 0; i < inv.getContainerSize(); i++) {
							if (inv.getItem(i).is(IafItemRegistry.BESTIARY)) return pages(inv.getItem(i));
						}
						return -1;
					});
					IafMod.LOGGER.info("[IAFLECTERN] book taken out: server pages {}, client pages {}", held[0], clientPages);
					if (held[0] != 2 || clientPages != 2) {
						throw new AssertionError("the book taken out has " + held[0] + " pages on the server and " + clientPages + " on the client");
					}
				}
				world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).closeContainer());
				context.waitTicks(5);
			}
		}
	}

	private static int pages(ItemStack book) {
		return EnumBestiaryPages.ALL_PAGES.size() - EnumBestiaryPages.possiblePages(book).size();
	}
}
