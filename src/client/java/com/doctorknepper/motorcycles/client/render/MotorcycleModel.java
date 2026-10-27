package com.doctorknepper.motorcycles.client.render;

import com.doctorknepper.motorcycles.entity.MotorcycleEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * One baked copy of the motorcycle model per tint group. Each copy shows only its own group,
 * so the renderer can draw the frame as-is, the paint in the dye colour, the engine in its tier
 * colour and the lights full-bright.
 */
public class MotorcycleModel extends EntityModel<MotorcycleRenderState> {
	public enum Group {
		FRAME("frame"),
		PAINT("paint"),
		ENGINE("engine"),
		LIGHT("light");

		final String partName;

		Group(final String partName) {
			this.partName = partName;
		}
	}

	private final Group group;
	private final ModelPart frame;
	private final ModelPart paint;
	private final ModelPart engine;
	private final ModelPart light;
	private final ModelPart frontWheel;
	private final ModelPart rearWheel;
	private final ModelPart[] saddlebags;
	private final ModelPart trunk;

	public MotorcycleModel(final ModelPart root, final Group group) {
		super(root);
		this.group = group;
		this.frame = root.getChild("frame");
		this.paint = root.getChild("paint");
		this.engine = root.getChild("engine");
		this.light = root.getChild("light");
		this.frontWheel = this.frame.getChild("front_wheel");
		this.rearWheel = this.frame.getChild("rear_wheel");
		this.saddlebags = new ModelPart[] {
			this.frame.getChild("saddlebag_1"), this.frame.getChild("saddlebag_2"), this.frame.getChild("saddlebag_3")
		};
		this.trunk = this.frame.getChild("trunk");
	}

	@Override
	public void setupAnim(final MotorcycleRenderState state) {
		super.setupAnim(state);
		this.frame.visible = this.group == Group.FRAME;
		this.paint.visible = this.group == Group.PAINT;
		this.engine.visible = this.group == Group.ENGINE;
		this.light.visible = this.group == Group.LIGHT;

		this.frontWheel.xRot = state.wheelRotation;
		this.rearWheel.xRot = state.wheelRotation;
		for (int bag = 0; bag < this.saddlebags.length; bag++) {
			this.saddlebags[bag].visible = (state.upgrades & MotorcycleEntity.bagBit(bag)) != 0;
		}
		this.trunk.visible = (state.upgrades & MotorcycleEntity.UPGRADE_TRUNK) != 0;
	}
}
