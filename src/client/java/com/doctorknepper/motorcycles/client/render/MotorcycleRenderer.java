package com.doctorknepper.motorcycles.client.render;

import com.doctorknepper.motorcycles.DKMotorcycles;
import com.doctorknepper.motorcycles.entity.EngineTier;
import com.doctorknepper.motorcycles.entity.MotorcycleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public class MotorcycleRenderer extends EntityRenderer<MotorcycleEntity, MotorcycleRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(DKMotorcycles.id("motorcycle"), "main");
	private static final Identifier TEXTURE = DKMotorcycles.id("textures/entity/motorcycle.png");
	private static final int FULL_BRIGHT = 0xF000F0;

	private final MotorcycleModel frameModel;
	private final MotorcycleModel paintModel;
	private final MotorcycleModel engineModel;
	private final MotorcycleModel lightModel;

	public MotorcycleRenderer(final EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.6F;
		this.frameModel = new MotorcycleModel(context.bakeLayer(LAYER), MotorcycleModel.Group.FRAME);
		this.paintModel = new MotorcycleModel(context.bakeLayer(LAYER), MotorcycleModel.Group.PAINT);
		this.engineModel = new MotorcycleModel(context.bakeLayer(LAYER), MotorcycleModel.Group.ENGINE);
		this.lightModel = new MotorcycleModel(context.bakeLayer(LAYER), MotorcycleModel.Group.LIGHT);
	}

	@Override
	public MotorcycleRenderState createRenderState() {
		return new MotorcycleRenderState();
	}

	@Override
	public void extractRenderState(final MotorcycleEntity entity, final MotorcycleRenderState state, final float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.yRot = entity.getYRot(partialTicks);
		state.wheelRotation = entity.getWheelRotation(partialTicks);
		state.hurtTime = Math.max(0.0F, entity.hurtTime - partialTicks);
		state.color = entity.getColor();
		state.engine = entity.getEngine();
		state.upgrades = entity.getUpgrades();
		state.lightsOn = entity.isHeadlightOn() && entity.getControllingPassenger() != null && entity.getEngine() != EngineTier.NONE;
	}

	@Override
	public void submit(
		final MotorcycleRenderState state, final PoseStack poseStack, final SubmitNodeCollector collector, final CameraRenderState camera
	) {
		poseStack.pushPose();
		poseStack.rotateDegrees(Axis.YP, 180.0F - state.yRot);
		if (state.hurtTime > 0.0F) {
			poseStack.rotateDegrees(Axis.ZP, Mth.sin(state.hurtTime) * state.hurtTime * 0.4F);
		}
		poseStack.scale(-1.0F, -1.0F, 1.0F);

		int light = state.lightCoords;
		int overlay = OverlayTexture.NO_OVERLAY;
		var renderType = this.frameModel.renderType(TEXTURE);

		collector.submitModel(this.frameModel, state, poseStack, renderType, light, overlay, -1, null, state.outlineColor);
		collector.submitModel(
			this.paintModel, state, poseStack, renderType, light, overlay, ARGB.opaque(state.color.getTextureDiffuseColor()), null, state.outlineColor
		);
		if (state.engine != EngineTier.NONE) {
			collector.submitModel(this.engineModel, state, poseStack, renderType, light, overlay, ARGB.opaque(state.engine.color()), null, state.outlineColor);
		}
		collector.submitModel(this.lightModel, state, poseStack, renderType, state.lightsOn ? FULL_BRIGHT : light, overlay, -1, null, state.outlineColor);

		poseStack.popPose();
		super.submit(state, poseStack, collector, camera);
	}
}
