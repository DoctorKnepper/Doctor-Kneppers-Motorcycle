package com.doctorknepper.motorcycles;

import com.doctorknepper.motorcycles.entity.MotorcycleEntity;
import com.doctorknepper.motorcycles.network.ToggleHeadlightPayload;
import com.doctorknepper.motorcycles.registry.ModEntities;
import com.doctorknepper.motorcycles.registry.ModItems;
import com.doctorknepper.motorcycles.registry.ModMenus;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DKMotorcycles implements ModInitializer {
	public static final String MOD_ID = "dkmotorcycles";
	public static final Logger LOGGER = LoggerFactory.getLogger("Doctor Knepper's Motorcycles");

	public static Identifier id(final String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModEntities.init();
		ModItems.init();
		ModMenus.init();

		PayloadTypeRegistry.serverboundPlay().register(ToggleHeadlightPayload.TYPE, ToggleHeadlightPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ToggleHeadlightPayload.TYPE, (payload, context) -> {
			if (context.player().getVehicle() instanceof MotorcycleEntity bike && bike.getControllingPassenger() == context.player()) {
				bike.toggleHeadlight();
			}
		});

		LOGGER.info("Doctor Knepper's Motorcycles loaded. Start your engines!");
	}
}
