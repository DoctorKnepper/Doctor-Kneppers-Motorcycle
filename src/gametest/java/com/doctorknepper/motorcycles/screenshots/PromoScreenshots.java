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
import net.minecraft.server.level.ServerLevel;
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

	@Override
	public void runTest(final ClientGameTestContext ctx) {
		ctx.getInput().resizeWindow(1920, 1080);
		ctx.runOnClient(mc -> {
			mc.options.renderDistance().set(6);
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});

		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			TestServerContext server = sp.getServer();
			sp.getConnection().waitForChunksRender(true, 12000);
			buildScene(ctx, server);

			heroShots(ctx, sp);
			engineLineup(ctx, sp);
			paintLineup(ctx, sp);
			storageStages(ctx, sp);
			ridingShots(ctx, sp);
			nightShot(ctx, sp);
			bikeInventory(ctx, sp);
			recipes(ctx, sp);
		}
	}

	// ------------------------------------------------------------------ scene

	private static void buildScene(final ClientGameTestContext ctx, final TestServerContext s) {
		s.runCommand("gamemode spectator @p");
		s.runCommand("time set 6000");
		s.runCommand("weather clear");
		s.runCommand("forceload add -32 -80 150 32");
		// asphalt road with a dashed yellow centre line
		s.runCommand("fill -60 -61 -3 220 -61 3 minecraft:gray_concrete");
		s.runCommand("fill -60 -61 -4 220 -61 -4 minecraft:stone_bricks");
		s.runCommand("fill -60 -61 4 220 -61 4 minecraft:stone_bricks");
		for (int x = -60; x <= 220; x += 4) {
			s.runCommand("fill " + x + " -61 0 " + (x + 1) + " -61 0 minecraft:yellow_concrete");
		}
		s.runCommand("place structure minecraft:village_plains 20 " + Y + " -55");
		int[][] trees = {{-14, -9}, {-4, -14}, {6, -10}, {17, -16}, {26, -9}, {36, -13}, {46, -10}, {-24, -12}, {58, -14},
			{-10, 13}, {3, 18}, {14, 12}, {30, 16}, {44, 13}, {-20, 16}, {60, 12}, {75, -10}, {90, 12}, {105, -11}, {120, 13}};
		for (int i = 0; i < trees.length; i++) {
			String feature = i % 3 == 0 ? "minecraft:birch" : "minecraft:oak";
			s.runCommand("place feature " + feature + " " + trees[i][0] + " " + Y + " " + trees[i][1]);
		}
		s.runCommand("kill @e[type=!minecraft:player,type=!minecraft:villager]");
		ctx.waitTicks(40);
	}

	// ------------------------------------------------------------------ helpers

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

	private static void camera(final ClientGameTestContext ctx, final TestSingleplayerContext sp, final double x, final double y,
		final double z, final double tx, final double ty, final double tz) {
		double dx = tx - x;
		double dy = ty - y;
		double dz = tz - z;
		double yaw = Math.toDegrees(Math.atan2(-dx, dz));
		double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		sp.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y - 1.62, z, yaw, pitch));
		ctx.waitTicks(10);
		sp.getConnection().waitForChunksRender(true, 12000);
		ctx.waitTicks(30);
	}

	private static void clearBikes(final ClientGameTestContext ctx, final TestServerContext s) {
		s.runCommand("ride @p dismount");
		s.runCommand("kill @e[type=" + BIKE + "]");
		s.runCommand("kill @e[type=minecraft:item]");
		ctx.waitTicks(5);
	}

	private static void hud(final ClientGameTestContext ctx, final boolean visible, final CameraType camera) {
		ctx.runOnClient(mc -> {
			if (mc.gui.hud.isHidden() == visible) {
				mc.gui.hud.toggle();
			}
			mc.options.setCameraType(camera);
		});
	}

	// ------------------------------------------------------------------ shots

	private static void heroShots(final ClientGameTestContext ctx, final TestSingleplayerContext sp) {
		TestServerContext s = sp.getServer();
		summon(s, 10.5, 1.0, -60F, 14, fullKit("netherite"), null);
		camera(ctx, sp, 14.8, -58.4, 5.6, 10.5, -59.3, 1.0);
		ctx.takeScreenshot("hero_front");
		camera(ctx, sp, 5.0, -58.6, 5.0, 10.5, -59.4, 1.0);
		ctx.takeScreenshot("hero_rear");
		camera(ctx, sp, 10.5, -59.0, 5.8, 10.5, -59.5, 1.0);
		ctx.takeScreenshot("hero_side");
		clearBikes(ctx, s);
	}

	private static void engineLineup(final ClientGameTestContext ctx, final TestSingleplayerContext sp) {
		TestServerContext s = sp.getServer();
		String[] engines = {"copper", "iron", "diamond", "netherite"};
		String[] labels = {"Copper engine", "Iron engine", "Diamond engine", "Netherite engine"};
		for (int i = 0; i < 4; i++) {
			summon(s, 2.5 + i * 4.0, 1.0, -55F, 11, items("0", "dkmotorcycles:" + engines[i] + "_engine"), labels[i]);
		}
		camera(ctx, sp, 8.5, -57.6, 9.5, 8.5, -59.8, 1.0);
		ctx.takeScreenshot("engine_tiers");
		clearBikes(ctx, s);
	}

	private static void paintLineup(final ClientGameTestContext ctx, final TestSingleplayerContext sp) {
		TestServerContext s = sp.getServer();
		int[] colors = {14, 1, 4, 5, 9, 11, 10, 6, 15, 0};
		for (int i = 0; i < colors.length; i++) {
			summon(s, -4.5 + i * 3.0, 1.0, -55F, colors[i], items("0", "dkmotorcycles:iron_engine"), null);
		}
		camera(ctx, sp, 9.0, -57.0, 12.0, 9.0, -60.0, 0.5);
		ctx.takeScreenshot("paint_colors");
		clearBikes(ctx, s);
	}

	private static void storageStages(final ClientGameTestContext ctx, final TestSingleplayerContext sp) {
		TestServerContext s = sp.getServer();
		String engine = "dkmotorcycles:diamond_engine";
		String bag = "dkmotorcycles:saddlebag";
		summon(s, 2.5, 1.0, -125F, 1, items("0", engine), "No storage");
		summon(s, 6.5, 1.0, -125F, 1, items("0", engine, "3", bag), "1 saddlebag");
		summon(s, 10.5, 1.0, -125F, 1, items("0", engine, "3", bag, "4", bag, "5", bag), "3 saddlebags");
		summon(s, 14.5, 1.0, -125F, 1, items("0", engine, "3", bag, "4", bag, "5", bag, "6", "dkmotorcycles:trunk"), "Saddlebags + trunk");
		camera(ctx, sp, 8.5, -57.4, 8.0, 8.5, -59.8, 1.0);
		ctx.takeScreenshot("storage_stages");
		clearBikes(ctx, s);
	}

	private static void mountWithPassenger(final ClientGameTestContext ctx, final TestSingleplayerContext sp, final double x,
		final float yaw, final int color) {
		TestServerContext s = sp.getServer();
		s.runCommand("gamemode creative @p");
		s.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d 1.0 %.1f 10", x, Y, yaw));
		summon(s, x, 1.0, yaw, color, fullKit("netherite"), null);
		ctx.waitTicks(5);
		s.runCommand("ride @p mount @e[type=" + BIKE + ",limit=1,sort=nearest]");
		ctx.waitTicks(5);
		s.runCommand(String.format(Locale.ROOT, "summon minecraft:villager %.1f %d 3.0 {NoAI:1b,VillagerData:{profession:\"minecraft:librarian\",level:1,type:\"minecraft:plains\"}}", x, Y));
		ctx.waitTicks(5);
		s.runCommand("ride @e[type=minecraft:villager,limit=1,sort=nearest] mount @e[type=" + BIKE + ",limit=1,sort=nearest]");
		s.runCommand(String.format(Locale.ROOT, "rotate @p %.1f 8", yaw));
		ctx.waitTicks(20);
		sp.getConnection().waitForChunksRender(true, 12000);
	}

	private static void ridingShots(final ClientGameTestContext ctx, final TestSingleplayerContext sp) {
		TestServerContext s = sp.getServer();
		mountWithPassenger(ctx, sp, 0.5, -90F, 14);
		hud(ctx, false, CameraType.THIRD_PERSON_FRONT);
		ctx.waitTicks(20);
		ctx.takeScreenshot("riding_passenger_front");
		hud(ctx, false, CameraType.THIRD_PERSON_BACK);
		ctx.waitTicks(20);
		ctx.takeScreenshot("riding_passenger_back");

		// on the move, with the speed / fuel HUD
		hud(ctx, true, CameraType.THIRD_PERSON_BACK);
		ctx.getInput().holdKeyFor(options -> options.keyUp, 50);
		ctx.getInput().holdKey(options -> options.keyUp);
		ctx.waitTicks(3);
		ctx.takeScreenshot("riding_hud");
		ctx.getInput().releaseKey(options -> options.keyUp);
		ctx.waitTicks(40);
		s.runCommand("ride @e[type=minecraft:villager] dismount");
		s.runCommand("kill @e[type=minecraft:villager]");
		clearBikes(ctx, s);
	}

	private static void nightShot(final ClientGameTestContext ctx, final TestSingleplayerContext sp) {
		TestServerContext s = sp.getServer();
		s.runCommand("time set 18000");
		s.runCommand("tp @p 140.5 " + Y + " 1.0 -90 10");
		ctx.waitTicks(10);
		s.runCommand("summon " + BIKE + " 140.5 " + Y + " 1.0 {Rotation:[-90f,0f],Color:15,Fuel:30000,Headlight:1b,Items:" + fullKit("diamond") + "}");
		ctx.waitTicks(5);
		s.runCommand("ride @p mount @e[type=" + BIKE + ",limit=1,sort=nearest]");
		s.runCommand("rotate @p -90 12");
		s.runCommand("kill @e[type=!minecraft:player,type=!" + BIKE + "]");
		hud(ctx, true, CameraType.THIRD_PERSON_BACK);
		ctx.waitTicks(40);
		sp.getConnection().waitForChunksRender(true, 12000);
		ctx.waitTicks(20);
		ctx.takeScreenshot("night_headlight");
		clearBikes(ctx, s);
		s.runCommand("time set 6000");
	}

	private static void bikeInventory(final ClientGameTestContext ctx, final TestSingleplayerContext sp) {
		TestServerContext s = sp.getServer();
		s.runCommand("tp @p 10.5 " + Y + " 1.0 -90 10");
		String cargo = items("0", "dkmotorcycles:netherite_engine", "1", "dkmotorcycles:fuel_tank", "3", "dkmotorcycles:saddlebag",
			"4", "dkmotorcycles:saddlebag", "5", "dkmotorcycles:saddlebag", "6", "dkmotorcycles:trunk");
		summon(s, 10.5, 1.0, -90F, 14, cargo, null);
		ctx.waitTicks(5);
		s.runCommand("ride @p mount @e[type=" + BIKE + ",limit=1,sort=nearest]");
		ctx.waitTicks(5);
		s.runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			ServerLevel level = player.level();
			List<? extends MotorcycleEntity> bikes = level.getEntities(ModEntities.MOTORCYCLE, e -> true);
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
		ctx.getInput().setCursorPos(1880, 1040);
		ctx.waitTicks(5);
		ctx.takeScreenshot("bike_inventory");
		ctx.setScreen(() -> null);
		clearBikes(ctx, s);
	}

	private static ItemStack stack(final String id, final int count) {
		return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)), count);
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
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
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
		ctx.takeScreenshot("recipe_netherite_engine");
		ctx.setScreen(() -> null);
	}

	private static void craft(final ClientGameTestContext ctx, final TestServerContext s, final String name, final String row1,
		final String row2, final String row3, final String... key) {
		String pattern = row1 + row2 + row3;
		s.runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
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
		ctx.takeScreenshot(name);
		ctx.setScreen(() -> null);
		ctx.waitTicks(5);
	}

	/** Moves the mouse to a GUI-space point (GUI scale 4 at 1920x1080). */
	private static void hoverGui(final ClientGameTestContext ctx, final int gx, final int gy) {
		ctx.getInput().setCursorPos(gx * 4.0, gy * 4.0);
		ctx.waitTicks(5);
	}

}
