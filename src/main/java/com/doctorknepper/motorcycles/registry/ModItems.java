package com.doctorknepper.motorcycles.registry;

import com.doctorknepper.motorcycles.DKMotorcycles;
import com.doctorknepper.motorcycles.entity.EngineTier;
import com.doctorknepper.motorcycles.item.EngineItem;
import com.doctorknepper.motorcycles.item.MotorcycleItem;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModItems {
	public static final Item MOTORCYCLE = register("motorcycle", MotorcycleItem::new, new Item.Properties().stacksTo(1));

	public static final Item COPPER_ENGINE = register("copper_engine", p -> new EngineItem(EngineTier.COPPER, p), new Item.Properties().stacksTo(1));
	public static final Item IRON_ENGINE = register("iron_engine", p -> new EngineItem(EngineTier.IRON, p), new Item.Properties().stacksTo(1));
	public static final Item DIAMOND_ENGINE = register("diamond_engine", p -> new EngineItem(EngineTier.DIAMOND, p), new Item.Properties().stacksTo(1));
	public static final Item NETHERITE_ENGINE = register(
		"netherite_engine", p -> new EngineItem(EngineTier.NETHERITE, p), new Item.Properties().stacksTo(1).fireResistant()
	);

	public static final Item FUEL_TANK = register("fuel_tank", Item::new, new Item.Properties().stacksTo(1));
	public static final Item SADDLEBAG = register("saddlebag", Item::new, new Item.Properties().stacksTo(1));
	public static final Item TRUNK = register("trunk", Item::new, new Item.Properties().stacksTo(1));
	public static final Item SIDECAR = register("sidecar", Item::new, new Item.Properties().stacksTo(1));

	private ModItems() {
	}

	private static Item register(final String name, final Function<Item.Properties, Item> factory, final Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, DKMotorcycles.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void init() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.accept(MOTORCYCLE);
			output.accept(COPPER_ENGINE);
			output.accept(IRON_ENGINE);
			output.accept(DIAMOND_ENGINE);
			output.accept(NETHERITE_ENGINE);
			output.accept(FUEL_TANK);
			output.accept(SADDLEBAG);
			output.accept(TRUNK);
			output.accept(SIDECAR);
		});
	}
}
