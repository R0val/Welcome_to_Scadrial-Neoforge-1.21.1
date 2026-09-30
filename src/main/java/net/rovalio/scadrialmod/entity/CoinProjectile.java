package net.rovalio.scadrialmod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.rovalio.scadrialmod.equipment.AllomanticEquipment;
import net.rovalio.scadrialmod.item.ScadrialItems;
import net.rovalio.scadrialmod.power.allomancy.physical.external.AllomanticProjectiles;
import net.rovalio.scadrialmod.sound.ScadrialSounds;

public final class CoinProjectile extends AbstractArrow
        implements ItemSupplier, IEntityWithComplexSpawn {

    private static final EntityDataAccessor<ItemStack> COIN =
            SynchedEntityData.defineId(
                    CoinProjectile.class,
                    EntityDataSerializers.ITEM_STACK
            );

    private boolean dealtDamage;
    private boolean wasEmbedded;

    public CoinProjectile(
            EntityType<? extends CoinProjectile> type,
            Level level
    ) {
        super(type, level);
    }

    public CoinProjectile(
            Level level,
            LivingEntity owner,
            ItemStack coin
    ) {
        super(
                AllomanticEquipment.COIN.get(),
                owner,
                level,
                coin.copyWithCount(1),
                null
        );

        entityData.set(COIN, coin.copyWithCount(1));
        pickup = Pickup.ALLOWED;

        setBaseDamage(
                coin.is(ScadrialItems.GOLD_IMPERIAL.get())
                        ? 3.0
                        : 2.0
        );
    }

    @Override
    protected void defineSynchedData(
            SynchedEntityData.Builder builder
    ) {
        super.defineSynchedData(builder);
        builder.define(COIN, ItemStack.EMPTY);
    }

    @Override
    public ItemStack getItem() {
        ItemStack stack = entityData.get(COIN);

        return stack.isEmpty()
                ? getDefaultPickupItem()
                : stack;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ScadrialItems.COPPER_IMPERIAL.get());
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return !dealtDamage && super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        Entity owner = getOwner();

        float damage = (float) Math.min(
                64.0,
                getDeltaMovement().length() * getBaseDamage()
        );

        hit.getEntity().hurt(
                damageSources().arrow(
                        this,
                        owner == null ? this : owner
                ),
                damage
        );

        dealtDamage = true;
        setDeltaMovement(getDeltaMovement().scale(-0.1));
        hurtMarked = true;

        playCollisionSound(hit.getLocation());
    }

    public void rearm() {
        dealtDamage = false;
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide()
                && (tickCount == 1 || inGround != wasEmbedded)) {
            AllomanticProjectiles.syncLater(this);
        }

        wasEmbedded = inGround;
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        // Client velocity uses our full-precision motion payload.
        if (!level().isClientSide()) {
            super.lerpMotion(x, y, z);
        }
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeDouble(getDeltaMovement().x);
        buffer.writeDouble(getDeltaMovement().y);
        buffer.writeDouble(getDeltaMovement().z);
        buffer.writeBoolean(inGround);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        setDeltaMovement(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble()
        );

        inGround = buffer.readBoolean();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("CoinDealtDamage", dealtDamage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        entityData.set(
                COIN,
                getPickupItemStackOrigin().copy()
        );

        dealtDamage = tag.getBoolean("CoinDealtDamage");
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return ScadrialSounds.COIN_COLLISION.get();
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        boolean silent = isSilent();

        setSilent(true);

        try {
            super.onHitBlock(hit);
        } finally {
            setSilent(silent);
        }

        playCollisionSound(hit.getLocation());
    }

    private void playCollisionSound(Vec3 position) {
        if (level().isClientSide() || isSilent()) {
            return;
        }

        level().playSound(
                null,
                position.x,
                position.y,
                position.z,
                ScadrialSounds.COIN_COLLISION.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
    }
}