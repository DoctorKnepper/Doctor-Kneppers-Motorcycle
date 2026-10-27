package com.doctorknepper.motorcycles.entity;

/**
 * Engine tiers, from copper up to netherite.
 * {@code speed} is the ridden movement speed; roughly 43 blocks/second per 1.0.
 */
public enum EngineTier {
	NONE(0.0F, 0x000000),
	COPPER(0.30F, 0xE3874F),
	IRON(0.42F, 0xE6E6E6),
	DIAMOND(0.58F, 0x6FF0E2),
	NETHERITE(0.80F, 0x5A5154);

	private final float speed;
	private final int color;

	EngineTier(final float speed, final int color) {
		this.speed = speed;
		this.color = color;
	}

	public float speed() {
		return this.speed;
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
