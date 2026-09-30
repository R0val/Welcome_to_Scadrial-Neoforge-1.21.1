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
        List<Source> found = collectSources(player, chest, radius);

        Source nearest = found.stream()
                .min(Comparator.comparingDouble(source ->
                        source.position(1.0F).distanceToSqr(chest)
                ))
                .orElse(null);

        found.sort(sourceOrder(player, chest, nearest));

        return List.copyOf(
                found.subList(
                        0,
                        Math.min(found.size(), maxResults)
                )
        );
    }

    private static List<Source> collectSources(
            ServerPlayer player,
            Vec3 chest,
            double radius
    ) {
        AABB area = new AABB(chest, chest).inflate(radius);
        List<Source> result = new ArrayList<>();

        List<Entity> entities = player.serverLevel().getEntities(
                player,
                area,
                entity -> entity.isAlive()
                        && !entity.isSpectator()
                        && entity.getRootVehicle() != player.getRootVehicle()
        );

        for (Entity entity : entities) {
            for (Part part : PARTS) {
                Source source = new Source(entity, part);

                if (source.isValidFor(player, radius)) {
                    result.add(source);
                }
            }
        }

        return result;
    }

    private static Comparator<Source> sourceOrder(
            ServerPlayer player,
            Vec3 chest,
            Source nearest
    ) {
        MetalTarget selected = ExternalAllomancyPhysics.selected(player);
        Vec3 eyes = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        return Comparator
                .comparingInt((Source source) ->
                        sourcePriority(
                                source,
                                nearest,
                                selected,
                                eyes,
                                look
                        )
                )
                .thenComparingDouble(source -> {
                    Vec3 point = source.position(1.0F);

                    double alignment = MetalTargetSelector.alignment(
                            eyes, look, point
                    );

                    return alignment >= MetalTargetSelector.ACQUIRE_COS
                            ? -alignment
                            : point.distanceToSqr(chest);
                })
                .thenComparingInt(source ->
                        source.entity().getId()
                )
                .thenComparing(source ->
                        source.part().serializedName()
                );
    }

    private static int sourcePriority(
            Source source,
            Source nearest,
            MetalTarget selected,
            Vec3 eyes,
            Vec3 look
    ) {
        var target = selected.entityTarget();

        if (target != null
                && target.uuid().equals(source.entity().getUUID())
                && target.part() == source.part()) {
            return 0;
        }

        if (source.equals(nearest)) {
            return 1;
        }

        double alignment = MetalTargetSelector.alignment(
                eyes,
                look,
                source.position(1.0F)
        );

        return alignment >= MetalTargetSelector.ACQUIRE_COS
                ? 2
                : 3;
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
