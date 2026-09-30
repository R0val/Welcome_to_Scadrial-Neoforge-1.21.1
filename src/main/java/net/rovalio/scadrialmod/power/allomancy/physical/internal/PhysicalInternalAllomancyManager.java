package net.rovalio.scadrialmod.power.allomancy.physical.internal;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.network.PhysicalInternalAllomancyNetworking;
import net.rovalio.scadrialmod.player.ScadrialAttachments;
import net.rovalio.scadrialmod.player.ScadrialPlayerData;
import net.rovalio.scadrialmod.power.ScadrialPowerManager;
import net.rovalio.scadrialmod.power.allomancy.AllomancyBurnManager;
import net.rovalio.scadrialmod.power.allomancy.AllomanticFuel;

@EventBusSubscriber(modid = ScadrialMod.MOD_ID)
public final class PhysicalInternalAllomancyManager {

    public static final ResourceKey<DamageType> PEWTER_DEBT =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            ScadrialMod.MOD_ID,
                            "pewter_debt"
                    )
            );

    private static final ResourceLocation PEWTER_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(
                    ScadrialMod.MOD_ID,
                    "pewter"
            );

    private PhysicalInternalAllomancyManager() {
    }

    public static double strength(
            ServerPlayer player,
            AllomanticFuel fuel
    ) {
        ScadrialPlayerData data = ScadrialAttachments.get(player);

        if (!player.isAlive()
                || player.isSpectator()
                || !usable(player, data, fuel)
                || usable(player, data, AllomanticFuel.ALUMINIUM)) {
            return 0.0;
        }

        return AllomancyBurnManager.effectiveStrength(player);
    }

    private static boolean usable(
            ServerPlayer player,
            ScadrialPlayerData data,
            AllomanticFuel fuel
    ) {
        return data.isBurning(fuel)
                && data.getAllomanticReserveSubunits(fuel) > 0
                && ScadrialPowerManager.canUseAllomanticFuel(player, fuel);
    }

    public static double pushResistance(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return 0.0;
        }

        return PhysicalInternalAllomancyMath.pushResistance(
                strength(player, AllomanticFuel.PEWTER)
        );
    }

    public static void refresh(ServerPlayer player) {
        double strength = strength(player, AllomanticFuel.PEWTER);

        updateAttributes(player, strength);

        if (strength == 0.0) {
            settleDebt(player);
        }
    }

    public static void tick(ServerPlayer player) {
        refresh(player);

        ScadrialPlayerData data = ScadrialAttachments.get(player);
        double strength = strength(player, AllomanticFuel.PEWTER);

        if (strength > 0.0 && data.getPewterDebt() > 0.0) {
            if (data.getPewterRecoveryDelay() > 0) {
                data.setPewterRecoveryDelay(
                        data.getPewterRecoveryDelay() - 1
                );
            } else {
                double recovery =
                        PhysicalInternalAllomancyMath.recoveryPerSecond(strength)
                                / 20.0;

                data.setPewterDebt(data.getPewterDebt() - recovery);
            }
        }

        if (player.tickCount % 5 == 0) {
            PhysicalInternalAllomancyNetworking.sync(player);
        }
    }

    private static void updateAttributes(
            ServerPlayer player,
            double strength
    ) {
        double unit = PhysicalInternalAllomancyMath.unit(strength);

        var multiplied =
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;

        var added =
                AttributeModifier.Operation.ADD_VALUE;

        modifier(player, Attributes.ATTACK_DAMAGE, unit, multiplied);

        modifier(
                player,
                Attributes.MOVEMENT_SPEED,
                0.4 * unit,
                multiplied
        );

        modifier(
                player,
                Attributes.JUMP_STRENGTH,
                0.5 * unit,
                multiplied
        );

        modifier(
                player,
                Attributes.ATTACK_KNOCKBACK,
                0.6 * unit,
                added
        );

        modifier(
                player,
                Attributes.KNOCKBACK_RESISTANCE,
                Math.min(0.95, 0.75 * unit),
                added
        );

        modifier(
                player,
                Attributes.EXPLOSION_KNOCKBACK_RESISTANCE,
                Math.min(0.95, 0.8 * unit),
                added
        );
    }

    private static void modifier(
            ServerPlayer player,
            Holder<Attribute> attribute,
            double amount,
            AttributeModifier.Operation operation
    ) {
        var instance = player.getAttribute(attribute);

        if (instance == null) {
            return;
        }

        var previous = instance.getModifier(PEWTER_MODIFIER);

        if (previous != null
                && previous.amount() == amount
                && previous.operation() == operation) {
            return;
        }

        instance.removeModifier(PEWTER_MODIFIER);

        if (amount != 0.0) {
            instance.addTransientModifier(
                    new AttributeModifier(
                            PEWTER_MODIFIER,
                            amount,
                            operation
                    )
            );
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void incomingDamage(
            LivingIncomingDamageEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer
                && event.getSource().is(PEWTER_DEBT)) {

            // The debt must not be absorbed again by golden hearts.
            event.addReductionModifier(
                    DamageContainer.Reduction.ABSORPTION,
                    (container, reduction) -> 0.0F
            );
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (event.getSource().is(PEWTER_DEBT)) {
            event.setNewDamage(event.getOriginalDamage());
            return;
        }

        // Preserve void and administrative kill behavior.
        if (event.getSource().is(
                DamageTypeTags.BYPASSES_INVULNERABILITY
        )) {
            return;
        }

        double strength = strength(player, AllomanticFuel.PEWTER);
        double damage = event.getNewDamage();

        if (strength <= 0.0
                || !Double.isFinite(damage)
                || damage <= 0.0) {
            return;
        }

        if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            damage *= 1.0
                    - PhysicalInternalAllomancyMath.explosionReduction(strength);
        }

        ScadrialPlayerData data = ScadrialAttachments.get(player);

        double deferred = PhysicalInternalAllomancyMath.deferredDamage(
                data.getPewterDebt(),
                damage,
                strength
        );

        double immediate =
                (damage - deferred)
                        * (1.0 - PhysicalInternalAllomancyMath
                        .immediateReduction(strength));

        data.setPewterDebt(data.getPewterDebt() + deferred);

        data.setPewterRecoveryDelay(
                PhysicalInternalAllomancyMath.RECOVERY_DELAY_TICKS
        );

        event.setNewDamage((float) immediate);

        PhysicalInternalAllomancyNetworking.sync(player);
    }

    private static void settleDebt(ServerPlayer player) {
        ScadrialPlayerData data = ScadrialAttachments.get(player);
        double debt = data.getPewterDebt();

        if (debt <= 0.0
                || !player.isAlive()
                || player.isSpectator()
                || player.getAbilities().invulnerable) {
            return;
        }

        DamageSource source = new DamageSource(
                player.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(PEWTER_DEBT)
        );

        // Clear first to prevent recursive damage processing.
        data.setPewterDebt(0.0);
        data.setPewterRecoveryDelay(0);

        if (!player.hurt(source, (float) debt)) {
            // Keep the debt if another mod cancels its application.
            data.setPewterDebt(debt);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void cloned(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }

        // NeoForge copies attachments before this LOWEST-priority handler.
        ScadrialPlayerData data =
                ScadrialAttachments.get(event.getEntity());

        data.setPewterDebt(0.0);
        data.setPewterRecoveryDelay(0);
    }
}