package com.doctorknepper.motorcycles.entity;

/**
 * Engine tiers, from copper up to netherite. Top speeds live in the config
 * ({@link com.doctorknepper.motorcycles.config.MotorcycleConfig}).
 */
public enum EngineTier {
	NONE(0x000000),
	COPPER(0xE3874F),
	IRON(0xE6E6E6),
	DIAMOND(0x6FF0E2),
	NETHERITE(0x5A5154);

	/** Ridden movement speed of 1.0 moves the bike about this many blocks per second on flat ground. */
	public static final double BLOCKS_PER_SECOND_PER_SPEED = 44.0;

	private final int color;

	EngineTier(final int color) {
		this.color = color;
	}

	/** RGB tint applied to the engine block on the model. */
	public int color() {
		return this.color;
	}

	public static EngineTier byId(final int id) {
		EngineTier[] values = values();
		return id >= 0 && id < values.length ? values[id] : NONE;
	}
}
