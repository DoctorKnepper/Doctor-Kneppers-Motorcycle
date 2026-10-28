package com.doctorknepper.motorcycles.item;

import com.doctorknepper.motorcycles.entity.MotorcycleEntity;
import com.doctorknepper.motorcycles.registry.ModEntities;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Places the motorcycle on the block you're looking at, like a boat. */
public class MotorcycleItem extends Item {
	public MotorcycleItem(final Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
		if (hit.getType() != HitResult.Type.BLOCK) {
			return InteractionResult.PASS;
		}

		Vec3 view = player.getViewVector(1.0F);
		List<Entity> nearby = level.getEntities(player, player.getBoundingBox().expandTowards(view.scale(5.0)).inflate(1.0), EntitySelector.CAN_BE_PICKED);
		Vec3 eye = player.getEyePosition();
		for (Entity entity : nearby) {
			AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
			if (box.contains(eye)) {
				return InteractionResult.PASS;
			}
		}

		MotorcycleEntity bike = ModEntities.MOTORCYCLE.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		if (bike == null) {
			return InteractionResult.FAIL;
		}

		Vec3 location = ((BlockHitResult) hit).getLocation();
		bike.snapTo(location.x, location.y, location.z, player.getYRot(), 0.0F);
		bike.setYHeadRot(player.getYRot());
		bike.setYBodyRot(player.getYRot());
		if (!level.noCollision(bike, bike.getBoundingBox())) {
			return InteractionResult.FAIL;
		}

		if (level instanceof ServerLevel serverLevel) {
			EntityType.<MotorcycleEntity>createDefaultStackConfig(serverLevel, stack, player).apply(bike);
			if (!bike.loadFromItem(stack)) {
				bike.installStarterEngine();
			}
			// Whoever places the bike owns it, so handing someone the item hands them the bike.
			bike.setOwner(player);
			serverLevel.addFreshEntity(bike);
			serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, location);
			stack.consume(1, player);
		}

		player.awardStat(Stats.ITEM_USED.get(this));
		return InteractionResult.SUCCESS;
	}
}
