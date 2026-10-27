package com.doctorknepper.motorcycles.client.screen;

import static com.doctorknepper.motorcycles.menu.MotorcycleMenu.GUI_HEIGHT;
import static com.doctorknepper.motorcycles.menu.MotorcycleMenu.PLAYER_INV_Y;
import static com.doctorknepper.motorcycles.menu.MotorcycleMenu.STORAGE_X;
import static com.doctorknepper.motorcycles.menu.MotorcycleMenu.STORAGE_Y;
import static com.doctorknepper.motorcycles.menu.MotorcycleMenu.UPGRADE_ROW_Y;

import com.doctorknepper.motorcycles.entity.MotorcycleEntity;
import com.doctorknepper.motorcycles.menu.MotorcycleMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class MotorcycleScreen extends AbstractContainerScreen<MotorcycleMenu> {
	private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	private static final int PANEL = 0xFFC6C6C6;
	private static final int BORDER = 0xFF000000;
	private static final int HIGHLIGHT = 0xFFFFFFFF;
	private static final int SHADOW = 0xFF555555;
	private static final int LOCKED = 0xFF8B8B8B;
	private static final int LABEL = 0xFF404040;
	private static final int GAUGE_X = 128;
	private static final int GAUGE_W = 20;

	private static final Component[] SLOT_HINTS = {
		Component.translatable("gui.dkmotorcycles.slot.engine"),
		Component.translatable("gui.dkmotorcycles.slot.fuel_tank"),
		Component.translatable("gui.dkmotorcycles.slot.fuel"),
		Component.translatable("gui.dkmotorcycles.slot.saddlebag"),
		Component.translatable("gui.dkmotorcycles.slot.saddlebag"),
		Component.translatable("gui.dkmotorcycles.slot.saddlebag"),
		Component.translatable("gui.dkmotorcycles.slot.trunk"),
	};

	public MotorcycleScreen(final MotorcycleMenu menu, final Inventory inventory, final Component title) {
		super(menu, inventory, title, 176, GUI_HEIGHT);
		this.inventoryLabelY = PLAYER_INV_Y - 11;
	}

	@Override
	public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int x = this.leftPos;
		int y = this.topPos;
		this.drawPanel(graphics, x, y, this.imageWidth, this.imageHeight);

		// Locked storage rows show what unlocks them
		var container = this.menu.getContainer();
		for (int bag = 0; bag < MotorcycleEntity.BAG_COUNT; bag++) {
			if (container.getItem(MotorcycleEntity.SLOT_FIRST_BAG + bag).isEmpty()) {
				this.drawLocked(graphics, x + STORAGE_X - 1, y + STORAGE_Y - 1 + bag * 18, 1, Component.translatable("gui.dkmotorcycles.locked.saddlebag"));
			}
		}
		if (container.getItem(MotorcycleEntity.SLOT_TRUNK).isEmpty()) {
			this.drawLocked(
				graphics, x + STORAGE_X - 1, y + STORAGE_Y - 1 + MotorcycleEntity.BAG_COUNT * 18, 3, Component.translatable("gui.dkmotorcycles.locked.trunk")
			);
		}

		for (Slot slot : this.menu.slots) {
			if (slot.isActive()) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x + slot.x - 1, y + slot.y - 1, 18, 18);
			}
		}

		this.drawFuelGauge(graphics, x + GAUGE_X, y + UPGRADE_ROW_Y - 1);
	}

	private void drawPanel(final GuiGraphicsExtractor graphics, final int x, final int y, final int w, final int h) {
		graphics.fill(x + 2, y, x + w - 2, y + 1, BORDER);
		graphics.fill(x + 2, y + h - 1, x + w - 2, y + h, BORDER);
		graphics.fill(x, y + 2, x + 1, y + h - 2, BORDER);
		graphics.fill(x + w - 1, y + 2, x + w, y + h - 2, BORDER);
		graphics.fill(x + 1, y + 1, x + 2, y + 2, BORDER);
		graphics.fill(x + w - 2, y + 1, x + w - 1, y + 2, BORDER);
		graphics.fill(x + 1, y + h - 2, x + 2, y + h - 1, BORDER);
		graphics.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, BORDER);
		graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, PANEL);
		graphics.fill(x + 2, y + 1, x + w - 3, y + 3, HIGHLIGHT);
		graphics.fill(x + 1, y + 2, x + 3, y + h - 3, HIGHLIGHT);
		graphics.fill(x + 3, y + h - 3, x + w - 2, y + h - 1, SHADOW);
		graphics.fill(x + w - 3, y + 3, x + w - 1, y + h - 2, SHADOW);
		graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, PANEL);
	}

	private void drawLocked(final GuiGraphicsExtractor graphics, final int x, final int y, final int rows, final Component text) {
		int w = 9 * 18;
		int h = rows * 18;
		graphics.fill(x, y, x + w, y + h, SHADOW);
		graphics.fill(x + 1, y + 1, x + w, y + h, HIGHLIGHT);
		graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, LOCKED);
		graphics.centeredText(this.font, text, x + w / 2, y + (h - 8) / 2, 0xFFE0E0E0);
	}

	private void drawFuelGauge(final GuiGraphicsExtractor graphics, final int x, final int y) {
		MotorcycleEntity bike = this.menu.getBike();
		float fraction = bike == null ? 0.0F : bike.getFuel() / (float) bike.getFuelCapacity();
		int h = 18;
		graphics.fill(x, y, x + GAUGE_W, y + h, SHADOW);
		graphics.fill(x + 1, y + 1, x + GAUGE_W, y + h, HIGHLIGHT);
		graphics.fill(x + 1, y + 1, x + GAUGE_W - 1, y + h - 1, 0xFF2B2B2B);
		int innerH = h - 4;
		int filled = Math.round(innerH * fraction);
		if (filled > 0) {
			int color = fraction > 0.5F ? 0xFF4CC24C : fraction > 0.2F ? 0xFFE0C030 : 0xFFD04030;
			graphics.fill(x + 3, y + 2 + innerH - filled, x + GAUGE_W - 3, y + 2 + innerH, color);
		}
		if (bike != null && bike.getBoost() > 0) {
			graphics.fill(x + 3, y + 2, x + GAUGE_W - 3, y + 3, 0xFFFF8020);
		}
	}

	@Override
	protected void extractLabels(final GuiGraphicsExtractor graphics, final int xm, final int ym) {
		super.extractLabels(graphics, xm, ym);
		MotorcycleEntity bike = this.menu.getBike();
		if (bike != null) {
			int fuelPercent = Math.round(100.0F * bike.getFuel() / bike.getFuelCapacity());
			Component status = Component.translatable(
				"gui.dkmotorcycles.status", fuelPercent, Math.round(bike.getHealth()), Math.round(bike.getMaxHealth())
			);
			graphics.text(this.font, status, this.imageWidth - 8 - this.font.width(status), this.inventoryLabelY, LABEL, false);
		}
	}

	@Override
	protected void extractTooltip(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.menu.getCarried().isEmpty()) {
			int index = this.hoveredSlot.index;
			if (index >= 0 && index < SLOT_HINTS.length) {
				graphics.setTooltipForNextFrame(this.font, SLOT_HINTS[index], mouseX, mouseY);
			}
		}

		int gx = this.leftPos + GAUGE_X;
		int gy = this.topPos + UPGRADE_ROW_Y - 1;
		MotorcycleEntity bike = this.menu.getBike();
		if (bike != null && mouseX >= gx && mouseX < gx + GAUGE_W && mouseY >= gy && mouseY < gy + 18) {
			int seconds = bike.getFuel() / 20;
			graphics.setTooltipForNextFrame(
				this.font, Component.translatable("gui.dkmotorcycles.fuel_time", seconds / 60, String.format("%02d", seconds % 60)), mouseX, mouseY
			);
		}
	}
}
