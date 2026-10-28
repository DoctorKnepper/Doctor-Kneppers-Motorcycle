package com.doctorknepper.motorcycles.screenshots;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.doctorknepper.motorcycles.entity.MotorcycleEntity;
import com.doctorknepper.motorcycles.registry.ModEntities;
import com.doctorknepper.motorcycles.registry.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

/**
 * Plays the mod in a real client and checks the riding rules. Run with
 * {@code ./gradlew runClientGameTest -PbehaviorTests}; any failed check fails the run.
 */
public class BehaviorTests implements FabricClientGameTest {
	private static final int Y = -60;
	private static final String BIKE = "dkmotorcycles:motorcycle";
	private final List<String> failures = new ArrayList<>();

	@Override
	public void runTest(final ClientGameTestContext ctx) {
		if (!Boolean.getBoolean("dk.behaviorTests")) {
			return;
		}
		ctx.getInput().resizeWindow(1280, 720);
		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			TestServerContext s = sp.getServer();
			sp.getConnection().waitForChunksRender(true, 12000);
			s.runCommand("gamerule spawn_mobs false");
			s.runCommand("gamerule advance_time false");
			s.runCommand("time set 6000");
			s.runCommand("forceload add 0 -64 400 64");
			ctx.waitTicks(100);

			speedTest(ctx, sp, "copper", 8.0);
			speedTest(ctx, sp, "netherite", 18.0);
			edgeTest(ctx, sp);
			waterTest(ctx, sp);
			rammingTest(ctx, sp);
			ownerTest(ctx, sp);
			sidecarTest(ctx, sp);
		}
		System.out.println("[DKTEST] finished with " + this.failures.size() + " failure(s)");
		if (!this.failures.isEmpty()) {
			throw new AssertionError("Behavior checks failed: " + String.join("; ", this.failures));
		}
	}

	private void check(final String name, final boolean ok, final String detail) {
		System.out.println("[DKTEST] " + (ok ? "PASS " : "FAIL ") + name + " - " + detail);
		if (!ok) {
			this.failures.add(name + " (" + detail + ")");
		}
	}

	// ------------------------------------------------------------------ helpers

	private static String kit(final String engine, final boolean sidecars) {
		String items = "{Slot:0b,id:\"dkmotorcycles:" + engine + "_engine\",count:1}";
		if (sidecars) {
			items += ",{Slot:61b,id:\"dkmotorcycles:sidecar\",count:1},{Slot:62b,id:\"dkmotorcycles:sidecar\",count:1}";
		}
		return "[" + items + "]";
	}

	private static void summonBike(final TestServerContext s, final double x, final double z, final String engine, final boolean sidecars, final String extra) {
		summonBikeAt(s, x, Y, z, engine, sidecars, extra);
	}

	private static void summonBikeAt(final TestServerContext s, final double x, final int y, final double z, final String engine, final boolean sidecars, final String extra) {
		s.runCommand(String.format(Locale.ROOT, "summon %s %.1f %d %.1f {Rotation:[-90f,0f],Fuel:30000,Items:%s%s}", BIKE, x, y, z, kit(engine, sidecars), extra));
	}

	private static void mount(final ClientGameTestContext ctx, final TestSingleplayerContext sp, final double x, final double z) {
		mountAt(ctx, sp, x, Y, z);
	}

	private static void mountAt(final ClientGameTestContext ctx, final TestSingleplayerContext sp, final double x, final int y, final double z) {
		TestServerContext s = sp.getServer();
		s.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f -90 10", x, y, z));
		ctx.waitTicks(5);
		s.runCommand("ride @p mount @e[type=" + BIKE + ",limit=1,sort=nearest]");
		s.runCommand("rotate @p -90 10");
		ctx.waitTicks(10);
		sp.getConnection().waitForChunksRender(true, 12000);
	}

	private static void clear(final ClientGameTestContext ctx, final TestServerContext s) {
		s.runCommand("ride @p dismount");
		s.runCommand("kill @e[type=!minecraft:player]");
		ctx.waitTicks(3);
		s.runCommand("kill @e[type=minecraft:item]");
		ctx.waitTicks(3);
	}

	private static ServerPlayer player(final MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static MotorcycleEntity serverBike(final MinecraftServer server) {
		return player(server).level().getEntities(ModEntities.MOTORCYCLE, e -> true).getFirst();
	}

	private static Vec3 clientBikePos(final ClientGameTestContext ctx) {
		return ctx.computeOnClient(mc -> mc.player.getVehicle() == null ? mc.player.position() : mc.player.getVehicle().position());
	}

	// ------------------------------------------------------------------ tests

	private void speedTest(final ClientGameTestContext ctx, final TestSingleplayerContext sp, final String engine, final double expected) {
		TestServerContext s = sp.getServer();
		summonBike(s, 0.5, 0.5, engine, false, "");
		mount(ctx, sp, 0.5, 0.5);
		ctx.getInput().holdKey(o -> o.keyUp);
		ctx.waitTicks(80);
		Vec3 a = clientBikePos(ctx);
		ctx.waitTicks(40);
		Vec3 b = clientBikePos(ctx);
		ctx.getInput().releaseKey(o -> o.keyUp);
		double bps = (b.x - a.x) * 20.0 / 40.0;
		this.check("top speed " + engine, Math.abs(bps - expected) < 1.5, String.format(Locale.ROOT, "%.1f blocks/s, expected about %.0f", bps, expected));
		ctx.waitTicks(40);
		clear(ctx, s);
	}

	private void edgeTest(final ClientGameTestContext ctx, final TestSingleplayerContext sp) {
		TestServerContext s = sp.getServer();
		s.runCommand("gamemode survival @p");
		// an 8-high platform that ends 12 blocks ahead of the bike
		s.runCommand("fill 90 -60 -4 111 -53 4 minecraft:stone");
		summonBikeAt(s, 100.5, -52, 0.5, "netherite", false, "");
		mountAt(ctx, sp, 100.5, -52, 0.5);
		ctx.getInput().holdKey(o -> o.keyUp);
		ctx.waitTicks(80);
		Vec3 stopped = clientBikePos(ctx);
		boolean warned = ctx.computeOnClient(mc -> mc.player.getVehicle() instanceof MotorcycleEntity bike && bike.isEdgeWarning());
		this.check("edge braking stops at a drop", stopped.x < 112.3 && stopped.y > -52.5,
			String.format(Locale.ROOT, "bike at x=%.2f y=%.2f (platform ends at x=112)", stopped.x, stopped.y));
		this.check("edge warning shown", warned, "HUD warning flag " + warned);
		ctx.takeScreenshot("test_edge_warning");

		float riderHealthBefore = s.computeOnServer(server -> player(server).getHealth());
		ctx.getInput().holdKey(o -> o.keySprint);
		ctx.waitTicks(40);
		ctx.getInput().releaseKey(o -> o.keySprint);
		ctx.getInput().releaseKey(o -> o.keyUp);
		ctx.waitTicks(40);
		Vec3 fell = clientBikePos(ctx);
		this.check("sprint drives over the edge", fell.y < -59, String.format(Locale.ROOT, "bike y=%.2f", fell.y));
		float riderHealthAfter = s.computeOnServer(server -> player(server).getHealth());
		float bikeHealth = s.computeOnServer(server -> serverBike(server).getHealth());
		this.check("rider takes no fall damage", riderHealthAfter >= riderHealthBefore,
			"rider " + riderHealthBefore + " -> " + riderHealthAfter + ", bike health " + bikeHealth);
		this.check("bike takes the fall damage", bikeHealth < 40.0F, "bike health " + bikeHealth);
		clear(ctx, s);
		s.runCommand("gamemode creative @p");
	}

	private void waterTest(final ClientGameTestContext ctx, final TestSingleplayerContext sp) {
		TestServerContext s = sp.getServer();
		s.runCommand("fill 210 -64 -4 230 -61 4 minecraft:water");
		summonBike(s, 205.5, 0.5, "iron", false, "");
		mount(ctx, sp, 205.5, 0.5);
		ctx.getInput().holdKey(o -> o.keyUp);
		ctx.waitTicks(100);
		ctx.getInput().releaseKey(o -> o.keyUp);
		ctx.waitTicks(40);
		Vec3 pos = clientBikePos(ctx);
		boolean afloat = s.computeOnServer(server -> serverBike(server).isAfloat());
		boolean canDrive = s.computeOnServer(server -> serverBike(server).canDrive());
		this.check("bike floats in deep water", pos.x > 211 && pos.y > -61.8, String.format(Locale.ROOT, "bike at x=%.2f y=%.2f (water bottom y=-64)", pos.x, pos.y));
		this.check("engine floods in water", afloat && !canDrive, "afloat=" + afloat + " canDrive=" + canDrive);
		ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		ctx.waitTicks(10);
		ctx.takeScreenshot("test_water");
		ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		clear(ctx, s);
	}

	private void rammingTest(final ClientGameTestContext ctx, final TestSingleplayerContext sp) {
		TestServerContext s = sp.getServer();
		String[] kinds = {"zombie", "villager", "wolf"};
		for (int lane = 0; lane < kinds.length; lane++) {
			double z = 20.5 + lane * 10;
			String kind = kinds[lane];
			s.runOnServer(server -> {
				ServerLevel level = player(server).level();
				LivingEntity mob = switch (kind) {
					case "zombie" -> new Zombie(EntityTypes.ZOMBIE, level);
					case "villager" -> new Villager(EntityTypes.VILLAGER, level);
					default -> {
						Wolf wolf = new Wolf(EntityTypes.WOLF, level);
						wolf.tame(player(server));
						yield wolf;
					}
				};
				mob.snapTo(20.5, Y, z, 90.0F, 0.0F);
				if (mob instanceof net.minecraft.world.entity.Mob m) {
					m.setNoAi(true);
					m.setPersistenceRequired();
				}
				level.addFreshEntity(mob);
			});
			summonBike(s, 0.5, z, "diamond", false, "");
			mount(ctx, sp, 0.5, z);
			ctx.getInput().holdKey(o -> o.keyUp);
			ctx.waitTicks(60);
			ctx.getInput().releaseKey(o -> o.keyUp);
			ctx.waitTicks(30);
			float[] health = s.computeOnServer(server -> {
				LivingEntity mob = player(server).level().getEntitiesOfClass(LivingEntity.class,
					player(server).getBoundingBox().inflate(60), e -> !(e instanceof MotorcycleEntity) && !(e instanceof ServerPlayer)).stream()
					.findFirst().orElse(null);
				return mob == null ? new float[] {-1, -1} : new float[] {mob.getHealth(), mob.getMaxHealth()};
			});
			boolean hurt = health[0] >= 0 && health[0] < health[1];
			boolean shouldHurt = kind.equals("zombie");
			this.check("ramming " + kind, hurt == shouldHurt, "health " + health[0] + "/" + health[1] + (shouldHurt ? " (should be hurt)" : " (should be spared)"));
			clear(ctx, s);
		}
	}

	private void ownerTest(final ClientGameTestContext ctx, final TestSingleplayerContext sp) {
		TestServerContext s = sp.getServer();
		s.runCommand("gamemode survival @p");
		s.runCommand("tp @p 300.5 " + Y + " 2.5");
		summonBike(s, 300.5, 0.5, "iron", false, ",Owner:\"6e1c5f1a-2b0c-4a5e-9a1d-3f4b5c6d7e8f\",OwnerName:\"Steve\"");
		ctx.waitTicks(10);
		String result = s.computeOnServer(server -> {
			ServerPlayer player = player(server);
			MotorcycleEntity bike = serverBike(server);
			StringBuilder sb = new StringBuilder();
			bike.interact(player, InteractionHand.MAIN_HAND, bike.position());
			sb.append("riding=").append(player.getVehicle() == bike);
			sb.append(" seat=").append(bike.seatOf(player));
			sb.append(" controls=").append(bike.getControllingPassenger() == player);
			player.stopRiding();
			player.setShiftKeyDown(true);
			bike.interact(player, InteractionHand.MAIN_HAND, bike.position());
			sb.append(" menuOpened=").append(player.containerMenu != player.inventoryMenu);
			player.setShiftKeyDown(false);
			float before = bike.getHealth();
			bike.hurtServer(player.level(), player.damageSources().playerAttack(player), 5.0F);
			sb.append(" damaged=").append(bike.getHealth() < before);
			return sb.toString();
		});
		this.check("owner lock", result.contains("riding=true") && result.contains("seat=PILLION") && result.contains("controls=false")
			&& result.contains("menuOpened=false") && result.contains("damaged=false"), result);

		s.runCommand("gamemode creative @p");
		ctx.waitTicks(5);
		String pickup = s.computeOnServer(server -> {
			ServerPlayer player = player(server);
			MotorcycleEntity bike = serverBike(server);
			bike.hurtServer(player.level(), player.damageSources().playerAttack(player), 1.0F);
			return "removed=" + bike.isRemoved() + " hasItem=" + player.getInventory().contains(st -> st.is(ModItems.MOTORCYCLE));
		});
		this.check("creative punch packs the bike up", pickup.contains("removed=true") && pickup.contains("hasItem=true"), pickup);
		s.runCommand("clear @p");
		clear(ctx, s);
	}

	private void sidecarTest(final ClientGameTestContext ctx, final TestSingleplayerContext sp) {
		TestServerContext s = sp.getServer();
		summonBike(s, 350.5, 0.5, "netherite", true, ",Color:11");
		mount(ctx, sp, 350.5, 0.5);
		String seats = s.computeOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			MotorcycleEntity bike = serverBike(server);
			for (int i = 0; i < 3; i++) {
				Sheep sheep = new Sheep(EntityTypes.SHEEP, level);
				sheep.snapTo(352.5, Y, 2.5 + i, 0.0F, 0.0F);
				level.addFreshEntity(sheep);
				sheep.setLeashedTo(player, true);
			}
			bike.interact(player, InteractionHand.MAIN_HAND, bike.position());
			return "driver=" + (bike.getControllingPassenger() == player)
				+ " left=" + (bike.getSeated(MotorcycleEntity.Seat.SIDECAR_LEFT) instanceof Sheep)
				+ " right=" + (bike.getSeated(MotorcycleEntity.Seat.SIDECAR_RIGHT) instanceof Sheep)
				+ " pillion=" + (bike.getSeated(MotorcycleEntity.Seat.PILLION) instanceof Sheep)
				+ " width=" + bike.getBbWidth();
		});
		this.check("leashed mobs fill sidecars then pillion", seats.contains("driver=true") && seats.contains("left=true")
			&& seats.contains("right=true") && seats.contains("pillion=true") && seats.contains("width=2.75"), seats);

		boolean slotLocked = s.computeOnServer(server -> {
			ServerPlayer player = player(server);
			MotorcycleEntity bike = serverBike(server);
			var menu = new com.doctorknepper.motorcycles.menu.MotorcycleMenu(99, player.getInventory(), bike);
			return !menu.getSlot(MotorcycleEntity.SLOT_SIDECAR_LEFT).mayPickup(player);
		});
		this.check("occupied sidecar can't be removed", slotLocked, "slot locked=" + slotLocked);

		ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		ctx.waitTicks(20);
		ctx.takeScreenshot("test_sidecars_front");
		ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		s.runOnServer(server -> serverBike(server).honk());
		ctx.getInput().holdKey(o -> o.keyUp);
		ctx.waitTicks(40);
		ctx.takeScreenshot("test_sidecars_riding_hud");
		ctx.getInput().releaseKey(o -> o.keyUp);
		ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		clear(ctx, s);
	}
}
