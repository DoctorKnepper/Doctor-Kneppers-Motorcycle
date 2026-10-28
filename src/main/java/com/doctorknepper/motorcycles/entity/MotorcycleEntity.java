package com.doctorknepper.motorcycles.entity;

import com.doctorknepper.motorcycles.DKMotorcycles;
import com.doctorknepper.motorcycles.config.MotorcycleConfig;
import com.doctorknepper.motorcycles.item.EngineItem;
import com.doctorknepper.motorcycles.menu.MotorcycleMenu;
import com.doctorknepper.motorcycles.registry.ModItems;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The motorcycle. Built on the same riding system horses use: the rider's client simulates
 * movement (smooth, auto step-up over full blocks, edge braking), the server handles fuel,
 * damage, seats, ownership and lights.
 */
public class MotorcycleEntity extends LivingEntity implements HasCustomInventoryScreen {
	// ---- inventory layout (indices are saved to disk: only ever append) ----
	public static final int SLOT_ENGINE = 0;
	public static final int SLOT_TANK = 1;
	public static final int SLOT_FUEL = 2;
	public static final int SLOT_FIRST_BAG = 3;
	public static final int BAG_COUNT = 3;
	public static final int SLOT_TRUNK = SLOT_FIRST_BAG + BAG_COUNT;
	public static final int STORAGE_START = SLOT_TRUNK + 1;
	public static final int BAG_SIZE = 9;
	public static final int TRUNK_SIZE = 27;
	public static final int TRUNK_STORAGE_START = STORAGE_START + BAG_COUNT * BAG_SIZE;
	public static final int STORAGE_END = TRUNK_STORAGE_START + TRUNK_SIZE;
	public static final int SLOT_SIDECAR_LEFT = STORAGE_END;
	public static final int SLOT_SIDECAR_RIGHT = STORAGE_END + 1;
	public static final int INVENTORY_SIZE = SLOT_SIDECAR_RIGHT + 1;

	// ---- upgrade bits (synced so the client can render and size the GUI) ----
	public static final int UPGRADE_TANK = 1;
	public static final int UPGRADE_BAG_1 = 1 << 1;
	public static final int UPGRADE_TRUNK = 1 << 4;
	public static final int UPGRADE_SIDECAR_LEFT = 1 << 5;
	public static final int UPGRADE_SIDECAR_RIGHT = 1 << 6;

	// ---- tuning ----
	public static final int BASE_FUEL_CAPACITY = 12_000;
	public static final int LARGE_FUEL_CAPACITY = 36_000;
	private static final float REVERSE_SPEED = 0.06F;
	private static final float PADDLE_SPEED = 0.04F;
	private static final float MAX_TURN_PER_TICK = 14.0F;
	private static final float MIN_TURN_PER_TICK = 5.0F;
	private static final float BOOST_MULTIPLIER = 1.15F;
	private static final double RAM_MIN_SPEED = 0.3;
	private static final double FLOAT_DEPTH = 0.5;
	private static final float WHEEL_RADIUS_BLOCKS = 5.0F / 16.0F;
	private static final int LIGHT_REFRESH_TICKS = 40;
	private static final String ITEM_DATA_KEY = "MotorcycleData";

	/** Client-side: true while the rider holds the key that drives over edges (set by the client mod). */
	public static BooleanSupplier edgeOverrideHeld = () -> false;

	public static final ResourceKey<DamageType> RUN_OVER = ResourceKey.create(Registries.DAMAGE_TYPE, DKMotorcycles.id("run_over"));

	private static final EntityDataAccessor<Integer> DATA_FUEL = SynchedEntityData.defineId(MotorcycleEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ENGINE = SynchedEntityData.defineId(MotorcycleEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_UPGRADES = SynchedEntityData.defineId(MotorcycleEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(MotorcycleEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_BOOST = SynchedEntityData.defineId(MotorcycleEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_HEADLIGHT = SynchedEntityData.defineId(MotorcycleEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Float> DATA_TOP_SPEED = SynchedEntityData.defineId(MotorcycleEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> DATA_EDGE_DEPTH = SynchedEntityData.defineId(MotorcycleEntity.class, EntityDataSerializers.INT);

	/** Who sits where, as entity ids (-1 = empty). Synced so every client places riders identically. */
	public enum Seat {
		DRIVER(0.0, 0.92, -0.1),
		PILLION(0.0, 0.92, -0.62),
		SIDECAR_LEFT(0.84, 0.66, -0.15),
		SIDECAR_RIGHT(-0.84, 0.66, -0.15);

		final double x;
		final double y;
		final double z;
		EntityDataAccessor<Integer> data;

		Seat(final double x, final double y, final double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	static {
		for (Seat seat : Seat.values()) {
			seat.data = SynchedEntityData.defineId(MotorcycleEntity.class, EntityDataSerializers.INT);
		}
	}

	private final MotorcycleContainer inventory = new MotorcycleContainer(this);

	// client-side state
	private float throttle;
	private float wheelRotation;
	private float wheelRotationO;
	private int edgeWarningTicks;

	// server-side state
	private @Nullable Vec3 lastServerPos;
	private @Nullable BlockPos lightPos;
	private int lightPlacedTick;
	private double fuelDebt;
	private int hornTicks;
	private @Nullable UUID ownerId;
	private String ownerName = "";
	private boolean seatMobsInSidecarFirst;

	public MotorcycleEntity(final EntityType<? extends MotorcycleEntity> type, final Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return LivingEntity.createLivingAttributes()
			.add(Attributes.MAX_HEALTH, 40.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.STEP_HEIGHT, 1.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.SAFE_FALL_DISTANCE, 6.0)
			.add(Attributes.FALL_DAMAGE_MULTIPLIER, 0.5);
	}

	@Override
	protected void defineSynchedData(final SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(DATA_FUEL, 0);
		entityData.define(DATA_ENGINE, EngineTier.NONE.ordinal());
		entityData.define(DATA_UPGRADES, 0);
		entityData.define(DATA_COLOR, DyeColor.RED.getId());
		entityData.define(DATA_BOOST, 0);
		entityData.define(DATA_HEADLIGHT, true);
		entityData.define(DATA_TOP_SPEED, 0.0F);
		entityData.define(DATA_EDGE_DEPTH, 3);
		for (Seat seat : Seat.values()) {
			entityData.define(seat.data, -1);
		}
	}

	// =====================================================================
	// Synced accessors
	// =====================================================================

	public SimpleContainer getInventory() {
		return this.inventory;
	}

	public int getFuel() {
		return this.entityData.get(DATA_FUEL);
	}

	private void setFuel(final int fuel) {
		this.entityData.set(DATA_FUEL, Mth.clamp(fuel, 0, this.getFuelCapacity()));
	}

	public int getFuelCapacity() {
		return this.hasUpgrade(UPGRADE_TANK) ? LARGE_FUEL_CAPACITY : BASE_FUEL_CAPACITY;
	}

	public EngineTier getEngine() {
		return EngineTier.byId(this.entityData.get(DATA_ENGINE));
	}

	public int getUpgrades() {
		return this.entityData.get(DATA_UPGRADES);
	}

	public boolean hasUpgrade(final int bit) {
		return (this.getUpgrades() & bit) != 0;
	}

	public static int bagBit(final int bag) {
		return UPGRADE_BAG_1 << bag;
	}

	public int sidecarCount() {
		return (this.hasUpgrade(UPGRADE_SIDECAR_LEFT) ? 1 : 0) + (this.hasUpgrade(UPGRADE_SIDECAR_RIGHT) ? 1 : 0);
	}

	public DyeColor getColor() {
		return DyeColor.byId(this.entityData.get(DATA_COLOR));
	}

	public void setColor(final DyeColor color) {
		this.entityData.set(DATA_COLOR, color.getId());
	}

	public int getBoost() {
		return this.entityData.get(DATA_BOOST);
	}

	public boolean isHeadlightOn() {
		return this.entityData.get(DATA_HEADLIGHT);
	}

	public void toggleHeadlight() {
		this.entityData.set(DATA_HEADLIGHT, !this.isHeadlightOn());
		this.playSound(SoundEvents.LEVER_CLICK, 0.5F, this.isHeadlightOn() ? 1.2F : 0.9F);
	}

	/** Floating in water (not just splashing through a puddle): the engine floods. */
	public boolean isAfloat() {
		double depth = this.getFluidHeight(FluidTags.WATER);
		return depth > FLOAT_DEPTH + 0.1 || depth > 0.15 && !this.onGround();
	}

	public boolean canDrive() {
		return this.getEngine() != EngineTier.NONE && this.getFuel() > 0 && !this.isAfloat();
	}

	/** Ridden movement speed at full throttle (about 44 blocks/second per 1.0). */
	public float getTopSpeed() {
		float speed = this.entityData.get(DATA_TOP_SPEED);
		return this.getBoost() > 0 ? speed * BOOST_MULTIPLIER : speed;
	}

	public float getWheelRotation(final float partialTicks) {
		return Mth.lerp(partialTicks, this.wheelRotationO, this.wheelRotation);
	}

	/** Client-side: the bike just braked at a drop. */
	public boolean isEdgeWarning() {
		return this.edgeWarningTicks > 0;
	}

	/** Re-derives synced state (engine tier, installed upgrades) from the inventory. Server only. */
	void onInventoryChanged() {
		if (this.level().isClientSide()) {
			return;
		}

		ItemStack engine = this.inventory.getItem(SLOT_ENGINE);
		EngineTier tier = engine.getItem() instanceof EngineItem engineItem ? engineItem.tier() : EngineTier.NONE;
		this.entityData.set(DATA_ENGINE, tier.ordinal());

		int upgrades = 0;
		if (this.inventory.getItem(SLOT_TANK).is(ModItems.FUEL_TANK)) {
			upgrades |= UPGRADE_TANK;
		}
		for (int bag = 0; bag < BAG_COUNT; bag++) {
			if (this.inventory.getItem(SLOT_FIRST_BAG + bag).is(ModItems.SADDLEBAG)) {
				upgrades |= bagBit(bag);
			}
		}
		if (this.inventory.getItem(SLOT_TRUNK).is(ModItems.TRUNK)) {
			upgrades |= UPGRADE_TRUNK;
		}
		if (this.inventory.getItem(SLOT_SIDECAR_LEFT).is(ModItems.SIDECAR)) {
			upgrades |= UPGRADE_SIDECAR_LEFT;
		}
		if (this.inventory.getItem(SLOT_SIDECAR_RIGHT).is(ModItems.SIDECAR)) {
			upgrades |= UPGRADE_SIDECAR_RIGHT;
		}
		this.entityData.set(DATA_UPGRADES, upgrades);
		this.setFuel(this.getFuel());
		this.syncConfig();

		// A sidecar taken off (by a command, say) can't keep its passenger.
		this.ejectFromMissingSidecar(Seat.SIDECAR_LEFT, UPGRADE_SIDECAR_LEFT);
		this.ejectFromMissingSidecar(Seat.SIDECAR_RIGHT, UPGRADE_SIDECAR_RIGHT);
	}

	private void ejectFromMissingSidecar(final Seat seat, final int bit) {
		Entity rider = this.getSeated(seat);
		if (rider != null && !this.hasUpgrade(bit)) {
			rider.stopRiding();
		}
	}

	/** Pushes server settings the rider's client needs into synced data. */
	private void syncConfig() {
		MotorcycleConfig config = MotorcycleConfig.get();
		float speed = (float) (config.blocksPerSecond(this.getEngine()) / EngineTier.BLOCKS_PER_SECOND_PER_SPEED);
		if (this.entityData.get(DATA_TOP_SPEED) != speed) {
			this.entityData.set(DATA_TOP_SPEED, speed);
		}
		int depth = Math.max(0, config.edgeBrakingDepth);
		if (this.entityData.get(DATA_EDGE_DEPTH) != depth) {
			this.entityData.set(DATA_EDGE_DEPTH, depth);
		}
	}

	@Override
	public void onSyncedDataUpdated(final EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (DATA_UPGRADES.equals(key)) {
			this.refreshDimensions();
		}
	}

	/** Sidecars make the bike wider, so it no longer squeezes through gaps it visibly can't fit. */
	@Override
	protected EntityDimensions getDefaultDimensions(final Pose pose) {
		EntityDimensions base = super.getDefaultDimensions(pose);
		int sidecars = this.sidecarCount();
		if (sidecars == 0) {
			return base;
		}
		float width = sidecars == 1 ? 2.0F : 2.75F;
		return base.scale(width / base.width(), 1.0F);
	}

	// =====================================================================
	// Ownership
	// =====================================================================

	public void setOwner(final Player player) {
		this.ownerId = player.getUUID();
		this.ownerName = player.getGameProfile().name();
	}

	/** Owners, creative-mode players, everyone on unowned bikes, or everyone when the lock is off. */
	public boolean isAllowed(final Player player) {
		return this.ownerId == null
			|| !MotorcycleConfig.get().ownerLock
			|| this.ownerId.equals(player.getUUID())
			|| player.getAbilities().instabuild;
	}

	private void denyNotOwner(final Player player) {
		player.sendOverlayMessage(Component.translatable("message.dkmotorcycles.not_yours", this.ownerName));
	}

	// =====================================================================
	// Seats
	// =====================================================================

	public @Nullable Entity getSeated(final Seat seat) {
		int id = this.entityData.get(seat.data);
		if (id < 0) {
			return null;
		}
		Entity entity = this.level().getEntity(id);
		return entity != null && this.hasPassenger(entity) ? entity : null;
	}

	public @Nullable Seat seatOf(final Entity passenger) {
		for (Seat seat : Seat.values()) {
			if (this.entityData.get(seat.data) == passenger.getId()) {
				return seat;
			}
		}
		return null;
	}

	private boolean seatExists(final Seat seat) {
		return switch (seat) {
			case DRIVER, PILLION -> true;
			case SIDECAR_LEFT -> this.hasUpgrade(UPGRADE_SIDECAR_LEFT);
			case SIDECAR_RIGHT -> this.hasUpgrade(UPGRADE_SIDECAR_RIGHT);
		};
	}

	public boolean isSeatFree(final Seat seat) {
		return this.getSeated(seat) == null;
	}

	/** Which seat this entity would take. Players drive if allowed; mobs prefer the sidecars. */
	private @Nullable Seat findSeat(final Entity entity) {
		if (entity instanceof Player player) {
			boolean mayDrive = this.level().isClientSide() || this.isAllowed(player);
			if (mayDrive && this.isSeatFree(Seat.DRIVER)) {
				return Seat.DRIVER;
			}
			for (Seat seat : new Seat[] {Seat.PILLION, Seat.SIDECAR_LEFT, Seat.SIDECAR_RIGHT}) {
				if (this.seatExists(seat) && this.isSeatFree(seat)) {
					return seat;
				}
			}
			return null;
		}
		Seat[] order = this.seatMobsInSidecarFirst
			? new Seat[] {Seat.SIDECAR_LEFT, Seat.SIDECAR_RIGHT, Seat.PILLION}
			: new Seat[] {Seat.PILLION, Seat.SIDECAR_LEFT, Seat.SIDECAR_RIGHT};
		for (Seat seat : order) {
			if (this.seatExists(seat) && this.isSeatFree(seat)) {
				return seat;
			}
		}
		return null;
	}

	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		return this.getSeated(Seat.DRIVER) instanceof Player player ? player : null;
	}

	@Override
	protected boolean canAddPassenger(final Entity passenger) {
		return this.getPassengers().size() < 4 && this.findSeat(passenger) != null;
	}

	@Override
	protected void addPassenger(final Entity passenger) {
		Seat seat = this.findSeat(passenger);
		super.addPassenger(passenger);
		if (!this.level().isClientSide() && seat != null) {
			this.entityData.set(seat.data, passenger.getId());
		}
		if (seat == Seat.DRIVER) {
			passenger.absSnapRotationTo(this.getYRot(), passenger.getXRot());
		}
	}

	@Override
	protected void removePassenger(final Entity passenger) {
		super.removePassenger(passenger);
		if (!this.level().isClientSide()) {
			for (Seat seat : Seat.values()) {
				if (this.entityData.get(seat.data) == passenger.getId()) {
					this.entityData.set(seat.data, -1);
				}
			}
		}
		if (this.getControllingPassenger() == null) {
			this.throttle = 0.0F;
		}
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(final Entity passenger, final EntityDimensions dimensions, final float scale) {
		Seat seat = this.seatOf(passenger);
		if (seat == null) {
			seat = passenger == this.getFirstPassenger() ? Seat.DRIVER : Seat.PILLION;
		}
		return new Vec3(seat.x * scale, seat.y * scale, seat.z * scale).yRot(-this.getYRot() * Mth.DEG_TO_RAD);
	}

	@Override
	protected void positionRider(final Entity passenger, final Entity.MoveFunction moveFunction) {
		super.positionRider(passenger, moveFunction);
		if (passenger instanceof LivingEntity living) {
			living.yBodyRot = this.getYRot();
		}
	}

	// =====================================================================
	// Ticking
	// =====================================================================

	@Override
	public void tick() {
		super.tick();
		if (this.getFluidHeight(FluidTags.WATER) > FLOAT_DEPTH) {
			// The bike bobs up to the surface instead of sinking.
			Vec3 motion = this.getDeltaMovement();
			this.setDeltaMovement(motion.x, Math.min(Math.max(motion.y, -0.02) + 0.06, 0.12), motion.z);
		}
		if (this.level() instanceof ServerLevel serverLevel) {
			this.serverTick(serverLevel);
		} else {
			this.clientTick();
		}
	}

	private void clientTick() {
		this.wheelRotationO = this.wheelRotation;
		if (this.edgeWarningTicks > 0) {
			this.edgeWarningTicks--;
		}
		double dx = this.getX() - this.xo;
		double dz = this.getZ() - this.zo;
		double distance = Math.sqrt(dx * dx + dz * dz);
		if (distance > 1.0E-4) {
			Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot());
			double sign = Math.signum(dx * forward.x + dz * forward.z);
			this.wheelRotation += (float) (sign * distance / WHEEL_RADIUS_BLOCKS);
		}

		if (this.getControllingPassenger() != null && this.canDrive() && this.random.nextInt(distance > 0.2 ? 2 : 6) == 0) {
			Vec3 exhaust = new Vec3(-0.25, 0.25, -0.85).yRot(-this.getYRot() * Mth.DEG_TO_RAD);
			this.level().addParticle(
				this.getBoost() > 0 ? ParticleTypes.FLAME : ParticleTypes.SMOKE,
				this.getX() + exhaust.x, this.getY() + exhaust.y, this.getZ() + exhaust.z,
				0.0, 0.02, 0.0
			);
		}
	}

	private void serverTick(final ServerLevel level) {
		if (this.tickCount % 20 == 0) {
			this.syncConfig();
		}
		this.refuelFromSlot(level);

		LivingEntity controller = this.getControllingPassenger();
		boolean throttling = controller instanceof ServerPlayer player
			&& (player.getLastClientInput().forward() || player.getLastClientInput().backward());
		if (throttling && this.canDrive()) {
			this.fuelDebt += Math.max(0.0, MotorcycleConfig.get().fuelUseMultiplier);
			int burn = (int) this.fuelDebt;
			if (burn > 0) {
				this.fuelDebt -= burn;
				this.setFuel(this.getFuel() - burn);
			}
			if (this.getBoost() > 0) {
				this.entityData.set(DATA_BOOST, this.getBoost() - 1);
			}
		}

		for (Entity passenger : this.getPassengers()) {
			// Riders never build up fall distance: if the bike drops, the bike takes the hit.
			passenger.resetFallDistance();
			// Clipping a lead onto a sidecar passenger leads it out.
			if (!(passenger instanceof Player) && passenger instanceof Leashable leashable && leashable.isLeashed()) {
				passenger.stopRiding();
			}
		}

		Vec3 pos = this.position();
		double speed = this.lastServerPos == null ? 0.0 : pos.subtract(this.lastServerPos).horizontalDistance();
		this.lastServerPos = pos;
		if (controller != null && speed > RAM_MIN_SPEED && speed < 4.0) {
			this.ramEntities(level, speed, controller);
		}

		if (this.hornTicks > 0) {
			this.hornTicks--;
			if (this.hornTicks == 5) {
				this.playSound(SoundEvents.NOTE_BLOCK_BIT.value(), 2.0F, 0.75F);
			}
		}

		this.updateHeadlight(level);
	}

	/** Beep-beep. Heard by everyone nearby; can't be spammed. */
	public void honk() {
		if (this.hornTicks > 0) {
			return;
		}
		this.hornTicks = 9;
		this.playSound(SoundEvents.NOTE_BLOCK_BIT.value(), 2.0F, 0.75F);
	}

	private void refuelFromSlot(final ServerLevel level) {
		ItemStack fuelStack = this.inventory.getItem(SLOT_FUEL);
		if (fuelStack.isEmpty() || !FuelHelper.isFuel(fuelStack)) {
			return;
		}

		int gain = Math.round(FuelHelper.furnaceBurnTime(level, fuelStack) * FuelHelper.BASE_MULTIPLIER * FuelHelper.bonusMultiplier(fuelStack));
		if (gain <= 0) {
			return;
		}

		int fuel = this.getFuel();
		int capacity = this.getFuelCapacity();
		// Burn the next item once it fits, or when running low (excess from huge fuels is lost).
		if (fuel + gain > capacity && fuel > capacity / 4) {
			return;
		}

		ItemStack burned = fuelStack.copyWithCount(1);
		ItemStack remainder = FuelHelper.remainder(burned);
		fuelStack.shrink(1);
		if (fuelStack.isEmpty()) {
			this.inventory.setItem(SLOT_FUEL, remainder);
		} else if (!remainder.isEmpty()) {
			this.spawnAtLocation(level, remainder);
		}
		this.inventory.setChanged();

		this.setFuel(fuel + gain);
		if (FuelHelper.givesBoost(burned)) {
			this.entityData.set(DATA_BOOST, Math.min(this.getBoost() + gain, capacity));
		}
	}

	/** Pets, villagers, golems, armor stands, named or leashed mobs, and (by default) players are never hurt. */
	private static boolean canRam(final LivingEntity entity) {
		if (entity instanceof Player) {
			return MotorcycleConfig.get().rammingHurtsPlayers;
		}
		if (entity instanceof OwnableEntity ownable && ownable.getOwnerReference() != null) {
			return false;
		}
		if (entity instanceof AbstractHorse horse && horse.isTamed()) {
			return false;
		}
		if (entity instanceof AbstractVillager || entity instanceof IronGolem || entity instanceof SnowGolem || entity instanceof ArmorStand) {
			return false;
		}
		if (entity.hasCustomName()) {
			return false;
		}
		return !(entity instanceof Leashable leashable && leashable.isLeashed());
	}

	private void ramEntities(final ServerLevel level, final double speed, final LivingEntity controller) {
		double multiplier = MotorcycleConfig.get().rammingDamageMultiplier;
		if (multiplier <= 0.0) {
			return;
		}
		Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot());
		AABB box = this.getBoundingBox().inflate(0.2, 0.0, 0.2).expandTowards(forward.scale(0.6));
		List<LivingEntity> victims = level.getEntitiesOfClass(
			LivingEntity.class,
			box,
			e -> e != this && e.isAlive() && !this.hasPassenger(e) && !(e instanceof MotorcycleEntity) && !e.isSpectator() && canRam(e)
		);
		if (victims.isEmpty()) {
			return;
		}

		DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(RUN_OVER), this, controller);
		float damage = (float) (speed * 9.0 * multiplier);
		for (LivingEntity victim : victims) {
			if (victim.getInvulnerableTime() > 0) {
				continue;
			}
			if (victim.hurtServer(level, source, damage)) {
				Vec3 away = victim.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
				away = away.lengthSqr() < 1.0E-4 ? forward : away.normalize();
				victim.push(away.x * speed * 1.2, 0.25 + speed * 0.2, away.z * speed * 1.2);
				this.hurtServer(level, this.damageSources().flyIntoWall(), damage * 0.15F);
				this.playSound(SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.6F, 1.2F);
			}
		}
	}

	// =====================================================================
	// Headlight: a real (invisible) light block placed ahead of the bike
	// =====================================================================

	private void updateHeadlight(final ServerLevel level) {
		BlockPos target = null;
		if (this.isHeadlightOn() && this.getControllingPassenger() != null && this.getEngine() != EngineTier.NONE) {
			Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot());
			BlockPos ahead = BlockPos.containing(this.position().add(forward.scale(2.5)).add(0.0, 1.0, 0.0));
			if (this.canHoldLight(level, ahead)) {
				target = ahead;
			} else if (this.canHoldLight(level, ahead.above())) {
				target = ahead.above();
			} else if (this.canHoldLight(level, this.blockPosition().above())) {
				target = this.blockPosition().above();
			}
		}

		if (Objects.equals(target, this.lightPos)) {
			if (this.lightPos != null) {
				if (!level.getBlockState(this.lightPos).is(Blocks.LIGHT)) {
					// Put the light back if something removed it (a player, a piston, another mod...).
					level.setBlock(this.lightPos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 3);
				} else if (this.tickCount - this.lightPlacedTick == LIGHT_REFRESH_TICKS) {
					// A light placed while the rider's chunks are still loading (login, teleport, respawn) never
					// reaches the client. Clearing it for one tick makes the line above re-place it once the
					// chunks have arrived, so a parked bike's headlight still lights the ground.
					level.setBlock(this.lightPos, Blocks.AIR.defaultBlockState(), 3);
				}
			}
			return;
		}

		this.removeLight(level);
		if (target != null) {
			level.setBlock(target, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 3);
			this.lightPos = target;
			this.lightPlacedTick = this.tickCount;
		}
	}

	private boolean canHoldLight(final ServerLevel level, final BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.isAir() || pos.equals(this.lightPos) && state.is(Blocks.LIGHT);
	}

	private void removeLight(final Level level) {
		if (this.lightPos != null) {
			if (level.isLoaded(this.lightPos) && level.getBlockState(this.lightPos).is(Blocks.LIGHT)) {
				level.setBlock(this.lightPos, Blocks.AIR.defaultBlockState(), 3);
			}
			this.lightPos = null;
		}
	}

	@Override
	public void remove(final Entity.RemovalReason reason) {
		if (!this.level().isClientSide() && reason.shouldDestroy()) {
			this.removeLight(this.level());
		}
		super.remove(reason);
	}

	// =====================================================================
	// Riding
	// =====================================================================

	@Override
	protected void tickRidden(final Player controller, final Vec3 riddenInput) {
		super.tickRidden(controller, riddenInput);

		// Steering gets gentler as speed rises, so high speed feels stable rather than twitchy.
		float speed = (float) this.getDeltaMovement().horizontalDistance();
		float maxTurn = Mth.clamp(MAX_TURN_PER_TICK - (speed - 0.2F) * 13.0F, MIN_TURN_PER_TICK, MAX_TURN_PER_TICK);
		float diff = Mth.wrapDegrees(controller.getYRot() - this.getYRot());
		float yaw = this.getYRot() + Mth.clamp(diff, -maxTurn, maxTurn);
		this.setRot(yaw, 0.0F);
		this.yBodyRot = this.yHeadRot = yaw;

		this.updateThrottle(controller.zza);
		this.applyEdgeBraking();
	}

	/** Arcade throttle: ramps up while holding forward, brakes on back, coasts otherwise. Paddles slowly when afloat. */
	private void updateThrottle(final float forwardInput) {
		boolean afloat = this.isAfloat();
		float max = afloat ? PADDLE_SPEED : this.canDrive() ? this.getTopSpeed() : 0.0F;
		float reverseMax = afloat ? PADDLE_SPEED : this.canDrive() ? REVERSE_SPEED : 0.0F;
		if (forwardInput > 0.0F) {
			if (this.throttle < 0.0F) {
				this.throttle = Math.min(0.0F, this.throttle + 0.04F);
			} else if (this.throttle < max) {
				this.throttle = Math.min(max, this.throttle + max * 0.035F + 0.002F);
			} else {
				this.throttle = Math.max(max, this.throttle * 0.97F);
			}
		} else if (forwardInput < 0.0F) {
			if (this.throttle > 0.0F) {
				this.throttle = Math.max(0.0F, this.throttle - 0.05F);
			} else {
				this.throttle = Math.max(-reverseMax, this.throttle - 0.01F);
			}
		} else {
			this.throttle *= 0.97F;
		}

		if (Math.abs(this.throttle) < 0.004F) {
			this.throttle = 0.0F;
		}
	}

	/** Brakes instead of rolling off a drop deeper than the configured depth, unless the rider holds Sprint. */
	private void applyEdgeBraking() {
		int depth = this.entityData.get(DATA_EDGE_DEPTH);
		if (depth <= 0 || this.throttle <= 0.0F || !this.onGround() || this.isAfloat() || edgeOverrideHeld.getAsBoolean()) {
			return;
		}
		Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot());
		double speed = this.getDeltaMovement().horizontalDistance();
		double lookahead = 1.2 + speed * 3.0;
		for (double distance = 0.9; distance <= lookahead; distance += 0.5) {
			if (this.isDropAt(this.position().add(forward.scale(distance)), depth)) {
				this.throttle = 0.0F;
				Vec3 motion = this.getDeltaMovement();
				this.setDeltaMovement(motion.x * 0.4, motion.y, motion.z * 0.4);
				this.edgeWarningTicks = 30;
				return;
			}
		}
	}

	private boolean isDropAt(final Vec3 point, final int depth) {
		Level level = this.level();
		int feetY = Mth.floor(this.getY() + 0.01);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(point.x), feetY, Mth.floor(point.z));
		// A step or wall ahead is not a drop (the bike climbs or stops on its own).
		for (int dy = 1; dy >= 0; dy--) {
			pos.setY(feetY + dy);
			if (isSupport(level, pos)) {
				return false;
			}
		}
		for (int dy = 1; dy <= depth; dy++) {
			pos.setY(feetY - dy);
			if (!level.isLoaded(pos) || isSupport(level, pos)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isSupport(final Level level, final BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return !state.getCollisionShape(level, pos).isEmpty() || !state.getFluidState().isEmpty();
	}

	@Override
	protected Vec3 getRiddenInput(final Player controller, final Vec3 selfInput) {
		if (this.throttle == 0.0F) {
			return Vec3.ZERO;
		}
		return new Vec3(0.0, 0.0, this.throttle > 0.0F ? 1.0 : -1.0);
	}

	@Override
	protected float getRiddenSpeed(final Player controller) {
		return Math.abs(this.throttle);
	}

	// =====================================================================
	// Interaction
	// =====================================================================

	@Override
	public InteractionResult interact(final Player player, final InteractionHand hand, final Vec3 location) {
		ItemStack stack = player.getItemInHand(hand);

		if (player.isSecondaryUseActive()) {
			if (!this.level().isClientSide()) {
				if (this.isAllowed(player)) {
					this.openCustomInventoryScreen(player);
				} else {
					this.denyNotOwner(player);
				}
			}
			return InteractionResult.SUCCESS;
		}

		if (this.loadLeashedMobs(player)) {
			return InteractionResult.SUCCESS;
		}

		if (stack.getItem() instanceof DyeItem) {
			DyeColor dye = stack.get(DataComponents.DYE);
			if (dye != null && dye != this.getColor()) {
				if (!this.level().isClientSide()) {
					if (!this.isAllowed(player)) {
						this.denyNotOwner(player);
						return InteractionResult.SUCCESS;
					}
					this.setColor(dye);
					stack.consume(1, player);
					this.playSound(SoundEvents.DYE_USE, 1.0F, 1.0F);
				}
				return InteractionResult.SUCCESS;
			}
		}

		if (stack.is(Items.IRON_INGOT) && this.getHealth() < this.getMaxHealth()) {
			if (!this.level().isClientSide()) {
				this.heal(10.0F);
				stack.consume(1, player);
				this.playSound(SoundEvents.ANVIL_USE, 0.6F, 1.4F);
			}
			return InteractionResult.SUCCESS;
		}

		if (!player.isPassenger()) {
			if (!this.level().isClientSide()) {
				if (this.canAddPassenger(player)) {
					player.startRiding(this);
				} else if (!this.isAllowed(player)) {
					this.denyNotOwner(player);
				}
			}
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	/** Right-click while leading friendly mobs: they climb into free sidecar (then pillion) seats. */
	private boolean loadLeashedMobs(final Player player) {
		List<Leashable> leashed = Leashable.leashableInArea(this, l -> l.getLeashHolder() == player);
		boolean any = false;
		for (Leashable leashable : leashed) {
			if (!(leashable instanceof LivingEntity mob) || mob instanceof Enemy || mob.isPassenger()) {
				continue;
			}
			this.seatMobsInSidecarFirst = true;
			boolean fits = this.canAddPassenger(mob);
			if (fits && !this.level().isClientSide()) {
				leashable.removeLeash();
				if (!player.getAbilities().instabuild) {
					ItemStack lead = new ItemStack(Items.LEAD);
					if (!player.getInventory().add(lead)) {
						player.spawnAtLocation((ServerLevel) this.level(), lead);
					}
				}
				mob.startRiding(this, true, true);
			}
			this.seatMobsInSidecarFirst = false;
			any |= fits;
		}
		return any;
	}

	@Override
	public void openCustomInventoryScreen(final Player player) {
		if (this.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		if (!this.isAllowed(player)) {
			this.denyNotOwner(player);
			return;
		}

		MotorcycleEntity bike = this;
		serverPlayer.openMenu(new ExtendedMenuProvider<Integer>() {
			@Override
			public Integer getScreenOpeningData(final ServerPlayer player) {
				return bike.getId();
			}

			@Override
			public Component getDisplayName() {
				return bike.getDisplayName();
			}

			@Override
			public AbstractContainerMenu createMenu(final int containerId, final Inventory inventory, final Player player) {
				return new MotorcycleMenu(containerId, inventory, bike);
			}
		});
	}

	// =====================================================================
	// Damage, breaking and picking up
	// =====================================================================

	@Override
	public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
		if (this.isRemoved() || this.isInvulnerableTo(level, source)) {
			return false;
		}

		if (source.getEntity() instanceof Player player && source.getDirectEntity() == player) {
			if (!player.getAbilities().mayBuild) {
				return false;
			}
			if (!this.isAllowed(player)) {
				this.denyNotOwner(player);
				return false;
			}
			boolean creative = player.getAbilities().instabuild;
			if (player.isSecondaryUseActive() || creative) {
				if (this.isVehicle() && !creative) {
					player.sendOverlayMessage(Component.translatable("message.dkmotorcycles.riders_aboard"));
					return false;
				}
				this.ejectPassengers();
				this.pickUp(level, player);
				return true;
			}
		}

		return super.hurtServer(level, source, damage);
	}

	/** Vanilla hands a vehicle's fall damage on to its riders. The bike takes the hit for them instead. */
	@Override
	protected void propagateFallToPassengers(final double fallDistance, final float damageModifier, final DamageSource damageSource) {
	}

	@Override
	public boolean skipAttackInteraction(final Entity source) {
		return this.hasPassenger(source) || super.skipAttackInteraction(source);
	}

	@Override
	public void die(final DamageSource source) {
		if (this.level() instanceof ServerLevel level && !this.isRemoved()) {
			this.breakApart(level, true);
		}
	}

	/** Destroyed: everything inside spills out, plus some scrap iron. */
	private void breakApart(final ServerLevel level, final boolean dropScrap) {
		this.ejectPassengers();
		if (level.getGameRules().get(GameRules.ENTITY_DROPS)) {
			for (ItemStack stack : this.inventory.removeAllItems()) {
				this.spawnAtLocation(level, stack);
			}
			if (dropScrap) {
				this.spawnAtLocation(level, new ItemStack(Items.IRON_INGOT, 3));
			}
		}
		this.playSound(SoundEvents.ANVIL_DESTROY, 0.7F, 1.1F);
		this.discard();
	}

	/** Sneak + punch (or any punch in creative): packs the whole bike (gear, fuel, paint) into one item. */
	private void pickUp(final ServerLevel level, final Player player) {
		ItemStack stack = new ItemStack(ModItems.MOTORCYCLE);
		this.saveToItem(stack);
		this.removeLight(level);
		this.playSound(SoundEvents.ITEM_PICKUP, 0.8F, 0.8F);
		this.discard();
		if (!player.getInventory().add(stack)) {
			this.spawnAtLocation(level, stack);
		}
	}

	public void saveToItem(final ItemStack stack) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, this.registryAccess());
		this.writeBikeData(output);
		output.putFloat("Health", this.getHealth());
		CompoundTag root = new CompoundTag();
		root.put(ITEM_DATA_KEY, output.buildResult());
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
		if (this.hasCustomName()) {
			stack.set(DataComponents.CUSTOM_NAME, this.getCustomName());
		}
	}

	/** @return true if the stack carried saved bike data. */
	public boolean loadFromItem(final ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return false;
		}
		CompoundTag root = data.copyTag();
		CompoundTag bikeTag = root.getCompoundOrEmpty(ITEM_DATA_KEY);
		if (bikeTag.isEmpty()) {
			return false;
		}
		ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, this.registryAccess(), bikeTag);
		this.readBikeData(input);
		this.setHealth(input.getFloatOr("Health", this.getMaxHealth()));
		return true;
	}

	/** A freshly crafted bike comes with a copper engine. */
	public void installStarterEngine() {
		this.inventory.setItem(SLOT_ENGINE, new ItemStack(ModItems.COPPER_ENGINE));
	}

	// =====================================================================
	// Saving
	// =====================================================================

	private void writeBikeData(final ValueOutput output) {
		output.putInt("Fuel", this.getFuel());
		output.putInt("Boost", this.getBoost());
		output.putInt("Color", this.getColor().getId());
		output.putBoolean("Headlight", this.isHeadlightOn());
		ContainerHelper.saveAllItems(output, this.inventory.getItems());
	}

	private void readBikeData(final ValueInput input) {
		this.inventory.clearContent();
		ContainerHelper.loadAllItems(input, this.inventory.getItems());
		this.onInventoryChanged();
		this.setColor(DyeColor.byId(input.getIntOr("Color", DyeColor.RED.getId())));
		this.entityData.set(DATA_HEADLIGHT, input.getBooleanOr("Headlight", true));
		this.entityData.set(DATA_BOOST, input.getIntOr("Boost", 0));
		this.setFuel(input.getIntOr("Fuel", 0));
	}

	@Override
	protected void addAdditionalSaveData(final ValueOutput output) {
		super.addAdditionalSaveData(output);
		this.writeBikeData(output);
		if (this.lightPos != null) {
			output.putLong("LightPos", this.lightPos.asLong());
		}
		if (this.ownerId != null) {
			output.putString("Owner", this.ownerId.toString());
			output.putString("OwnerName", this.ownerName);
		}
	}

	@Override
	protected void readAdditionalSaveData(final ValueInput input) {
		super.readAdditionalSaveData(input);
		this.readBikeData(input);
		long light = input.getLongOr("LightPos", Long.MIN_VALUE);
		this.lightPos = light == Long.MIN_VALUE ? null : BlockPos.of(light);
		String owner = input.getStringOr("Owner", "");
		try {
			this.ownerId = owner.isEmpty() ? null : UUID.fromString(owner);
		} catch (IllegalArgumentException e) {
			this.ownerId = null;
		}
		this.ownerName = input.getStringOr("OwnerName", "");
	}

	// =====================================================================
	// LivingEntity housekeeping: a machine, not a mob
	// =====================================================================

	@Override
	public HumanoidArm getMainArm() {
		return HumanoidArm.RIGHT;
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public boolean isAffectedByPotions() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected @Nullable SoundEvent getHurtSound(final DamageSource source) {
		return SoundEvents.ZOMBIE_ATTACK_IRON_DOOR;
	}

	@Override
	protected float getSoundVolume() {
		return 0.5F;
	}

	@Override
	public ItemStack getPickResult() {
		return new ItemStack(ModItems.MOTORCYCLE);
	}
}
