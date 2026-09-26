package net.rovalio.scadrialmod.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.item.custom.AllomanticContainerContents.Form;
import net.rovalio.scadrialmod.item.custom.AllomanticContainerContents.Portion;
import net.rovalio.scadrialmod.item.ScadrialItems;
import net.rovalio.scadrialmod.network.ScadrialNetworking;
import net.rovalio.scadrialmod.player.ScadrialAttachments;
import net.rovalio.scadrialmod.player.ScadrialPlayerData;
import net.rovalio.scadrialmod.power.allomancy.AllomanticFuel;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class AllomanticContainerItem extends Item {

    private static final long RESERVE_CAPACITY_PER_FUEL = 16_000L;

    private static final int FLASK_MAX_USE_DURATION = 72_000;
    private static final long FLASK_SIP_SUBUNITS = 1_000L;
    private static final long BOTTLE_DOSE_SUBUNITS = 16_000L;
    private static final long BUNDLE_DOSE_SUBUNITS = 2_000L;

    private static final AllomanticFuel[] FUELS =
            AllomanticFuel.values();

    public enum Kind {
        VIAL(24, true, true, true),
        FLASK(72, true, true, false),
        BOTTLE(144, true, true, true),
        BUNDLE(512, true, false, false);

        private final long storageCapacity;
        private final boolean acceptsDust;
        private final boolean acceptsShavings;
        private final boolean acceptsBeads;

        Kind(
                long space,
                boolean acceptsDust,
                boolean acceptsShavings,
                boolean acceptsBeads
        ) {
            this.storageCapacity = space * 1_000L;
            this.acceptsDust = acceptsDust;
            this.acceptsShavings = acceptsShavings;
            this.acceptsBeads = acceptsBeads;
        }

        private boolean accepts(Form form) {
            return switch (form) {
                case DUST -> acceptsDust;
                case SHAVINGS -> acceptsShavings;
                case BEAD -> acceptsBeads;
            };
        }
    }

    private final Kind kind;
    private final Supplier<Item> emptyItem;

    public AllomanticContainerItem(
            Kind kind,
            Supplier<Item> emptyItem,
            Properties properties
    ) {
        super(properties.stacksTo(1));
        this.kind = kind;
        this.emptyItem = emptyItem;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack container = player.getItemInHand(hand);
        AllomanticContainerContents contents = contents(container);

        if (!valid(contents)) {
            return InteractionResultHolder.fail(container);
        }

        if (contents.isEmpty()) {
            return InteractionResultHolder.pass(container);
        }

        if (player instanceof ServerPlayer serverPlayer
                && !hasReserveSpace(serverPlayer, contents)) {
            return InteractionResultHolder.fail(container);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(container);
    }

    public static ItemStack fillForCrafting(
            ItemStack containerStack,
            ItemStack metalStack
    ) {
        if (containerStack.isEmpty()
                || metalStack.isEmpty()
                || !(containerStack.getItem()
                instanceof AllomanticContainerItem container)) {
            return ItemStack.EMPTY;
        }

        AllomanticContainerContents existing =
                contents(containerStack);

        if (!container.valid(existing)) {
            return ItemStack.EMPTY;
        }

        AllomanticContainerContents filled;

        if (metalStack.is(ScadrialItems.ALLOMANTIC_BEAD.get())) {
            if (!existing.isEmpty()) {
                return ItemStack.EMPTY;
            }

            List<Portion> maxedReserves =
                    new ArrayList<>(FUELS.length);

            for (AllomanticFuel fuel : FUELS) {
                maxedReserves.add(new Portion(
                        fuel,
                        Form.DUST,
                        RESERVE_CAPACITY_PER_FUEL
                ));
            }

            filled = new AllomanticContainerContents(maxedReserves);
        } else {
            MetalInput input = metalInput(metalStack);

            if (input == null) {
                return ItemStack.EMPTY;
            }

            filled = existing.withAdded(
                    input.fuel(),
                    input.form(),
                    input.form().subunitsPerItem()
            );
        }

        if (!container.valid(filled)) {
            return ItemStack.EMPTY;
        }

        ItemStack result = containerStack.copy();
        result.setCount(1);
        result.set(
                ScadrialDataComponents.ALLOMANTIC_CONTENTS.get(),
                filled
        );

        return result;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return kind == Kind.BUNDLE
                ? UseAnim.EAT
                : UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(
            ItemStack stack,
            LivingEntity entity
    ) {
        Form form = slowestForm(contents(stack));

        return switch (kind) {
            case VIAL -> switch (form) {
                case DUST -> 12;
                case SHAVINGS -> 32;
                case BEAD -> 48;
            };

            case BOTTLE -> switch (form) {
                case DUST -> 24;
                case SHAVINGS -> 48;
                case BEAD -> 72;
            };

            case FLASK -> FLASK_MAX_USE_DURATION;
            case BUNDLE -> 20;
        };
    }

    @Override
    public ItemStack finishUsingItem(
            ItemStack stack,
            Level level,
            LivingEntity entity
    ) {
        if (level.isClientSide()
                || !(entity instanceof ServerPlayer player)) {
            return stack;
        }

        AllomanticContainerContents contents = contents(stack);
        boolean developmentPayload = isDevelopmentPayload(contents);

        if (kind == Kind.FLASK && !developmentPayload) {
            return stack;
        }

        if (!valid(contents) || contents.isEmpty()) {
            return stack;
        }

        long dose = developmentPayload
                ? Long.MAX_VALUE
                : switch (kind) {
            case VIAL -> Long.MAX_VALUE;
            case BOTTLE -> BOTTLE_DOSE_SUBUNITS;
            case BUNDLE -> BUNDLE_DOSE_SUBUNITS;
            case FLASK -> FLASK_SIP_SUBUNITS;
        };

        Consumption consumption =
                transferDose(player, contents, dose);

        if (consumption.transferred() <= 0) {
            return stack;
        }

        ItemStack result;

        if (consumption.remaining().isEmpty()
                || developmentPayload) {
            result = emptiedStack(stack);
        } else {
            stack.set(
                    ScadrialDataComponents.ALLOMANTIC_CONTENTS.get(),
                    consumption.remaining()
            );
            result = stack;
        }

        ScadrialNetworking.sync(player);
        return result;
    }

    @Override
    public void onUseTick(
            Level level,
            LivingEntity entity,
            ItemStack stack,
            int remainingUseDuration
    ) {
        if (level.isClientSide()
                || kind != Kind.FLASK
                || !(entity instanceof ServerPlayer player)) {
            return;
        }

        AllomanticContainerContents contents = contents(stack);

        if (!valid(contents) || contents.isEmpty()) {
            player.stopUsingItem();
            return;
        }

        int interval = slowestForm(contents) == Form.DUST
                ? 5
                : 8;

        int elapsed =
                FLASK_MAX_USE_DURATION - remainingUseDuration;

        if (elapsed <= 0 || elapsed % interval != 0) {
            return;
        }

        boolean developmentPayload = isDevelopmentPayload(contents);

        Consumption consumption = transferDose(
                player,
                contents,
                developmentPayload
                        ? Long.MAX_VALUE
                        : FLASK_SIP_SUBUNITS
        );

        if (consumption.transferred() <= 0) {
            player.stopUsingItem();
            return;
        }

        AllomanticContainerContents remaining =
                developmentPayload
                        ? AllomanticContainerContents.EMPTY
                        : consumption.remaining();

        if (remaining.isEmpty()) {
            InteractionHand usedHand = player.getUsedItemHand();

            player.stopUsingItem();
            player.setItemInHand(
                    usedHand,
                    emptiedStack(stack)
            );
        } else {
            stack.set(
                    ScadrialDataComponents.ALLOMANTIC_CONTENTS.get(),
                    remaining
            );

            if (!hasReserveSpace(player, remaining)) {
                player.stopUsingItem();
            }
        }

        ScadrialNetworking.sync(player);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltip, flag);

        AllomanticContainerContents contents = contents(stack);

        if (!valid(contents)) {
            tooltip.add(
                    Component.literal("Invalid allomantic contents")
                            .withStyle(ChatFormatting.RED)
            );
            return;
        }

        long total = 0;

        for (Portion portion : contents.portions()) {
            total += portion.subunits();
        }

        double occupiedPercent =
                100.0 * usedStorage(contents)
                        / kind.storageCapacity;

        tooltip.add(
                Component.literal("Metal: " + total + " units")
                        .withStyle(ChatFormatting.GRAY)
        );

        tooltip.add(
                Component.literal(String.format(
                        Locale.ROOT,
                        "Space: %.1f%%",
                        occupiedPercent
                )).withStyle(ChatFormatting.GRAY)
        );

        for (Portion portion : contents.portions()) {
            tooltip.add(
                    Component.literal(String.format(
                            Locale.ROOT,
                            "%s (%s): units",
                            portion.fuel().getSerializedName(),
                            portion.subunits()
                    )).withStyle(ChatFormatting.GOLD)
            );
        }
    }

    private boolean valid(
            AllomanticContainerContents contents
    ) {
        if (isDevelopmentPayload(contents)) {
            return true;
        }

        long remainingSpace = kind.storageCapacity;

        EnumSet<AllomanticFuel> fuels =
                EnumSet.noneOf(AllomanticFuel.class);

        int maxFuels = kind == Kind.BUNDLE
                ? 1
                : Integer.MAX_VALUE;

        for (Portion portion : contents.portions()) {
            if (portion.fuel() == null
                    || portion.form() == null
                    || portion.subunits() <= 0
                    || !kind.accepts(portion.form())) {
                return false;
            }

            long cost = portion.form().spaceCostPerSubunit();

            if (portion.subunits() > remainingSpace / cost) {
                return false;
            }

            remainingSpace -= portion.subunits() * cost;
            fuels.add(portion.fuel());

            int formLimit = switch (portion.form()) {
                case DUST -> Integer.MAX_VALUE;
                case SHAVINGS -> 4;
                case BEAD -> 1;
            };

            maxFuels = Math.min(maxFuels, formLimit);
        }

        return fuels.size() <= maxFuels;
    }

    private static boolean isDevelopmentPayload(
            AllomanticContainerContents contents
    ) {
        if (contents == null
                || contents.portions().size() != FUELS.length) {
            return false;
        }

        EnumSet<AllomanticFuel> included =
                EnumSet.noneOf(AllomanticFuel.class);

        for (Portion portion : contents.portions()) {
            if (portion == null
                    || portion.fuel() == null
                    || portion.form() != Form.DUST
                    || portion.subunits() != RESERVE_CAPACITY_PER_FUEL
                    || !included.add(portion.fuel())) {
                return false;
            }
        }

        return included.size() == FUELS.length;
    }

    private static long usedStorage(
            AllomanticContainerContents contents
    ) {
        long occupied = 0;

        for (Portion portion : contents.portions()) {
            occupied += portion.subunits()
                    * portion.form().spaceCostPerSubunit();
        }

        return occupied;
    }

    private static Form slowestForm(
            AllomanticContainerContents contents
    ) {
        Form result = Form.DUST;

        for (Portion portion : contents.portions()) {
            if (portion.form() == Form.BEAD) {
                return Form.BEAD;
            }

            if (portion.form() == Form.SHAVINGS) {
                result = Form.SHAVINGS;
            }
        }

        return result;
    }

    private static boolean hasReserveSpace(
            ServerPlayer player,
            AllomanticContainerContents contents
    ) {
        ScadrialPlayerData data = ScadrialAttachments.get(player);

        for (Portion portion : contents.portions()) {
            if (data.getAllomanticReserveSpaceSubunits(
                    portion.fuel(),
                    RESERVE_CAPACITY_PER_FUEL
            ) > 0) {
                return true;
            }
        }

        return false;
    }

    private static Consumption transferDose(
            ServerPlayer player,
            AllomanticContainerContents contents,
            long maxDose
    ) {
        ScadrialPlayerData data = ScadrialAttachments.get(player);
        List<Portion> portions = contents.portions();

        long[] storedByFuel = new long[FUELS.length];

        for (Portion portion : portions) {
            storedByFuel[portion.fuel().ordinal()] +=
                    portion.subunits();
        }

        long[] usableByFuel = new long[FUELS.length];

        for (int i = 0; i < FUELS.length; i++) {
            if (storedByFuel[i] <= 0) {
                continue;
            }

            long reserveSpace = Math.max(
                    0L,
                    data.getAllomanticReserveSpaceSubunits(
                            FUELS[i],
                            RESERVE_CAPACITY_PER_FUEL
                    )
            );

            usableByFuel[i] = Math.min(
                    storedByFuel[i],
                    reserveSpace
            );
        }

        long[] requestedByFuel =
                distribute(maxDose, usableByFuel);

        long[] acceptedByFuel = new long[FUELS.length];
        long transferred = 0;

        for (int i = 0; i < FUELS.length; i++) {
            if (requestedByFuel[i] <= 0) {
                continue;
            }

            acceptedByFuel[i] =
                    data.addAllomanticReserveSubunits(
                            FUELS[i],
                            requestedByFuel[i],
                            RESERVE_CAPACITY_PER_FUEL
                    );

            transferred += acceptedByFuel[i];
        }

        if (transferred <= 0) {
            return new Consumption(contents, 0L);
        }

        long[] removedByPortion = new long[portions.size()];

        for (int fuelIndex = 0;
             fuelIndex < FUELS.length;
             fuelIndex++) {

            if (acceptedByFuel[fuelIndex] <= 0) {
                continue;
            }

            long[] weights = new long[portions.size()];

            for (int i = 0; i < portions.size(); i++) {
                Portion portion = portions.get(i);

                if (portion.fuel() == FUELS[fuelIndex]) {
                    weights[i] = portion.subunits();
                }
            }

            long[] removal = distribute(
                    acceptedByFuel[fuelIndex],
                    weights
            );

            for (int i = 0; i < removal.length; i++) {
                removedByPortion[i] += removal[i];
            }
        }

        List<Portion> remaining = new ArrayList<>();

        for (int i = 0; i < portions.size(); i++) {
            Portion portion = portions.get(i);
            long amount =
                    portion.subunits() - removedByPortion[i];

            if (amount > 0) {
                remaining.add(new Portion(
                        portion.fuel(),
                        portion.form(),
                        amount
                ));
            }
        }

        AllomanticContainerContents result =
                remaining.isEmpty()
                        ? AllomanticContainerContents.EMPTY
                        : new AllomanticContainerContents(remaining);

        return new Consumption(result, transferred);
    }

    private static long[] distribute(
            long requested,
            long[] weights
    ) {
        long[] result = new long[weights.length];
        long total = 0;

        for (long weight : weights) {
            total += weight;
        }

        if (requested <= 0 || total <= 0) {
            return result;
        }

        long target = Math.min(requested, total);
        long[] remainders = new long[weights.length];
        long assigned = 0;

        for (int i = 0; i < weights.length; i++) {
            long product = target * weights[i];

            result[i] = product / total;
            remainders[i] = product % total;
            assigned += result[i];
        }

        long pending = target - assigned;

        while (pending > 0) {
            int best = -1;

            for (int i = 0; i < weights.length; i++) {
                if (result[i] >= weights[i]
                        || remainders[i] < 0) {
                    continue;
                }

                if (best < 0
                        || remainders[i] > remainders[best]) {
                    best = i;
                }
            }

            if (best < 0) {
                throw new IllegalStateException(
                        "Cannot distribute allomantic dose"
                );
            }

            result[best]++;
            remainders[best] = -1;
            pending--;
        }

        return result;
    }

    private ItemStack emptiedStack(ItemStack original) {
        Item empty = emptyItem.get();

        if (original.is(empty)) {
            original.set(
                    ScadrialDataComponents.ALLOMANTIC_CONTENTS.get(),
                    AllomanticContainerContents.EMPTY
            );
            return original;
        }

        return new ItemStack(empty);
    }

    private static AllomanticContainerContents contents(
            ItemStack stack
    ) {
        return stack.getOrDefault(
                ScadrialDataComponents.ALLOMANTIC_CONTENTS.get(),
                AllomanticContainerContents.EMPTY
        );
    }

    private static MetalInput metalInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        ResourceLocation id =
                BuiltInRegistries.ITEM.getKey(stack.getItem());

        if (!ScadrialMod.MOD_ID.equals(id.getNamespace())) {
            return null;
        }

        String path = id.getPath();
        String fuelName;
        Form form;

        if (path.endsWith("_dust")) {
            form = Form.DUST;
            fuelName = path.substring(0, path.length() - 5);
        } else if (path.endsWith("_shavings")) {
            form = Form.SHAVINGS;
            fuelName = path.substring(0, path.length() - 9);
        } else if (path.endsWith("_bead")) {
            form = Form.BEAD;
            fuelName = path.substring(0, path.length() - 5);
        } else if (path.endsWith("_beads")) {
            form = Form.BEAD;
            fuelName = path.substring(0, path.length() - 6);
        } else {
            return null;
        }

        // In this project, "atium" means to the atium-electrum alloy
        if (fuelName.equals("atium")) {
            fuelName = "atium_electrum";
        }

        AllomanticFuel fuel =
                AllomanticFuel.fromSerializedName(fuelName)
                        .orElse(null);

        return fuel == null
                ? null
                : new MetalInput(fuel, form);
    }

    private record MetalInput(
            AllomanticFuel fuel,
            Form form
    ) {
    }

    private record Consumption(
            AllomanticContainerContents remaining,
            long transferred
    ) {
    }
}