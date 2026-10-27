package com.doctorknepper.motorcycles.client;

import com.doctorknepper.motorcycles.entity.EngineTier;
import com.doctorknepper.motorcycles.entity.MotorcycleEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Speedometer and fuel bar, bottom-left of the screen while riding. */
public final class MotorcycleHud {
	private MotorcycleHud() {
	}

	public static void extract(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || !(minecraft.player.getVehicle() instanceof MotorcycleEntity bike)) {
			return;
		}

		int x = 6;
		int y = graphics.guiHeight() - 34;
		double dx = bike.getX() - bike.xo;
		double dz = bike.getZ() - bike.zo;
		int kmh = (int) Math.round(Math.sqrt(dx * dx + dz * dz) * 20.0 * 3.6);

		Component speed = Component.translatable("hud.dkmotorcycles.speed", kmh);
		graphics.fill(x - 3, y - 3, x + 92, y + 23, 0x80000000);
		graphics.text(minecraft.font, speed, x, y, 0xFFFFFFFF, true);

		if (bike.isHeadlightOn() && bike.getEngine() != EngineTier.NONE) {
			graphics.text(minecraft.font, Component.translatable("hud.dkmotorcycles.light_on"), x + 62, y, 0xFFFFF2A0, true);
		}

		float fraction = bike.getFuel() / (float) bike.getFuelCapacity();
		int barY = y + 12;
		int barW = 86;
		graphics.fill(x, barY, x + barW, barY + 6, 0xFF2B2B2B);
		int filled = Math.round((barW - 2) * fraction);
		int color = fraction > 0.5F ? 0xFF4CC24C : fraction > 0.2F ? 0xFFE0C030 : 0xFFD04030;
		if (filled > 0) {
			graphics.fill(x + 1, barY + 1, x + 1 + filled, barY + 5, color);
		}
		if (bike.getBoost() > 0) {
			graphics.fill(x + 1, barY + 1, x + 1 + filled, barY + 2, 0xFFFF8020);
		}
		if (!bike.canDrive()) {
			Component warning = Component.translatable(
				bike.getEngine() == EngineTier.NONE ? "hud.dkmotorcycles.no_engine" : "hud.dkmotorcycles.no_fuel"
			);
			graphics.text(minecraft.font, warning, x, y - 12, 0xFFFF6050, true);
		}
	}
}
