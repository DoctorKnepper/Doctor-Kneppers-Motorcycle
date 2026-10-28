package com.doctorknepper.motorcycles.screenshots;

import java.util.List;
import java.util.Locale;

import com.doctorknepper.motorcycles.entity.MotorcycleEntity;
import com.doctorknepper.motorcycles.registry.ModEntities;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Scripted client run that stages scenes and captures promo screenshots for the mod pages.
 * Run with {@code ./gradlew runClientGameTest}; images land in build/run/clientGameTest/screenshots.
 */
public class PromoScreenshots implements FabricClientGameTest {
	private static final int Y = -60; // superflat surface
	private static final String BIKE = "dkmotorcycles:motorcycle";
	private static final int RED = 14;
	private static final int ORANGE = 1;
	private static final int BLUE = 11;
	private static final int BLACK = 15;

	@Override
	public void runTest(final ClientGameTestContext ctx) {
		if (Boolean.getBoolean("dk.behaviorTests")) {
			return;
		}
		ctx.getInput().resizeWindow(1920, 1080);
		ctx.runOnClient(mc -> {
			mc.options.renderDistance().set(6);
			mc.options.fov().set(50);
		});
		hud(ctx, false, CameraType.FIRST_PERSON);

		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			settle(ctx, sp);
			buildScene(ctx, sp);
			if (Boolean.getBoolean("dk.headlightOnly")) {
				tunnelHeadlight(ctx, sp);
				return;
			}

			heroShots(ctx, sp);
			engineLineup(ctx, sp);
			paintLineup(ctx, sp);
			storageStages(ctx, sp);
			ridingShots(ctx, sp);
			bikeInventory(ctx, sp);
			recipes(ctx, sp);
			tunnelHeadlight(ctx, sp);
		}
	}

	// ------------------------------------------------------------------ scene

	private static void buildScene(final ClientGameTestContext ctx, final TestSingleplayerContext sp) {
		TestServerContext s = sp.getServer();
		s.runCommand("gamemode spectator @p");
		s.runCommand("time set 6000");
		s.runCommand("weather clear");
		s.runCommand("gamerule send_command_feedback false");
		s.runCommand("gamerule show_advancement_messages false");
		s.runCommand("gamerule spawn_mobs false");
		s.runCommand("gamerule advance_time false");
		s.runCommand("gamerule doDaylightCycle false");
		s.runCommand("forceload add -48 -80 191 31");
		ctx.waitTicks(200);

		// asphalt road with kerbs and a dashed yellow centre line, built in 16-block segments
		for (int x = -48; x < 192; x += 16) {
			int x2 = x + 15;
			s.runCommand("fill " + x + " -61 -3 " + x2 + " -61 3 minecraft:gray_concrete");
			s.runCommand("fill " + x + " -61 -4 " + x2 + " -61 -4 minecraft:polished_andesite");
			s.runCommand("fill " + x + " -61 4 " + x2 + " -61 4 minecraft:polished_andesite");
			for (int d = x; d <= x2; d += 4) {
				s.runCommand("fill " + d + " -61 0 " + (d + 1) + " -61 0 minecraft:yellow_concrete");
			}
		}
		s.runCommand("place structure minecraft:village_plains 24 " + Y + " -48");
		int[][] trees = {{-14, -9}, {-4, -13}, {8, -10}, {38, -9}, {48, -12}, {-24, -12}, {60, -11},
			{-10, 12}, {3, 16}, {16, 12}, {30, 15}, {44, 12}, {-20, 15}, {60, 12}, {75, -10}, {90, 12}, {105, -11}, {120, 13}};
		for (int i = 0; i < trees.length; i++) {
			String feature = i % 3 == 0 ? "minecraft:birch" : "minecraft:oak";
			s.runCommand("place feature " + feature + " " + trees[i][0] + " " + Y + " " + trees[i][1]);
		}

		// dark stone tunnel for the headlight shot
		s.runCommand("fill 150 -61 -4 185 -55 4 minecraft:deepslate_bricks");
		s.runCommand("fill 150 -60 -3 185 -56 3 minecraft:air");
		s.runCommand("fill 150 -61 -3 185 -61 3 minecraft:gray_concrete");
		for (int d = 150; d <= 185; d += 4) {
			s.runCommand("fill " + d + " -61 0 " + (d + 1) + " -61 0 minecraft:yellow_concrete");
		}
		s.runCommand("fill 185 -60 -3 185 -56 3 minecraft:deepslate_bricks");
		s.runCommand("fill 150 -60 -3 150 -56 3 minecraft:deepslate_bricks");
		s.runCommand("kill @e[type=!minecraft:player]");
		ctx.waitTicks(40);
	}

	// ------------------------------------------------------------------ helpers

	private static void settle(final ClientGameTestContext ctx, final TestSingleplayerContext sp) {
		ctx.waitTicks(10);
		sp.getConnection().waitForChunksRender(true, 12000);
		ctx.waitTicks(20);
	}

	private static void shot(final ClientGameTestContext ctx, final String name) {
		ctx.runOnClient(mc -> {
			mc.gui.hud.getChat().clearMessages(false);
			mc.gui.toastManager().clear();
		});
		ctx.waitTicks(2);
		ctx.takeScreenshot(name);
	}

	private static String items(final String... entries) {
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < entries.length; i += 2) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append("{Slot:").append(entries[i]).append("b,id:\"").append(entries[i + 1]).append("\",count:1}");
		}
		return sb.append(']').toString();
	}

	private static String fullKit(final String engine) {
		return items("0", "dkmotorcycles:" + engine + "_engine", "1", "dkmotorcycles:fuel_tank", "3", "dkmotorcycles:saddlebag",
			"4", "dkmotorcycles:saddlebag", "5", "dkmotorcycles:saddlebag", "6", "dkmotorcycles:trunk");
	}

	private static void summon(final TestServerContext s, final double x, final double z, final float yaw, final int color,
		final String itemList, final String name) {
		String nameNbt = name == null ? "" : ",CustomName:\"" + name + "\",CustomNameVisible:1b";
		s.runCommand(String.format(Locale.ROOT, "summon %s %.2f %d %.2f {Rotation:[%.1ff,0f],Color:%d,Fuel:30000,Items:%s%s}",
			BIKE, x, Y, z, yaw, color, itemList, nameNbt));
	}

	/** Puts the spectator camera's eye at (x, y, z) looking at (tx, ty, tz). */
	private static void camera(final ClientGameTestContext ctx, final TestSingleplayerContext sp, final double x, final double y,
		final double z, final double tx, final double ty, final double tz) {
		double dx = tx - x;
		double dy = ty - y;
		double dz = tz - z;
		double yaw = Math.toDegrees(Math.atan2(-dx, dz));
		double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		sp.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y - 1.62, z, yaw, pitch));
		settle(ctx, sp);
	}

	private static void clearBikes(final ClientGameTestContext ctx, final TestServerContext s) {
		s.runCommand("ride @p dismount");
		s.runCommand("kill @e[type=!minecraft:player]");
		ctx.waitTicks(3);
		s.runCommand("kill @e[type=minecraft:item]");
		s.runCommand("clear @p");
		ctx.waitTicks(3);
	}

	private static void hud(final ClientGameTestContext ctx, final boolean visible, final CameraType camera) {
		ctx.runOnClient(mc -> {
			if (mc.gui.hud.isHidden() == visible) {
				mc.gui.hud.toggle();
			}
			mc.options.setCameraType(camera);
		});
	}

	private static ItemStack stack(final String id, final int count) {
		return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)), count);
	}

	private static ServerPlayer player(final MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	// ------------------------------------------------------------------ shots

	private static void heroShots(final ClientGameTestContext ctx, final TestSingleplayerContext sp) {
		TestServerContext s = sp.getServer();
		summon(s, 10.5, 1.5, -60F, RED, fullKit("netherite"), null);
		camera(ctx, sp, 12.2, -58.95, 5.5, 10.5, -59.55, 1.5);
		shot(ctx, "hero_front");
		camera(ctx, sp, 7.0, -58.9, 4.6, 10.5, -59.6, 1.5);
		shot(ctx, "hero_side");
		clearBikes(ctx, s);
	}

	private static void engineLineup(final ClientGameTestContext ctx, final TestSingleplayerContext sp) {
		TestServerContext s = sp.getServer();
		String[] engines = {"copper", "iron", "diamond", "netherite"};
		String[] labels = {"Copper engine", "Iron engine", "Diamond engine", "Netherite engine"};
		for (int i = 0; i < 4; i++) {
			summon(s, 4.5 + i * 3.0, 1.5, -55F, BLUE, items("0", "dkmotorcycles:" + engines[i] + "_engine"), labels[i]);
		}
		camera(ctx, sp, 9.0, -58.3, 8.6, 9.0, -59.6, 1.5);
		shot(ctx, "engine_tiers");
		clearBikes(ctx, s);
	}

	private static void paintLineup(final ClientGameTestContext ctx, final TestSingleplayerContext sp) {
		TestServerContext s = sp.getServer();
		int[] colors = {RED, ORANGE, 4, 5, 9, BLUE, 10, 6};
		// one row, parked at an angle like bikes along a kerb
		for (int i = 0; i < colors.length; i++) {
			summon(s, 3.0 + i * 2.0, 2.0, -30F, colors[i], items("0", "dkmotorcycles:iron_engine"), null);
		}
		camera(ctx, sp, 10.0, -58.2, 10.5, 10.0, -59.6, 2.0);
		shot(ctx, "paint_colors");
		clearBikes(ctx, s);
	}

	private static void storageStages(final ClientGameTestContext ctx, final TestSingleplayerContext sp) {
		TestServerContext s = sp.getServer();
		String engine = "dkmotorcycles:diamond_engine";
		String bag = "dkmotorcycles:saddlebag";
		float yaw = 125F;
		summon(s, 4.5, 1.5, yaw, ORANGE, items("0", engine), "No storage");
		summon(s, 7.5, 1.5, yaw, ORANGE, items("0", engine, "3", bag), "1 saddlebag");
		summon(s, 10.5, 1.5, yaw, ORANGE, items("0", engine, "3", bag, "4", bag, "5", bag), "3 saddlebags");
		summon(s, 13.5, 1.5, yaw, ORANGE, items("0", engine, "3", bag, "4", bag, "5", bag, "6", "dkmotorcycles:trunk"), "Bags + trunk");
		camera(ctx, sp, 9.0, -58.3, 8.2, 9.0, -59.6, 1.5);
		shot(ctx, "storage_stages");
		clearBikes(ctx, s);
	}

	private static void mount(final ClientGameTestContext ctx, final TestSingleplayerContext sp, final double x, final float yaw,
		final int color, final String kit, final boolean passenger) {
		TestServerContext s = sp.getServer();
		s.runCommand("clear @p");
		s.runCommand("gamemode creative @p");
		s.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d 1.5 %.1f 10", x, Y, yaw));
		summon(s, x, 1.5, yaw, color, kit, null);
		ctx.waitTicks(5);
		s.runCommand("ride @p mount @e[type=" + BIKE + ",limit=1,sort=nearest]");
		ctx.waitTicks(5);
		if (passenger) {
			s.runCommand(String.format(Locale.ROOT,
				"summon minecraft:villager %.1f %d 3.5 {NoAI:1b,VillagerData:{profession:\"minecraft:librarian\",level:1,type:\"minecraft:plains\"}}", x, Y));
			ctx.waitTicks(5);
			s.runCommand("ride @e[type=minecraft:villager,limit=1,sort=nearest] mount @e[type=" + BIKE + ",limit=1,sort=nearest]");
		}
		s.runCommand(String.format(Locale.ROOT, "rotate @p %.1f 8", yaw));
		settle(ctx, sp);
	}

	private static void ridingShots(final ClientGameTestContext ctx, final TestSingleplayerContext sp) {
		TestServerContext s = sp.getServer();
		ctx.runOnClient(mc -> mc.options.fov().set(70));
		mount(ctx, sp, 0.5, -90F, RED, fullKit("netherite"), true);
		hud(ctx, false, CameraType.THIRD_PERSON_FRONT);
		ctx.waitTicks(20);
		shot(ctx, "riding_passenger");
		clearBikes(ctx, s);

		// on the move, with the speed / fuel HUD
		mount(ctx, sp, -30.5, -90F, BLUE, fullKit("netherite"), false);
		hud(ctx, true, CameraType.THIRD_PERSON_BACK);
		ctx.getInput().holdKey(options -> options.keyUp);
		ctx.waitTicks(45);
		shot(ctx, "riding_hud");
		ctx.getInput().releaseKey(options -> options.keyUp);
		ctx.waitTicks(60);
		clearBikes(ctx, s);
		ctx.runOnClient(mc -> mc.options.fov().set(50));
	}

	private static void bikeInventory(final ClientGameTestContext ctx, final TestSingleplayerContext sp) {
		TestServerContext s = sp.getServer();
		mount(ctx, sp, 10.5, -90F, RED, fullKit("netherite"), false);
		s.runOnServer(server -> {
			ServerPlayer player = player(server);
			List<? extends MotorcycleEntity> bikes = player.level().getEntities(ModEntities.MOTORCYCLE, e -> true);
			MotorcycleEntity bike = bikes.getFirst();
			var inv = bike.getInventory();
			inv.setItem(MotorcycleEntity.SLOT_FUEL, stack("minecraft:coal", 48));
			String[] cargoItems = {"minecraft:bread", "minecraft:cooked_beef", "minecraft:torch", "minecraft:oak_log", "minecraft:iron_ingot",
				"minecraft:cobblestone", "minecraft:apple", "minecraft:arrow", "minecraft:diamond", "minecraft:map", "minecraft:redstone",
				"minecraft:blaze_rod", "minecraft:lava_bucket", "minecraft:dried_kelp_block", "minecraft:charcoal"};
			int[] counts = {12, 20, 48, 32, 9, 64, 7, 40, 3, 1, 22, 6, 1, 16, 30};
			int[] slots = {0, 1, 2, 9, 10, 18, 19, 27, 28, 29, 30, 36, 37, 38, 45};
			for (int i = 0; i < cargoItems.length; i++) {
				inv.setItem(MotorcycleEntity.STORAGE_START + slots[i], stack(cargoItems[i], counts[i]));
			}
			bike.openCustomInventoryScreen(player);
		});
		hud(ctx, true, CameraType.FIRST_PERSON);
		ctx.waitTicks(20);
		ctx.getInput().setCursorPos(1900, 1060);
		ctx.waitTicks(5);
		shot(ctx, "bike_inventory");
		ctx.setScreen(() -> null);
		clearBikes(ctx, s);
	}

	// ------------------------------------------------------------------ recipes

	private static void recipes(final ClientGameTestContext ctx, final TestSingleplayerContext sp) {
		TestServerContext s = sp.getServer();
		s.runCommand("gamemode survival @p");
		s.runCommand("tp @p 0.5 " + Y + " 8.5 0 0");
		s.runCommand("setblock 0 " + Y + " 10 minecraft:crafting_table");
		s.runCommand("setblock 1 " + Y + " 10 minecraft:smithing_table");
		hud(ctx, true, CameraType.FIRST_PERSON);
		ctx.waitTicks(10);

		craft(ctx, s, "recipe_motorcycle", " LI", "ICI", "W W",
			"L", "minecraft:leather", "I", "minecraft:iron_ingot", "C", "dkmotorcycles:copper_engine", "W", "minecraft:black_wool");
		craft(ctx, s, "recipe_copper_engine", "CCC", "CRC", "CFC",
			"C", "minecraft:copper_ingot", "R", "minecraft:redstone", "F", "minecraft:furnace");
		craft(ctx, s, "recipe_iron_engine", "IRI", "IEI", "III",
			"I", "minecraft:iron_ingot", "R", "minecraft:redstone", "E", "dkmotorcycles:copper_engine");
		craft(ctx, s, "recipe_diamond_engine", " D ", "DED", " R ",
			"D", "minecraft:diamond", "E", "dkmotorcycles:iron_engine", "R", "minecraft:redstone_block");
		craft(ctx, s, "recipe_fuel_tank", "III", "IBI", "III", "I", "minecraft:iron_ingot", "B", "minecraft:bucket");
		craft(ctx, s, "recipe_saddlebag", "SGS", "L L", "LLL",
			"S", "minecraft:string", "G", "minecraft:gold_nugget", "L", "minecraft:leather");
		craft(ctx, s, "recipe_trunk", "III", "ICI", "III", "I", "minecraft:iron_ingot", "C", "minecraft:chest");

		s.runOnServer(server -> {
			ServerPlayer player = player(server);
			ContainerLevelAccess access = ContainerLevelAccess.create(player.level(), new BlockPos(1, Y, 10));
			player.openMenu(new SimpleMenuProvider((id, inv, p) -> new SmithingMenu(id, inv, access), Component.translatable("container.upgrade")));
			AbstractContainerMenu menu = player.containerMenu;
			menu.getSlot(0).set(stack("minecraft:netherite_upgrade_smithing_template", 1));
			menu.getSlot(1).set(stack("dkmotorcycles:diamond_engine", 1));
			menu.getSlot(2).set(stack("minecraft:netherite_ingot", 1));
			menu.broadcastChanges();
		});
		ctx.waitTicks(15);
		hoverGui(ctx, 152 + 98 + 8, 52 + 48 + 8);
		shot(ctx, "recipe_netherite_engine");
		closeAndClear(ctx, s);
	}

	private static void craft(final ClientGameTestContext ctx, final TestServerContext s, final String name, final String row1,
		final String row2, final String row3, final String... key) {
		String pattern = row1 + row2 + row3;
		s.runOnServer(server -> {
			ServerPlayer player = player(server);
			ContainerLevelAccess access = ContainerLevelAccess.create(player.level(), new BlockPos(0, Y, 10));
			player.openMenu(new SimpleMenuProvider((id, inv, p) -> new CraftingMenu(id, inv, access), Component.translatable("container.crafting")));
			AbstractContainerMenu menu = player.containerMenu;
			for (int i = 0; i < 9; i++) {
				char c = pattern.charAt(i);
				ItemStack st = ItemStack.EMPTY;
				for (int k = 0; k < key.length; k += 2) {
					if (key[k].charAt(0) == c) {
						st = stack(key[k + 1], 1);
					}
				}
				menu.getSlot(1 + i).set(st);
			}
			menu.broadcastChanges();
		});
		ctx.waitTicks(15);
		hoverGui(ctx, 152 + 124 + 8, 52 + 35 + 8);
		shot(ctx, name);
		closeAndClear(ctx, s);
	}

	/** Empties the grid before closing so ingredients don't pile up in the player's inventory. */
	private static void closeAndClear(final ClientGameTestContext ctx, final TestServerContext s) {
		s.runOnServer(server -> {
			ServerPlayer player = player(server);
			AbstractContainerMenu menu = player.containerMenu;
			for (int i = 0; i < menu.slots.size() - 36; i++) {
				menu.getSlot(i).set(ItemStack.EMPTY);
			}
			player.closeContainer();
			player.getInventory().clearContent();
		});
		ctx.waitTicks(10);
	}

	/** Moves the mouse to a GUI-space point (GUI scale 4 at 1920x1080). */
	private static void hoverGui(final ClientGameTestContext ctx, final int gx, final int gy) {
		ctx.getInput().setCursorPos(gx * 4.0, gy * 4.0);
		ctx.waitTicks(5);
	}

	// ------------------------------------------------------------------ headlight

	private static void tunnelHeadlight(final ClientGameTestContext ctx, final TestSingleplayerContext sp) {
		TestServerContext s = sp.getServer();
		s.runCommand("time set 18000");
		ctx.runOnClient(mc -> mc.options.fov().set(70));
		mount(ctx, sp, 156.5, -90F, BLACK, fullKit("diamond"), false);
		hud(ctx, false, CameraType.THIRD_PERSON_BACK);
		ctx.waitTicks(60);
		settle(ctx, sp);
		shot(ctx, "headlight_on");
		s.runOnServer(server -> player(server).level().getEntities(ModEntities.MOTORCYCLE, e -> true).getFirst().toggleHeadlight());
		ctx.waitTicks(40);
		settle(ctx, sp);
		shot(ctx, "headlight_off");
		clearBikes(ctx, s);
	}
}
