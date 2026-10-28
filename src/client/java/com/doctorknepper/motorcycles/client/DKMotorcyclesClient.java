package com.doctorknepper.motorcycles.client;

import com.doctorknepper.motorcycles.DKMotorcycles;
import com.doctorknepper.motorcycles.client.render.MotorcycleModelLayer;
import com.doctorknepper.motorcycles.client.render.MotorcycleRenderer;
import com.doctorknepper.motorcycles.client.screen.MotorcycleScreen;
import com.doctorknepper.motorcycles.entity.MotorcycleEntity;
import com.doctorknepper.motorcycles.network.HornPayload;
import com.doctorknepper.motorcycles.network.ToggleHeadlightPayload;
import com.doctorknepper.motorcycles.registry.ModEntities;
import com.doctorknepper.motorcycles.registry.ModMenus;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;

public class DKMotorcyclesClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(DKMotorcycles.id("motorcycles"));
	private static KeyMapping headlightKey;
	private static KeyMapping hornKey;
	private static EngineSoundInstance engineSound;

	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(MotorcycleRenderer.LAYER, MotorcycleModelLayer::create);
		EntityRendererRegistry.register(ModEntities.MOTORCYCLE, MotorcycleRenderer::new);
		MenuScreens.register(ModMenus.MOTORCYCLE, MotorcycleScreen::new);

		headlightKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.dkmotorcycles.headlight", InputConstants.KEY_H, CATEGORY)
		);

		hornKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.dkmotorcycles.horn", InputConstants.KEY_G, CATEGORY)
		);

		// Holding Sprint lets the rider drive over an edge the bike would otherwise brake at.
		MotorcycleEntity.edgeOverrideHeld = () -> Minecraft.getInstance().options.keySprint.isDown();

		HudElementRegistry.addLast(DKMotorcycles.id("motorcycle_hud"), MotorcycleHud::extract);
		// The bike's health is shown in the mod's own panel, so hide vanilla's two rows of vehicle hearts.
		HudElementRegistry.replaceElement(VanillaHudElements.MOUNT_HEALTH, vanilla -> (graphics, deltaTracker) -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || !(minecraft.player.getVehicle() instanceof MotorcycleEntity)) {
				vanilla.extractRenderState(graphics, deltaTracker);
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(DKMotorcyclesClient::onClientTick);
	}

	private static void onClientTick(final Minecraft minecraft) {
		while (headlightKey.consumeClick()) {
			if (minecraft.player != null && minecraft.player.getVehicle() instanceof MotorcycleEntity bike && bike.getControllingPassenger() == minecraft.player) {
				ClientPlayNetworking.send(new ToggleHeadlightPayload());
			}
		}

		while (hornKey.consumeClick()) {
			if (minecraft.player != null && minecraft.player.getVehicle() instanceof MotorcycleEntity bike && bike.getControllingPassenger() == minecraft.player) {
				ClientPlayNetworking.send(new HornPayload());
			}
		}

		if (minecraft.player != null && minecraft.player.getVehicle() instanceof MotorcycleEntity bike
			&& bike.getControllingPassenger() == minecraft.player && bike.canDrive()) {
			if (engineSound == null || engineSound.isStopped()) {
				engineSound = new EngineSoundInstance(bike);
				minecraft.getSoundManager().play(engineSound);
			}
		}
	}
}
