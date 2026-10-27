package com.doctorknepper.motorcycles.entity;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;

/** The bike's inventory. Any change re-syncs which upgrades are installed. */
public class MotorcycleContainer extends SimpleContainer {
	private final MotorcycleEntity bike;

	public MotorcycleContainer(final MotorcycleEntity bike) {
		super(MotorcycleEntity.INVENTORY_SIZE);
		this.bike = bike;
	}

	@Override
	public void setChanged() {
		super.setChanged();
		this.bike.onInventoryChanged();
	}

	@Override
	public boolean stillValid(final Player player) {
		return this.bike.isAlive() && player.isWithinEntityInteractionRange(this.bike, 4.0);
	}
}
