package com.doctorknepper.motorcycles.client;

import com.doctorknepper.motorcycles.entity.MotorcycleEntity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/** A low engine hum that rises in pitch with speed. Uses a pitched-down vanilla loop. */
public class EngineSoundInstance extends AbstractTickableSoundInstance {
	private final MotorcycleEntity bike;

	public EngineSoundInstance(final MotorcycleEntity bike) {
		super(SoundEvents.BEE_LOOP, SoundSource.NEUTRAL, bike.getRandom());
		this.bike = bike;
		this.looping = true;
		this.delay = 0;
		this.volume = 0.0F;
		this.pitch = 0.5F;
		this.x = bike.getX();
		this.y = bike.getY();
		this.z = bike.getZ();
	}

	@Override
	public void tick() {
		if (this.bike.isRemoved() || this.bike.getControllingPassenger() == null || !this.bike.canDrive()) {
			this.stop();
			return;
		}

		this.x = this.bike.getX();
		this.y = this.bike.getY();
		this.z = this.bike.getZ();
		double dx = this.bike.getX() - this.bike.xo;
		double dz = this.bike.getZ() - this.bike.zo;
		float speed = (float) Math.sqrt(dx * dx + dz * dz);
		float top = Math.max(0.2F, this.bike.getTopSpeed() * 2.2F);
		float load = Mth.clamp(speed / top, 0.0F, 1.0F);
		this.pitch = 0.45F + load * 0.75F;
		this.volume = 0.35F + load * 0.35F;
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}
}
