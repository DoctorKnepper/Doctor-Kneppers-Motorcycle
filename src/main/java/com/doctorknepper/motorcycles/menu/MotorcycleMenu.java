package com.doctorknepper.motorcycles.menu;

import static com.doctorknepper.motorcycles.entity.MotorcycleEntity.BAG_COUNT;
import static com.doctorknepper.motorcycles.entity.MotorcycleEntity.BAG_SIZE;
import static com.doctorknepper.motorcycles.entity.MotorcycleEntity.INVENTORY_SIZE;
import static com.doctorknepper.motorcycles.entity.MotorcycleEntity.SLOT_ENGINE;
import static com.doctorknepper.motorcycles.entity.MotorcycleEntity.SLOT_FIRST_BAG;
import static com.doctorknepper.motorcycles.entity.MotorcycleEntity.SLOT_FUEL;
import static com.doctorknepper.motorcycles.entity.MotorcycleEntity.SLOT_TANK;
import static com.doctorknepper.motorcycles.entity.MotorcycleEntity.SLOT_TRUNK;
import static com.doctorknepper.motorcycles.entity.MotorcycleEntity.STORAGE_START;
import static com.doctorknepper.motorcycles.entity.MotorcycleEntity.TRUNK_SIZE;
import static com.doctorknepper.motorcycles.entity.MotorcycleEntity.TRUNK_STORAGE_START;

import com.doctorknepper.motorcycles.entity.FuelHelper;
import com.doctorknepper.motorcycles.entity.MotorcycleEntity;
import com.doctorknepper.motorcycles.item.EngineItem;
import com.doctorknepper.motorcycles.registry.ModItems;
import com.doctorknepper.motorcycles.registry.ModMenus;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class MotorcycleMenu extends AbstractContainerMenu {
	// ---- layout (pixels from the GUI's top-left) ----
	public static final int UPGRADE_ROW_Y = 18;
	public static final int ENGINE_X = 8;
	public static final int TANK_X = 26;
	public static final int FIRST_BAG_X = 50;
	public static final int TRUNK_X = 104;
	public static final int FUEL_X = 152;
	public static final int STORAGE_X = 8;
	public static final int STORAGE_Y = 42;
	public static final int PLAYER_INV_Y = 162;
	public static final int GUI_HEIGHT = 244;

	private static final int PLAYER_START = INVENTORY_SIZE;
	private static final int HOTBAR_START = PLAYER_START + 27;
	private static final int PLAYER_END = PLAYER_START + 36;

	private final Container container;
	private final @Nullable MotorcycleEntity bike;

	/** Client-side constructor: the opening data is the bike's entity id. */
	public MotorcycleMenu(final int containerId, final Inventory playerInventory, final Integer bikeId) {
		this(
			containerId,
			playerInventory,
			new SimpleContainer(INVENTORY_SIZE),
			playerInventory.player.level().getEntity(bikeId) instanceof MotorcycleEntity bike ? bike : null
		);
	}

	/** Server-side constructor. */
	public MotorcycleMenu(final int containerId, final Inventory playerInventory, final MotorcycleEntity bike) {
		this(containerId, playerInventory, bike.getInventory(), bike);
	}

	private MotorcycleMenu(final int containerId, final Inventory playerInventory, final Container container, final @Nullable MotorcycleEntity bike) {
		super(ModMenus.MOTORCYCLE, containerId);
		checkContainerSize(container, INVENTORY_SIZE);
		this.container = container;
		this.bike = bike;
		container.startOpen(playerInventory.player);

		// Upgrade row
		this.addSlot(new UpgradeSlot(container, SLOT_ENGINE, ENGINE_X, UPGRADE_ROW_Y, s -> s.getItem() instanceof EngineItem, () -> true));
		this.addSlot(new UpgradeSlot(container, SLOT_TANK, TANK_X, UPGRADE_ROW_Y, s -> s.is(ModItems.FUEL_TANK), () -> true));
		this.addSlot(new FuelSlot(container, SLOT_FUEL, FUEL_X, UPGRADE_ROW_Y));
		for (int bag = 0; bag < BAG_COUNT; bag++) {
			int storageStart = STORAGE_START + bag * BAG_SIZE;
			this.addSlot(new UpgradeSlot(
				container, SLOT_FIRST_BAG + bag, FIRST_BAG_X + bag * 18, UPGRADE_ROW_Y,
				s -> s.is(ModItems.SADDLEBAG),
				() -> isRangeEmpty(container, storageStart, BAG_SIZE)
			));
		}
		this.addSlot(new UpgradeSlot(
			container, SLOT_TRUNK, TRUNK_X, UPGRADE_ROW_Y,
			s -> s.is(ModItems.TRUNK),
			() -> isRangeEmpty(container, TRUNK_STORAGE_START, TRUNK_SIZE)
		));

		// Saddlebag storage: one row per bag
		for (int bag = 0; bag < BAG_COUNT; bag++) {
			int upgradeSlot = SLOT_FIRST_BAG + bag;
			for (int col = 0; col < 9; col++) {
				this.addSlot(new StorageSlot(
					container, STORAGE_START + bag * BAG_SIZE + col,
					STORAGE_X + col * 18, STORAGE_Y + bag * 18,
					() -> container.getItem(upgradeSlot).is(ModItems.SADDLEBAG)
				));
			}
		}

		// Trunk storage: three rows
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new StorageSlot(
					container, TRUNK_STORAGE_START + row * 9 + col,
					STORAGE_X + col * 18, STORAGE_Y + (BAG_COUNT + row) * 18,
					() -> container.getItem(SLOT_TRUNK).is(ModItems.TRUNK)
				));
			}
		}

		this.addStandardInventorySlots(playerInventory, 8, PLAYER_INV_Y);
	}

	public @Nullable MotorcycleEntity getBike() {
		return this.bike;
	}

	public Container getContainer() {
		return this.container;
	}

	private static boolean isRangeEmpty(final Container container, final int start, final int size) {
		for (int i = start; i < start + size; i++) {
			if (!container.getItem(i).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean stillValid(final Player player) {
		return this.container.stillValid(player);
	}

	@Override
	public void removed(final Player player) {
		super.removed(player);
		this.container.stopOpen(player);
	}

	@Override
	public ItemStack quickMoveStack(final Player player, final int slotIndex) {
		Slot slot = this.slots.get(slotIndex);
		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();

		if (slotIndex < PLAYER_START) {
			if (!this.moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
				return ItemStack.EMPTY;
			}
		} else {
			boolean moved = false;
			for (int target : new int[] {SLOT_ENGINE, SLOT_TANK, SLOT_FIRST_BAG, SLOT_FIRST_BAG + 1, SLOT_FIRST_BAG + 2, SLOT_TRUNK}) {
				Slot targetSlot = this.slots.get(target);
				if (!targetSlot.hasItem() && targetSlot.mayPlace(stack) && this.moveItemStackTo(stack, target, target + 1, false)) {
					moved = true;
					break;
				}
			}
			if (!moved && this.slots.get(SLOT_FUEL).mayPlace(stack)) {
				moved = this.moveItemStackTo(stack, SLOT_FUEL, SLOT_FUEL + 1, false);
			}
			if (!moved) {
				moved = this.moveItemStackTo(stack, STORAGE_START, INVENTORY_SIZE, false);
			}
			if (!moved) {
				moved = slotIndex < HOTBAR_START
					? this.moveItemStackTo(stack, HOTBAR_START, PLAYER_END, false)
					: this.moveItemStackTo(stack, PLAYER_START, HOTBAR_START, false);
			}
			if (!moved) {
				return ItemStack.EMPTY;
			}
		}

		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}

		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}

		slot.onTake(player, stack);
		return original;
	}

	/** Holds one upgrade item. Saddlebags and the trunk can only come out once emptied. */
	public static class UpgradeSlot extends Slot {
		private final Predicate<ItemStack> accepts;
		private final BooleanSupplier removable;

		public UpgradeSlot(
			final Container container, final int index, final int x, final int y,
			final Predicate<ItemStack> accepts, final BooleanSupplier removable
		) {
			super(container, index, x, y);
			this.accepts = accepts;
			this.removable = removable;
		}

		@Override
		public boolean mayPlace(final ItemStack stack) {
			return this.accepts.test(stack);
		}

		@Override
		public boolean mayPickup(final Player player) {
			return this.removable.getAsBoolean();
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}
	}

	/** Takes any furnace fuel. */
	public static class FuelSlot extends Slot {
		public FuelSlot(final Container container, final int index, final int x, final int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(final ItemStack stack) {
			return FuelHelper.isFuel(stack);
		}
	}

	/** Storage that only exists while its saddlebag or trunk is installed. */
	public static class StorageSlot extends Slot {
		private final BooleanSupplier active;

		public StorageSlot(final Container container, final int index, final int x, final int y, final BooleanSupplier active) {
			super(container, index, x, y);
			this.active = active;
		}

		@Override
		public boolean isActive() {
			return this.active.getAsBoolean();
		}

		@Override
		public boolean mayPlace(final ItemStack stack) {
			return this.active.getAsBoolean() && !stack.is(ModItems.MOTORCYCLE);
		}
	}
}
