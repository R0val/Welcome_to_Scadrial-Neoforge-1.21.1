package net.rovalio.scadrialmod.power.allomancy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class MetalSourceProperties {

    public static final double PLAYER_BASE_MASS_KG = 80.0;

    public static final String ENTITY_MASS_KEY =
            "scadrial_allomantic_mass";

    public static final String INVESTED_CHARGE_KEY =
            "scadrial_invested_charge";

    private static final Map<ResourceLocation, Double> ITEM_MASSES =
            new HashMap<>();

    private static final Map<ResourceLocation, Double> ENTITY_MASSES =
            new HashMap<>();

    static {
        registerVanillaItem("iron_block", 1_500.0);
        registerVanillaItem("gold_block", 1_500.0);
        registerVanillaItem("raw_iron_block", 1_500.0);
        registerVanillaItem("raw_gold_block", 1_500.0);
        registerVanillaItem("raw_copper_block", 1_500.0);
        registerVanillaItem("netherite_block", 2_500.0);

        registerVanillaItem("anvil", 270.0);
        registerVanillaItem("chipped_anvil", 270.0);
        registerVanillaItem("damaged_anvil", 270.0);

        registerVanillaItem("trident", 3.0);
        registerVanillaItem("arrow", 0.05);
        registerVanillaItem("spectral_arrow", 1.0);
        registerVanillaItem("tipped_arrow", 0.05);
        registerVanillaItem("shield", 5.0);

        registerEntityMass(
                ResourceLocation.withDefaultNamespace("iron_golem"),
                2_000.0
        );
    }

    private MetalSourceProperties() {
    }

    private static void registerVanillaItem(
            String name,
            double kg
    ) {
        registerItemMass(
                ResourceLocation.withDefaultNamespace(name),
                kg
        );
    }

    public static void registerItemMass(
            ResourceLocation id,
            double kg
    ) {
        requireMass(kg);
        ITEM_MASSES.put(id, kg);
    }

    public static void registerEntityMass(
            ResourceLocation id,
            double kg
    ) {
        requireMass(kg);
        ENTITY_MASSES.put(id, kg);
    }

    private static void requireMass(double kg) {
        if (!Double.isFinite(kg) || kg <= 0.0) {
            throw new IllegalArgumentException(
                    "Mass must be positive and finite"
            );
        }
    }

    public static double stackMass(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0;
        }

        return unitMass(stack) * stack.getCount();
    }

    private static double unitMass(ItemStack stack) {
        double custom = stack.getOrDefault(
                AllomanticPhysicalComponents.MASS,
                0.0
        );

        if (positive(custom)) {
            return custom;
        }

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(
                stack.getItem()
        );

        Double registered = ITEM_MASSES.get(id);

        if (registered != null) {
            return registered;
        }

        if (stack.getItem() instanceof BlockItem blockItem) {
            return blockMass(id, blockItem);
        }

        return equipmentOrMaterialMass(id.getPath());
    }

    private static double blockMass(
            ResourceLocation id,
            BlockItem item
    ) {
        String name = id.getPath();

        if (name.contains("copper")) {
            if (name.endsWith("_slab")) {
                return 400.0;
            }

            if (name.endsWith("_stairs")) {
                return 600.0;
            }

            if (name.endsWith("_trapdoor")) {
                return 60.0;
            }

            if (name.endsWith("_door")) {
                return 120.0;
            }

            if (name.endsWith("_grate")
                    || name.endsWith("_bulb")) {
                return 100.0;
            }

            return 800.0;
        }

        boolean metallic = MetalSourceDetector.isDetectableBlock(
                item.getBlock().defaultBlockState()
        );

        return metallic ? 500.0 : 50.0;
    }

    private static double equipmentOrMaterialMass(String name) {
        if (name.endsWith("_nugget")) {
            return 1.0 / 9.0;
        }

        if (name.endsWith("_chestplate")) {
            return 8.0;
        }

        if (name.endsWith("_leggings")) {
            return 6.0;
        }

        if (name.endsWith("_helmet")
                || name.endsWith("_sword")) {
            return 3.0;
        }

        if (name.endsWith("_boots")) {
            return 2.0;
        }

        return 1.0;
    }

    public static double mass(Entity entity) {
        double total = bodyMass(entity);

        for (Entity passenger : entity.getPassengers()) {
            total += mass(passenger);
        }

        return Math.max(0.0001, total);
    }

    private static double bodyMass(Entity entity) {
        double custom = entity.getPersistentData()
                .getDouble(ENTITY_MASS_KEY);

        if (positive(custom)) {
            return custom;
        }

        if (entity instanceof ItemEntity item) {
            return stackMass(item.getItem());
        }

        if (entity instanceof FallingBlockEntity falling) {
            ItemStack stack = falling.getBlockState()
                    .getBlock()
                    .asItem()
                    .getDefaultInstance();

            return stackMass(stack);
        }

        if (entity instanceof Projectile) {
            ItemStack stack = EntityMetalSources.item(
                    entity,
                    EntityMetalSources.Part.PROJECTILE_ITEM
            );

            return Math.max(0.05, stackMass(stack));
        }

        ResourceLocation id =
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        double base = ENTITY_MASSES.getOrDefault(
                id,
                defaultBodyMass(entity)
        );

        if (entity instanceof LivingEntity living) {
            base += equipmentMass(living);
        }

        return base;
    }

    private static double defaultBodyMass(Entity entity) {
        if (entity instanceof Player) {
            return PLAYER_BASE_MASS_KG;
        }

        if (entity instanceof AbstractMinecart) {
            return 200.0;
        }

        double volume = entity.getBbWidth()
                * entity.getBbWidth()
                * entity.getBbHeight();

        return Math.max(
                1.0,
                PLAYER_BASE_MASS_KG * volume / (0.6 * 0.6 * 1.8)
        );
    }

    private static double equipmentMass(LivingEntity entity) {
        double total = 0.0;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = entity.getItemBySlot(slot);

            if (stack.isEmpty()) {
                continue;
            }

            boolean hand = slot == EquipmentSlot.MAINHAND
                    || slot == EquipmentSlot.OFFHAND;

            /*
             * Helded items and equiped armor is contabilized
             * when calculating the weight
             */
            if (hand && stack.getMaxDamage() <= 0) {
                continue;
            }

            total += unitMass(stack);
        }

        return total;
    }

    public static boolean fixed(Entity entity) {
        return entity instanceof ItemFrame;
    }

    public static double stackCharge(ItemStack stack) {
        double charge = stack.getOrDefault(
                AllomanticPhysicalComponents.INVESTED_CHARGE,
                0.0
        );

        return nonnegative(charge) * stack.getCount();
    }

    public static double charge(
            ServerLevel level,
            MetalTarget target
    ) {
        if (target.block() != null) {
            var blockEntity = level.getBlockEntity(target.block());

            return blockEntity == null
                    ? 0.0
                    : nonnegative(
                    blockEntity.getPersistentData()
                            .getDouble(INVESTED_CHARGE_KEY)
            );
        }

        Entity entity = target.entity(level);

        if (entity == null) {
            return 0.0;
        }

        ItemStack stack = investedStack(entity, target);

        double stored = entity.getPersistentData()
                .getDouble(INVESTED_CHARGE_KEY);

        return stackCharge(stack) + nonnegative(stored);
    }

    private static ItemStack investedStack(
            Entity entity,
            MetalTarget target
    ) {
        if (entity instanceof ItemEntity item) {
            return item.getItem();
        }

        if (entity instanceof Projectile) {
            return EntityMetalSources.item(
                    entity,
                    EntityMetalSources.Part.PROJECTILE_ITEM
            );
        }

        return EntityMetalSources.item(
                entity,
                target.entityTarget().part()
        );
    }

    private static boolean positive(double value) {
        return Double.isFinite(value) && value > 0.0;
    }

    private static double nonnegative(double value) {
        return positive(value) ? value : 0.0;
    }
}