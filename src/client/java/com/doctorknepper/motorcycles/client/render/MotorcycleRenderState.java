package com.doctorknepper.motorcycles.client.render;

import com.doctorknepper.motorcycles.entity.EngineTier;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.item.DyeColor;

public class MotorcycleRenderState extends EntityRenderState {
	public float yRot;
	public float wheelRotation;
	public float hurtTime;
	public DyeColor color = DyeColor.RED;
	public EngineTier engine = EngineTier.NONE;
	public int upgrades;
	public boolean lightsOn;
}
