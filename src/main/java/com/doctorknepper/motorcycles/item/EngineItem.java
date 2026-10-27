package com.doctorknepper.motorcycles.item;

import com.doctorknepper.motorcycles.entity.EngineTier;
import net.minecraft.world.item.Item;

public class EngineItem extends Item {
	private final EngineTier tier;

	public EngineItem(final EngineTier tier, final Item.Properties properties) {
		super(properties);
		this.tier = tier;
	}

	public EngineTier tier() {
		return this.tier;
	}
}
