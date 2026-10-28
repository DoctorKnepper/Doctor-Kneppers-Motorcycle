package com.doctorknepper.motorcycles.client;

import com.doctorknepper.motorcycles.entity.EngineTier;
import com.doctorknepper.motorcycles.entity.MotorcycleEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Speedometer, fuel and bike health, bottom-left of the screen while riding. */
public final class MotorcycleHud {
	private static final int BAR_W = 86;

	private MotorcycleHud() {
	}

	public static void extract(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || !(minecraft.player.getVehicle() instanceof MotorcycleEntity bike)) {
			return;
		}
		Font font = minecraft.font;

		int x = 6;
		int y = graphics.guiHeight() - 42;
		double dx = bike.getX() - bike.xo;
		double dz = bike.getZ() - bike.zo;
		int kmh = (int) Math.round(Math.sqrt(dx * dx + dz * dz) * 20.0 * 3.6);

		graphics.fill(x - 3, y - 3, x + BAR_W + 6, y + 31, 0x80000000);
		graphics.text(font, Component.translatable("hud.dkmotorcycles.speed", kmh), x, y, 0xFFFFFFFF, true);
		if (bike.isHeadlightOn() && bike.getEngine() != EngineTier.NONE) {
			graphics.text(font, Component.translatable("hud.dkmotorcycles.light_on"), x + 62, y, 0xFFFFF2A0, true);
		}

		// Fuel
		float fuel = bike.getFuel() / (float) bike.getFuelCapacity();
		int fuelY = y + 12;
		int fuelColor = fuel > 0.5F ? 0xFF4CC24C : fuel > 0.2F ? 0xFFE0C030 : 0xFFD04030;
		int filled = bar(graphics, x, fuelY, fuel, fuelColor);
		if (bike.getBoost() > 0) {
			graphics.fill(x + 1, fuelY + 1, x + 1 + filled, fuelY + 2, 0xFFFF8020);
		}

		// Bike health
		float health = bike.getHealth() / bike.getMaxHealth();
		int healthColor = health > 0.5F ? 0xFFD8D8DC : health > 0.25F ? 0xFFE0A040 : 0xFFD04030;
		bar(graphics, x, fuelY + 8, health, healthColor);
		graphics.text(font, Component.literal("♥"), x + BAR_W + 1, fuelY + 6, 0xFFE04848, true);

		// Notices above the panel
		Component warning = null;
		int warningColor = 0xFFFF6050;
		if (bike.isAfloat()) {
			warning = Component.translatable("hud.dkmotorcycles.flooded");
			warningColor = 0xFF60B0FF;
		} else if (bike.getEngine() == EngineTier.NONE) {
			warning = Component.translatable("hud.dkmotorcycles.no_engine");
		} else if (bike.getFuel() <= 0) {
			warning = Component.translatable("hud.dkmotorcycles.no_fuel");
		} else if (bike.isEdgeWarning()) {
			warning = Component.translatable("hud.dkmotorcycles.edge", minecraft.options.keySprint.getTranslatedKeyMessage());
			warningColor = 0xFFFFD050;
		}
		if (warning != null) {
			graphics.text(font, warning, x, y - 12, warningColor, true);
		}
	}

	/** Draws a 6px bar and returns how many pixels are filled. */
	private static int bar(final GuiGraphicsExtractor graphics, final int x, final int y, final float fraction, final int color) {
		graphics.fill(x, y, x + BAR_W, y + 6, 0xFF2B2B2B);
		int filled = Math.round((BAR_W - 2) * Math.max(0.0F, Math.min(1.0F, fraction)));
		if (filled > 0) {
			graphics.fill(x + 1, y + 1, x + 1 + filled, y + 5, color);
		}
		return filled;
	}
}
