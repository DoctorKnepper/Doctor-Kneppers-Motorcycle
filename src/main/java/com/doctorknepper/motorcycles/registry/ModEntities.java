package com.doctorknepper.motorcycles.registry;

import com.doctorknepper.motorcycles.DKMotorcycles;
import com.doctorknepper.motorcycles.entity.MotorcycleEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final ResourceKey<EntityType<?>> MOTORCYCLE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, DKMotorcycles.id("motorcycle"));

	public static final EntityType<MotorcycleEntity> MOTORCYCLE = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		MOTORCYCLE_KEY,
		EntityType.Builder.of(MotorcycleEntity::new, MobCategory.MISC)
			.sized(1.0F, 1.1F)
			.eyeHeight(0.9F)
			.noLootTable()
			.clientTrackingRange(10)
			.build(MOTORCYCLE_KEY)
	);

	private ModEntities() {
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(MOTORCYCLE, MotorcycleEntity.createAttributes());
	}
}
