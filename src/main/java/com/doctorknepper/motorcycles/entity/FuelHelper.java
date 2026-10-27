package com.doctorknepper.motorcycles.entity;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Turns furnace fuel into motorcycle fuel. Any item a furnace can burn works; a few give bonuses.
 */
public final class FuelHelper {
	/** Ticks of riding gained per tick of furnace burn time. */
	public static final float BASE_MULTIPLIER = 2.0F;
	/** Fallback burn time if the data-driven value can't be resolved. */
	private static final int FALLBACK_BURN_TIME = 300;

	private FuelHelper() {
	}

	public static boolean isFuel(final ItemStack stack) {
		return stack.has(DataComponents.COOKING_FUEL);
	}

	/** Furnace burn time in ticks, resolved exactly like a regular furnace would. */
	public static int furnaceBurnTime(final ServerLevel level, final ItemStack stack) {
		CookingFuel fuel = stack.get(DataComponents.COOKING_FUEL);
		if (fuel == null) {
			return 0;
		}

		try {
			BlockState furnaceState = Blocks.FURNACE.defaultBlockState();
			FurnaceBlockEntity furnace = new FurnaceBlockEntity(BlockPos.ZERO, furnaceState);
			furnace.setLevel(level);
			LootContext context = new LootContext.Builder(
					new LootParams.Builder(level)
						.withParameter(LootContextParams.BLOCK_STATE, furnaceState)
						.withParameter(LootContextParams.BLOCK_ENTITY, furnace)
						.withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
						.withParameter(LootContextParams.CONTAINER, furnace)
						.create(LootContextParamSets.CONTAINER_PROCESS)
				)
				.create(Optional.empty());
			return Math.max(0, fuel.burnTime().get(context, FALLBACK_BURN_TIME));
		} catch (RuntimeException e) {
			return FALLBACK_BURN_TIME;
		}
	}

	/** Bonus multiplier on top of {@link #BASE_MULTIPLIER} for special fuels. */
	public static float bonusMultiplier(final ItemStack stack) {
		if (stack.is(Items.BLAZE_ROD)) {
			return 1.25F;
		}
		if (stack.is(Items.DRIED_KELP_BLOCK) || stack.is(Items.LAVA_BUCKET)) {
			return 1.5F;
		}
		return 1.0F;
	}

	/** Blaze rods also give a speed boost for as long as their fuel lasts. */
	public static boolean givesBoost(final ItemStack stack) {
		return stack.is(Items.BLAZE_ROD);
	}

	/** What's left in the slot after burning one item (lava bucket gives its bucket back). */
	public static ItemStack remainder(final ItemStack stack) {
		if (stack.is(Items.LAVA_BUCKET)) {
			return new ItemStack(Items.BUCKET);
		}
		return ItemStack.EMPTY;
	}
}
