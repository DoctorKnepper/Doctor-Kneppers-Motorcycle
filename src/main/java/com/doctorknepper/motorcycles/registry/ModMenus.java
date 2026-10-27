package com.doctorknepper.motorcycles.registry;

import com.doctorknepper.motorcycles.DKMotorcycles;
import com.doctorknepper.motorcycles.menu.MotorcycleMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModMenus {
	/** Opening data is the motorcycle's entity id, so the client can find the bike. */
	public static final ExtendedMenuType<MotorcycleMenu, Integer> MOTORCYCLE = Registry.register(
		BuiltInRegistries.MENU, DKMotorcycles.id("motorcycle"), new ExtendedMenuType<>(MotorcycleMenu::new, ByteBufCodecs.VAR_INT)
	);

	private ModMenus() {
	}

	public static void init() {
	}
}
