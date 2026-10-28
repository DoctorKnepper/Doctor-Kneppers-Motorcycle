package com.doctorknepper.motorcycles.config;

import com.doctorknepper.motorcycles.DKMotorcycles;
import com.doctorknepper.motorcycles.entity.EngineTier;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Server-side settings, stored in config/dkmotorcycles.json. Values the rider's client needs
 * (top speed, edge braking) are sent to it through the bike's synced data, so a server's
 * settings always win over whatever a joining player has locally.
 */
public final class MotorcycleConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static MotorcycleConfig instance = new MotorcycleConfig();

	/** Top speed of each engine, in blocks per second. Sprinting is about 5.6. */
	public EngineSpeeds engineSpeedsBlocksPerSecond = new EngineSpeeds();

	/** 1.0 = normal. 0.5 makes fuel last twice as long. */
	public double fuelUseMultiplier = 1.0;

	/** 1.0 = normal. 0 turns ramming damage off. */
	public double rammingDamageMultiplier = 1.0;

	/** Whether running into other players hurts them. Pets, villagers and named mobs are always spared. */
	public boolean rammingHurtsPlayers = false;

	/** The bike brakes instead of driving off a drop deeper than this many blocks. 0 turns edge braking off. */
	public int edgeBrakingDepth = 3;

	/** Only the player who placed a bike can drive it, open it, pick it up or damage it. Others may ride along. */
	public boolean ownerLock = true;

	public static final class EngineSpeeds {
		public double copper = 8.0;
		public double iron = 11.0;
		public double diamond = 14.0;
		public double netherite = 18.0;
	}

	public static MotorcycleConfig get() {
		return instance;
	}

	/** Blocks per second for an engine tier, from the config. */
	public double blocksPerSecond(final EngineTier tier) {
		return switch (tier) {
			case NONE -> 0.0;
			case COPPER -> this.engineSpeedsBlocksPerSecond.copper;
			case IRON -> this.engineSpeedsBlocksPerSecond.iron;
			case DIAMOND -> this.engineSpeedsBlocksPerSecond.diamond;
			case NETHERITE -> this.engineSpeedsBlocksPerSecond.netherite;
		};
	}

	public static void load() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve(DKMotorcycles.MOD_ID + ".json");
		MotorcycleConfig loaded = null;
		if (Files.exists(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				loaded = GSON.fromJson(reader, MotorcycleConfig.class);
			} catch (IOException | RuntimeException e) {
				DKMotorcycles.LOGGER.warn("Couldn't read {}, using defaults: {}", file, e.getMessage());
			}
		}
		instance = loaded == null ? new MotorcycleConfig() : loaded;
		if (instance.engineSpeedsBlocksPerSecond == null) {
			instance.engineSpeedsBlocksPerSecond = new EngineSpeeds();
		}
		// Write back so new settings appear in older files.
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			DKMotorcycles.LOGGER.warn("Couldn't write {}: {}", file, e.getMessage());
		}
	}
}
