package net.rovalio.scadrialmod.power.allomancy.physical.external;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.network.SyncMetalSourcesS2CPayload;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class EntityMetalSources {

    public static final TagKey<EntityType<?>> METALLIC_BODIES = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(
                    ScadrialMod.MOD_ID, "allomancy_tangible")
    );

    public static final TagKey<EntityType<?>> ALUMINIUM_BODIES = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(
                    ScadrialMod.MOD_ID, "allomancy_aluminium")
    );

    public enum Part {
        BODY(null),
        DROPPED_ITEM(null),
        FRAME_ITEM(null),
        PROJECTILE_ITEM(null),
        FALLING_BLOCK(null),
        MAIN_HAND(EquipmentSlot.MAINHAND),
        OFF_HAND(EquipmentSlot.OFFHAND),
        HEAD(EquipmentSlot.HEAD),
        CHEST(EquipmentSlot.CHEST),
        LEGS(EquipmentSlot.LEGS),
        FEET(EquipmentSlot.FEET),
        ANIMAL_ARMOR(EquipmentSlot.BODY);

        private final EquipmentSlot slot;
        private final String serializedName;

        Part(EquipmentSlot slot) {
            this.slot = slot;
            this.serializedName = name().toLowerCase(Locale.ROOT);
        }

        public String serializedName() {
            return serializedName;
        }

        public static Part byId(int id) {
            if (id < 0 || id >= PARTS.length) {
                throw new IllegalArgumentException("Unknown metal source part id: " + id);
            }

            return PARTS[id];
        }

        public static Part read(String name) {
            for (Part part : PARTS) {
                if (part.serializedName().equals(name)) {
                    return part;
                }
            }
            throw new IllegalArgumentException("Unknown metal source part: " + name);
        }
    }

    private static final Part[] PARTS = Part.values();

    public record Source(Entity entity, Part part) {
        public Source {
            Objects.requireNonNull(entity);
            Objects.requireNonNull(part);
        }

        public ItemStack stack() {
            return item(entity, part);
        }

        public Vec3 position(float partialTick) {
            return EntityMetalSources.position(entity, part, partialTick);
        }

        public boolean isValidFor(ServerPlayer player, double radius) {
            return Double.isFinite(radius) && radius > 0.0
                    && entity != player
                    && entity.level() == player.level()
                    && isSource(entity, part)
                    && position(1.0F).distanceToSqr(
                            MetalSourceDetector.chestPosition(player)) <= radius * radius;
        }
    }

    private EntityMetalSources() {
    }

    public static boolean isMetalItem(ItemStack stack) {
        if (stack.isEmpty() || stack.is(MetalSourceDetector.ALUMINIUM_ITEMS)) {
            return false;
        }
        if (stack.getItem() instanceof BlockItem blockItem) {
            var state = blockItem.getBlock().defaultBlockState();
            if (state.is(MetalSourceDetector.ALUMINIUM_BLOCKS)) {
                return false;
            }
            if (MetalSourceDetector.isDetectableBlock(state)) {
                return true;
            }
        }
        if (stack.is(MetalSourceDetector.TANGIBLE_ITEMS)) {
            return true;
        }
        ArmorTrim trim = stack.get(DataComponents.TRIM);
        return trim != null
                && trim.material().value().ingredient()
                        .is(MetalSourceDetector.TANGIBLE_ITEMS)
                && !trim.material().value().ingredient()
                        .is(MetalSourceDetector.ALUMINIUM_ITEMS);
    }

    public static ItemStack item(Entity entity, Part part) {
        if (part.slot != null && entity instanceof LivingEntity living) {
            return living.getItemBySlot(part.slot);
        }
        return switch (part) {
            case DROPPED_ITEM -> entity instanceof ItemEntity item
                    ? item.getItem() : ItemStack.EMPTY;
            case FRAME_ITEM -> entity instanceof ItemFrame frame
                    ? frame.getItem() : ItemStack.EMPTY;
            case PROJECTILE_ITEM -> {
                if (entity instanceof AbstractArrow arrow) {
                    yield arrow.getPickupItemStackOrigin();
                }
                if (entity instanceof ThrowableItemProjectile projectile) {
                    yield projectile.getItem();
                }
                yield ItemStack.EMPTY;
            }
            case FALLING_BLOCK -> entity instanceof FallingBlockEntity falling
                    ? falling.getBlockState().getBlock().asItem().getDefaultInstance()
                    : ItemStack.EMPTY;
            default -> ItemStack.EMPTY;
        };
    }

    public static boolean isSource(Entity entity, Part part) {
        if (!entity.isAlive() || entity.isSpectator()) {
            return false;
        }
        if (part == Part.BODY) {
            return entity.getType().is(METALLIC_BODIES)
                    && !entity.getType().is(ALUMINIUM_BODIES);
        }
        if (part == Part.FALLING_BLOCK) {
            return entity instanceof FallingBlockEntity falling
                    && MetalSourceDetector.isDetectableBlock(falling.getBlockState());
        }
        return isMetalItem(item(entity, part));
    }

    public static List<Source> find(
            ServerPlayer player,
            double radius,
            int maxResults
    ) {
        if (!Double.isFinite(radius)
                || radius <= 0.0
                || maxResults < 0) {
            throw new IllegalArgumentException(
                    "Invalid source search"
            );
        }

        if (maxResults == 0) {
            return List.of();
        }

        Vec3 chest = MetalSourceDetector.chestPosition(player);
        Vec3 eyes = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        List<RankedSource> found =
                collectSources(player, chest, eyes, look, radius);

        if (found.isEmpty()) {
            return List.of();
        }

        RankedSource nearest = found.get(0);

        for (RankedSource candidate : found) {
            if (candidate.distanceSquared() < nearest.distanceSquared()) {
                nearest = candidate;
            }
        }

        var selected = ExternalAllomancyPhysics.selected(player).entityTarget();

        for (RankedSource candidate : found) {
            candidate.rank(selected, nearest);
        }

        found.sort(RANKED_ORDER);

        int size = Math.min(found.size(), maxResults);
        List<Source> result = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            result.add(found.get(i).source());
        }

        return List.copyOf(result);
    }

    private static final Part[] LIVING_PARTS = {
            Part.BODY,
            Part.MAIN_HAND,
            Part.OFF_HAND,
            Part.HEAD,
            Part.CHEST,
            Part.LEGS,
            Part.FEET,
            Part.ANIMAL_ARMOR
    };

    private static final Part[] BODY_PARTS = {Part.BODY};
    private static final Part[] DROPPED_ITEM_PARTS = {Part.BODY, Part.DROPPED_ITEM};
    private static final Part[] FRAME_ITEM_PARTS = {Part.BODY, Part.FRAME_ITEM};
    private static final Part[] PROJECTILE_ITEM_PARTS = {Part.BODY, Part.PROJECTILE_ITEM};
    private static final Part[] FALLING_BLOCK_PARTS = {Part.BODY, Part.FALLING_BLOCK};

    private static final Comparator<RankedSource> RANKED_ORDER =
            Comparator.comparingInt(RankedSource::priority)
                    .thenComparingDouble(RankedSource::orderKey)
                    .thenComparingInt(ranked -> ranked.source().entity().getId())
                    .thenComparingInt(ranked -> ranked.source().part().ordinal());

    private static final class RankedSource {

        private final Source source;
        private final double distanceSquared;
        private final double alignment;
        private int priority;

        private RankedSource(
                Source source,
                double distanceSquared,
                double alignment
        ) {
            this.source = source;
            this.distanceSquared = distanceSquared;
            this.alignment = alignment;
        }

        private Source source() {
            return source;
        }

        private double distanceSquared() {
            return distanceSquared;
        }

        private int priority() {
            return priority;
        }

        private boolean aligned() {
            return alignment >= MetalTargetSelector.ACQUIRE_COS;
        }

        private double orderKey() {
            return aligned() ? -alignment : distanceSquared;
        }

        private void rank(
                SyncMetalSourcesS2CPayload.Target selected,
                RankedSource nearest
        ) {
            if (selected != null
                    && selected.part() == source.part()
                    && selected.uuid().equals(source.entity().getUUID())) {
                priority = 0;
            } else if (this == nearest) {
                priority = 1;
            } else {
                priority = aligned() ? 2 : 3;
            }
        }
    }

    private static Part[] partsFor(Entity entity) {
        if (entity instanceof LivingEntity) {
            return LIVING_PARTS;
        }

        if (entity instanceof ItemEntity) {
            return DROPPED_ITEM_PARTS;
        }

        if (entity instanceof ItemFrame) {
            return FRAME_ITEM_PARTS;
        }

        if (entity instanceof AbstractArrow
                || entity instanceof ThrowableItemProjectile) {
            return PROJECTILE_ITEM_PARTS;
        }

        if (entity instanceof FallingBlockEntity) {
            return FALLING_BLOCK_PARTS;
        }

        return BODY_PARTS;
    }

    private static List<RankedSource> collectSources(
            ServerPlayer player,
            Vec3 chest,
            Vec3 eyes,
            Vec3 look,
            double radius
    ) {
        AABB area = new AABB(chest, chest).inflate(radius);
        double radiusSquared = radius * radius;
        Entity playerVehicle = player.getRootVehicle();

        List<Entity> entities = player.serverLevel().getEntities(
                player,
                area,
                entity -> entity.isAlive()
                        && !entity.isSpectator()
                        && entity.getRootVehicle() != playerVehicle
        );

        List<RankedSource> result = new ArrayList<>();

        for (Entity entity : entities) {
            for (Part part : partsFor(entity)) {
                if (!isSource(entity, part)) {
                    continue;
                }

                Vec3 point = position(entity, part, 1.0F);
                double distanceSquared = point.distanceToSqr(chest);

                if (distanceSquared > radiusSquared) {
                    continue;
                }

                result.add(new RankedSource(
                        new Source(entity, part),
                        distanceSquared,
                        MetalTargetSelector.alignment(eyes, look, point)
                ));
            }
        }

        return result;
    }

    public static Vec3 position(Entity entity, Part part, float partialTick) {
        Vec3 origin = entity.getPosition(partialTick);
        if (part.slot == null || !(entity instanceof LivingEntity living)) {
            return origin.add(entity.getBoundingBox().getCenter()
                    .subtract(entity.position()));
        }

        double height = entity.getBbHeight();
        double fraction = switch (part) {
            case HEAD -> 0.90;
            case CHEST -> 0.65;
            case LEGS -> 0.35;
            case FEET -> 0.10;
            default -> 0.55;
        };
        Vec3 point = origin.add(0.0, height * fraction, 0.0);
        if (part != Part.MAIN_HAND && part != Part.OFF_HAND) {
            return point;
        }

        boolean right = living.getMainArm() == HumanoidArm.RIGHT;
        if (part == Part.OFF_HAND) {
            right = !right;
        }
        double yaw = Math.toRadians(Mth.rotLerp(
                partialTick, living.yBodyRotO, living.yBodyRot));
        double side = entity.getBbWidth() * (right ? -0.40 : 0.40);
        double forward = entity.getBbWidth() * 0.15;
        return point.add(
                Math.cos(yaw) * side - Math.sin(yaw) * forward,
                0.0,
                Math.sin(yaw) * side + Math.cos(yaw) * forward
        );
    }
}
