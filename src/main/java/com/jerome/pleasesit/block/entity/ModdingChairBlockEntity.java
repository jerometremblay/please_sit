package com.jerome.pleasesit.block.entity;

import com.jerome.pleasesit.block.ModdingChairBlock;
import com.jerome.pleasesit.config.PleaseSitConfig;
import com.jerome.pleasesit.menu.ControllerMenu;
import com.jerome.pleasesit.registry.ModBlockEntities;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import javax.annotation.Nullable;

public class ModdingChairBlockEntity extends BlockEntity implements MenuProvider {
    private static final String TARGET_POS_KEY = "target_pos";
    private static final String LOCKED_VILLAGER_UUID_KEY = "locked_villager_uuid";
    private static final double APPROACH_SPEED = 0.6D;
    private static final double MOUNT_DISTANCE_SQR = 0.75D;
    private static final double STALLED_NEAR_DESTINATION_DISTANCE_SQR = 2.5D * 2.5D;
    private static final double MIN_MOVEMENT_DISTANCE_SQR = 0.02D * 0.02D;
    private static final double SEAT_X_OFFSET = 0.5D;
    private static final double SEAT_Y_OFFSET = -1.85D;
    private static final double SEAT_Z_OFFSET = 0.5D;
    private static final Map<ResourceKey<Level>, Map<UUID, BlockPos>> CLAIMED_VILLAGERS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Map<UUID, BlockPos>> LOCKED_VILLAGERS = new HashMap<>();

    private static final String SEARCH_RADIUS_KEY = "searchRadius";
    private static final int DEFAULT_SEARCH_RADIUS = 16;
    private static final int MIN_SEARCH_RADIUS = 1;
    private static final int MAX_SEARCH_RADIUS = 64;

    private UUID seatEntityUuid;
    private UUID villagerUuid;
    private UUID lockedVillagerUuid;
    private BlockPos targetPos;
    private int searchRadius = DEFAULT_SEARCH_RADIUS;
    private double lastVillagerX;
    private double lastVillagerY;
    private double lastVillagerZ;
    private boolean hasLastVillagerPosition;

    public ModdingChairBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MODDING_CHAIR.get(), pos, blockState);
    }

    public void activate(ServerLevel level) {
        refreshPersistentLock(level);
        if (villagerUuid != null) {
            return;
        }

        Optional<Villager> villager = findNearestVillager(level);
        if (villager.isEmpty()) {
            return;
        }

        Villager selectedVillager = villager.get();
        enableDoorNavigation(selectedVillager);
        claimVillager(level, selectedVillager.getUUID());
        villagerUuid = selectedVillager.getUUID();
        selectedVillager.getNavigation().moveTo(getApproachX(), getApproachY(), getApproachZ(), APPROACH_SPEED);
        rememberVillagerPosition(selectedVillager);
        setChanged();
    }

    public void applyPlacementData(@Nullable BlockPos targetPos, @Nullable UUID lockedVillagerUuid) {
        this.targetPos = targetPos;

        if (level instanceof ServerLevel serverLevel && this.lockedVillagerUuid != null && !this.lockedVillagerUuid.equals(lockedVillagerUuid)) {
            unregisterPersistentLock(serverLevel);
        }

        this.lockedVillagerUuid = lockedVillagerUuid;

        if (level instanceof ServerLevel serverLevel) {
            refreshPersistentLock(serverLevel);
        }

        setChanged();
    }

    public int getSearchRadius() {
        return searchRadius;
    }

    public void setSearchRadius(int searchRadius) {
        this.searchRadius = Math.clamp(searchRadius, MIN_SEARCH_RADIUS, MAX_SEARCH_RADIUS);
        setChanged();
    }

    public void openMenu(net.neoforged.neoforge.common.extensions.IPlayerExtension player) {
        player.openMenu(this, worldPosition);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.pleasesit.modding_chair");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ControllerMenu(containerId, playerInventory, this);
    }

    public void releaseOccupant() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Entity seat = getSeatEntity(serverLevel);
        Entity villager = getVillagerEntity(serverLevel);
        boolean wasSeated = seat != null && villager != null && villager.isPassengerOfSameVehicle(seat);
        if (seat != null) {
            seat.ejectPassengers();
            seat.discard();
        }

        if (villager instanceof Villager villagerEntity) {
            villagerEntity.getNavigation().stop();
            villagerEntity.setInvulnerable(false);
            if (wasSeated) {
                placeVillagerOnFloor(villagerEntity);
            }
        }

        releaseVillagerClaim(serverLevel);
        seatEntityUuid = null;
        villagerUuid = null;
        hasLastVillagerPosition = false;
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ModdingChairBlockEntity chair) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        chair.refreshPersistentLock(serverLevel);
        if (chair.villagerUuid == null) {
            return;
        }

        chair.refreshVillagerClaim(serverLevel);
        Entity villager = chair.getVillagerEntity(serverLevel);
        if (!(villager instanceof Villager villagerEntity) || !villagerEntity.isAlive()) {
            chair.releaseOccupant();
            return;
        }

        Entity seat = chair.getSeatEntity(serverLevel);
        if (seat == null) {
            villagerEntity.getNavigation().moveTo(chair.getApproachX(), chair.getApproachY(), chair.getApproachZ(), APPROACH_SPEED);
            double distanceToApproach = villagerEntity.distanceToSqr(chair.getApproachX(), chair.getApproachY(), chair.getApproachZ());
            if (distanceToApproach <= MOUNT_DISTANCE_SQR
                    || chair.isStalledNearApproach(villagerEntity, distanceToApproach)) {
                chair.mountVillager(serverLevel, villagerEntity);
                return;
            }

            chair.rememberVillagerPosition(villagerEntity);
            return;
        }

        if (!villagerEntity.isPassengerOfSameVehicle(seat)) {
            chair.releaseOccupant();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        seatEntityUuid = input.read("seat_entity_uuid", UUIDUtil.CODEC).orElse(null);
        villagerUuid = input.read("villager_uuid", UUIDUtil.CODEC).orElse(null);
        lockedVillagerUuid = input.read(LOCKED_VILLAGER_UUID_KEY, UUIDUtil.CODEC).orElse(null);
        targetPos = input.getLong(TARGET_POS_KEY).map(BlockPos::of).orElse(null);
        searchRadius = input.read(SEARCH_RADIUS_KEY, com.mojang.serialization.Codec.INT).map(i -> i).orElse(DEFAULT_SEARCH_RADIUS);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (seatEntityUuid != null) {
            output.store("seat_entity_uuid", UUIDUtil.CODEC, seatEntityUuid);
        }
        if (villagerUuid != null) {
            output.store("villager_uuid", UUIDUtil.CODEC, villagerUuid);
        }
        if (lockedVillagerUuid != null) {
            output.store(LOCKED_VILLAGER_UUID_KEY, UUIDUtil.CODEC, lockedVillagerUuid);
        }
        if (targetPos != null) {
            output.putLong(TARGET_POS_KEY, targetPos.asLong());
        }
        output.store(SEARCH_RADIUS_KEY, com.mojang.serialization.Codec.INT, searchRadius);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            refreshPersistentLock(serverLevel);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            unregisterPersistentLock(serverLevel);
            releaseVillagerClaim(serverLevel);
        }
        super.setRemoved();
    }

    private Optional<Villager> findNearestVillager(ServerLevel level) {
        if (lockedVillagerUuid != null) {
            Entity entity = level.getEntity(lockedVillagerUuid);
            if (!(entity instanceof Villager villager)
                    || !villager.isAlive()
                    || villager.isPassenger()
                    || isVillagerLockedByAnotherChair(level, villager.getUUID())
                    || isVillagerClaimedByAnotherChair(level, villager.getUUID())) {
                return Optional.empty();
            }

            return Optional.of(villager);
        }

        AABB searchBox = new AABB(getTargetPos()).inflate(getSearchRadius());
        return level.getEntitiesOfClass(Villager.class, searchBox).stream()
                .filter(Entity::isAlive)
                .filter(villager -> !villager.isPassenger())
                .filter(villager -> !isVillagerLockedByAnotherChair(level, villager.getUUID()))
                .filter(villager -> !isVillagerClaimedByAnotherChair(level, villager.getUUID()))
                .min(Comparator.comparingDouble(villager -> villager.distanceToSqr(getSeatX(), getSeatY(), getSeatZ())));
    }

    private ArmorStand createSeat(ServerLevel level) {
        ArmorStand seat = new ArmorStand(
                level,
                getSeatX(),
                getSeatY(),
                getSeatZ()
        );
        float seatYaw = getSeatYaw();
        seat.setNoGravity(true);
        seat.setInvisible(true);
        seat.setInvulnerable(true);
        seat.setSilent(true);
        seat.setNoBasePlate(true);
        seat.setYRot(seatYaw);
        seat.setYHeadRot(seatYaw);
        seat.setYBodyRot(seatYaw);
        level.addFreshEntity(seat);
        return seat;
    }

    private void mountVillager(ServerLevel level, Villager villager) {
        ArmorStand seat = createSeat(level);
        float seatYaw = getSeatYaw();
        villager.getNavigation().stop();
        villager.setInvulnerable(true);
        villager.setPos(getSeatX(), getSeatY(), getSeatZ());
        villager.setYRot(seatYaw);
        villager.setYBodyRot(seatYaw);
        villager.setYHeadRot(seatYaw);
        if (!villager.startRiding(seat, true)) {
            villager.setInvulnerable(false);
            seat.discard();
            releaseVillagerClaim(level);
            villagerUuid = null;
            seatEntityUuid = null;
            setChanged();
            return;
        }

        seatEntityUuid = seat.getUUID();
        setChanged();
    }

    private Entity getSeatEntity(ServerLevel level) {
        return seatEntityUuid == null ? null : level.getEntity(seatEntityUuid);
    }

    private Entity getVillagerEntity(ServerLevel level) {
        return villagerUuid == null ? null : level.getEntity(villagerUuid);
    }

    private BlockPos getTargetPos() {
        return targetPos != null ? targetPos : worldPosition;
    }

    private void refreshVillagerClaim(ServerLevel level) {
        if (villagerUuid == null) {
            return;
        }

        claimVillager(level, villagerUuid);
    }

    private void claimVillager(ServerLevel level, UUID uuid) {
        CLAIMED_VILLAGERS
                .computeIfAbsent(level.dimension(), ignored -> new HashMap<>())
                .put(uuid, worldPosition);
    }

    private void refreshPersistentLock(ServerLevel level) {
        if (lockedVillagerUuid == null) {
            return;
        }

        LOCKED_VILLAGERS
                .computeIfAbsent(level.dimension(), ignored -> new HashMap<>())
                .put(lockedVillagerUuid, worldPosition);
    }

    private void releaseVillagerClaim(ServerLevel level) {
        if (villagerUuid == null) {
            return;
        }

        Map<UUID, BlockPos> claims = CLAIMED_VILLAGERS.get(level.dimension());
        if (claims == null) {
            return;
        }

        BlockPos claimedBy = claims.get(villagerUuid);
        if (worldPosition.equals(claimedBy)) {
            claims.remove(villagerUuid);
        }

        if (claims.isEmpty()) {
            CLAIMED_VILLAGERS.remove(level.dimension());
        }
    }

    private void unregisterPersistentLock(ServerLevel level) {
        if (lockedVillagerUuid == null) {
            return;
        }

        Map<UUID, BlockPos> locks = LOCKED_VILLAGERS.get(level.dimension());
        if (locks == null) {
            return;
        }

        BlockPos lockedBy = locks.get(lockedVillagerUuid);
        if (worldPosition.equals(lockedBy)) {
            locks.remove(lockedVillagerUuid);
        }

        if (locks.isEmpty()) {
            LOCKED_VILLAGERS.remove(level.dimension());
        }
    }

    private boolean isVillagerClaimedByAnotherChair(ServerLevel level, UUID uuid) {
        Map<UUID, BlockPos> claims = CLAIMED_VILLAGERS.get(level.dimension());
        if (claims == null) {
            return false;
        }

        BlockPos claimedBy = claims.get(uuid);
        return claimedBy != null && !worldPosition.equals(claimedBy);
    }

    private boolean isVillagerLockedByAnotherChair(ServerLevel level, UUID uuid) {
        Map<UUID, BlockPos> locks = LOCKED_VILLAGERS.get(level.dimension());
        if (locks == null) {
            return false;
        }

        BlockPos lockedBy = locks.get(uuid);
        return lockedBy != null && !worldPosition.equals(lockedBy);
    }

    private boolean isStalledNearApproach(Villager villager, double distanceToApproach) {
        if (!hasLastVillagerPosition || distanceToApproach > STALLED_NEAR_DESTINATION_DISTANCE_SQR) {
            return false;
        }

        double movementDistance = villager.distanceToSqr(lastVillagerX, lastVillagerY, lastVillagerZ);
        return movementDistance <= MIN_MOVEMENT_DISTANCE_SQR;
    }

    private void rememberVillagerPosition(Villager villager) {
        lastVillagerX = villager.getX();
        lastVillagerY = villager.getY();
        lastVillagerZ = villager.getZ();
        hasLastVillagerPosition = true;
    }

    private void placeVillagerOnFloor(Villager villager) {
        BlockPos floorPos = getReleasePos();
        villager.setPos(floorPos.getX() + 0.5D, floorPos.getY() + 1.0D, floorPos.getZ() + 0.5D);
    }

    private double getSeatX() {
        return getTargetPos().getX() + SEAT_X_OFFSET;
    }

    private double getApproachX() {
        return getApproachPos().getX() + 0.5D;
    }

    private double getApproachY() {
        return getApproachPos().getY();
    }

    private double getApproachZ() {
        return getApproachPos().getZ() + 0.5D;
    }

    private double getSeatY() {
        return getTargetPos().getY() + SEAT_Y_OFFSET;
    }

    private double getSeatZ() {
        return getTargetPos().getZ() + SEAT_Z_OFFSET;
    }

    private float getSeatYaw() {
        return getBlockState().getValue(ModdingChairBlock.FACING).toYRot();
    }

    private BlockPos getApproachPos() {
        BlockPos targetPos = getTargetPos();
        int dx = worldPosition.getX() - targetPos.getX();
        int dz = worldPosition.getZ() - targetPos.getZ();

        if (Math.abs(dx) >= Math.abs(dz) && dx != 0) {
            return targetPos.relative(dx > 0 ? Direction.EAST : Direction.WEST);
        }

        if (dz != 0) {
            return targetPos.relative(dz > 0 ? Direction.SOUTH : Direction.NORTH);
        }

        return targetPos.relative(Direction.SOUTH);
    }

    private BlockPos getReleasePos() {
        Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (Direction direction : directions) {
            BlockPos candidate = getTargetPos().relative(direction);
            if (isClearStandingSpot(candidate)) {
                return candidate;
            }
        }

        return getApproachPos();
    }

    private boolean isClearStandingSpot(BlockPos pos) {
        if (level == null) {
            return false;
        }

        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState floor = level.getBlockState(pos.below());
        return feet.getCollisionShape(level, pos).isEmpty()
                && head.getCollisionShape(level, pos.above()).isEmpty()
                && floor.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private void enableDoorNavigation(Villager villager) {
        if (villager.getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanOpenDoors(true);
        }
    }
}
