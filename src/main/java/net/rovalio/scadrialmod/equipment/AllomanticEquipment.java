package net.rovalio.scadrialmod.equipment;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.entity.CoinProjectile;
import net.rovalio.scadrialmod.item.ScadrialItems;
import net.rovalio.scadrialmod.player.ScadrialAttachments;
import net.rovalio.scadrialmod.power.allomancy.*;
import net.rovalio.scadrialmod.power.allomancy.physical.external.ExternalAllomancyMath;
import net.rovalio.scadrialmod.power.allomancy.physical.external.ExternalAllomancyPerception;
import net.rovalio.scadrialmod.power.allomancy.physical.external.MetalTargetSelector;
import net.rovalio.scadrialmod.sound.ScadrialSounds;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ScadrialMod.MOD_ID)
public final class AllomanticEquipment {

    public static final int SHOT_COOLDOWN_TICKS = 20;

    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(
                    Registries.ENTITY_TYPE,
                    ScadrialMod.MOD_ID
            );

    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(
                    Registries.MENU,
                    ScadrialMod.MOD_ID
            );

    public static final DeferredHolder<
            EntityType<?>,
            EntityType<CoinProjectile>
            > COIN = ENTITIES.register(
            "coin_projectile",
            () -> EntityType.Builder
                    .<CoinProjectile>of(
                            CoinProjectile::new,
                            MobCategory.MISC
                    )
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(
                            ScadrialMod.MOD_ID
                                    + ":coin_projectile"
                    )
    );

    public static final DeferredHolder<
            MenuType<?>,
            MenuType<EquipmentMenu>
            > MENU = MENUS.register(
            "allomantic_equipment",
            () -> IMenuTypeExtension.create(
                    EquipmentMenu::new
            )
    );

    private static final Map<UUID, DrawnItem> DRAWN =
            new HashMap<>();

    private record DrawnItem(
            ItemStack belt,
            int beltSlot,
            int pocket,
            int handSlot,
            ItemStack item,
            int parkedSlot,
            ItemStack parkedItem
    ) {}

    private AllomanticEquipment() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        MENUS.register(bus);
    }

    public static void open(
            ServerPlayer player,
            InteractionHand hand
    ) {
        if (!canAct(player)) {
            return;
        }

        if (EquipmentStorage.isBelt(player.getItemInHand(hand))) {
            stow(player);
        }

        int index = hand == InteractionHand.MAIN_HAND
                ? player.getInventory().selected
                : EquipmentStorage.OFFHAND_SLOT;

        openAt(player, index);
    }

    public static void openEquipped(ServerPlayer player) {
        if (!canAct(player)) {
            return;
        }

        stow(player);

        int index = EquipmentStorage.findBelt(player);

        if (index >= 0) {
            openAt(player, index);
        }
    }

    private static void openAt(
            ServerPlayer player,
            int index
    ) {
        ItemStack owner = player.getInventory().getItem(index);
        boolean belt = EquipmentStorage.isBelt(owner);

        if (!belt && !EquipmentStorage.isPouch(owner)) {
            return;
        }

        player.openMenu(
                new SimpleMenuProvider(
                        (id, inventory, viewer) ->
                                new EquipmentMenu(
                                        id,
                                        inventory,
                                        index,
                                        belt,
                                        true
                                ),
                        Component.translatable(
                                belt
                                        ? "menu.welcome_to_scadrial.toolbelt"
                                        : "menu.welcome_to_scadrial.coin_pouch"
                        )
                ),
                buffer -> {
                    buffer.writeVarInt(index);
                    buffer.writeBoolean(belt);
                }
        );
    }

    public static void select(ServerPlayer player, int pocket) {
        if (pocket < 0
                || pocket >= EquipmentStorage.BELT_SIZE
                || !canAct(player)) {
            return;
        }

        stow(player);

        int beltSlot = EquipmentStorage.findBelt(player);

        if (beltSlot < 0) {
            return;
        }

        ItemStack belt = player.getInventory().getItem(beltSlot);
        var contents = EquipmentStorage.read(belt);
        ItemStack item = contents.get(pocket);

        if (!EquipmentStorage.accepts(true, pocket, item)) {
            return;
        }

        int handSlot = player.getInventory().selected;
        ItemStack parkedItem = player.getMainHandItem();
        int parkedSlot = -1;

        if (!parkedItem.isEmpty()) {
            parkedSlot = player.getInventory().getFreeSlot();

            if (parkedSlot < 0) {
                player.displayClientMessage(
                        Component.translatable(
                                "message.welcome_to_scadrial.inventory_space"
                        ),
                        true
                );
                return;
            }

            player.getInventory().setItem(
                    parkedSlot,
                    parkedItem
            );

            if (beltSlot == handSlot) {
                beltSlot = parkedSlot;
            }
        }

        contents.set(pocket, ItemStack.EMPTY);
        EquipmentStorage.write(belt, contents);
        player.getInventory().setItem(handSlot, item);

        DRAWN.put(
                player.getUUID(),
                new DrawnItem(
                        belt,
                        beltSlot,
                        pocket,
                        handSlot,
                        item,
                        parkedSlot,
                        parkedItem
                )
        );

        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();

        EquipmentNetworking.sync(
                player,
                beltSlot,
                pocket,
                handSlot
        );
    }

    public static void stow(ServerPlayer player) {
        DrawnItem drawn = DRAWN.remove(player.getUUID());

        if (drawn == null) {
            return;
        }

        ItemStack held = player.getInventory()
                .getItem(drawn.handSlot());

        if (belongsToSelection(drawn, held)
                && player.getInventory().getItem(drawn.beltSlot())
                == drawn.belt()) {

            var contents = EquipmentStorage.read(drawn.belt());

            if (contents.get(drawn.pocket()).isEmpty()
                    && EquipmentStorage.accepts(
                    true,
                    drawn.pocket(),
                    held
            )) {

                contents.set(drawn.pocket(), held);
                EquipmentStorage.write(drawn.belt(), contents);

                player.getInventory().setItem(
                        drawn.handSlot(),
                        ItemStack.EMPTY
                );

                player.getInventory().setChanged();
            }
        }

        if (drawn.parkedSlot() >= 0
                && player.getInventory()
                .getItem(drawn.handSlot()).isEmpty()
                && player.getInventory()
                .getItem(drawn.parkedSlot())
                == drawn.parkedItem()) {

            player.getInventory().setItem(
                    drawn.handSlot(),
                    drawn.parkedItem()
            );

            player.getInventory().setItem(
                    drawn.parkedSlot(),
                    ItemStack.EMPTY
            );

            player.getInventory().setChanged();
        }

        player.inventoryMenu.broadcastChanges();
        EquipmentNetworking.sync(player, -1, -1, -1);
    }

    private static boolean belongsToSelection(
            DrawnItem drawn,
            ItemStack held
    ) {
        return held == drawn.item()
                || (EquipmentStorage.isVial(drawn.item())
                && held.is(ScadrialItems.METAL_VIAL.get()));
    }

    public static boolean canAct(ServerPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && player.containerMenu == player.inventoryMenu;
    }

    public static boolean steelActive(ServerPlayer player) {
        var data = ScadrialAttachments.get(player);

        return canAct(player)
                && data.isUsableAndBurning(AllomanticFuel.STEEL)
                && !data.isUsableAndBurning(AllomanticFuel.ALUMINIUM)
                && AllomancyBurnManager.effectiveStrength(player, data) > 0.0;
    }

    public static CoinProjectile shootForPush(
            ServerPlayer player,
            double radius
    ) {
        if (!steelActive(player)
                || EquipmentStorage.freeHand(player) < 0) {
            return null;
        }

        ItemStack held = EquipmentStorage.isPouch(
                player.getMainHandItem()
        )
                ? player.getMainHandItem()
                : player.getOffhandItem();

        if (EquipmentStorage.isPouch(held)) {
            return shoot(player, held);
        }

        int beltSlot = EquipmentStorage.findBelt(player);

        if (beltSlot < 0) {
            return null;
        }

        ExternalAllomancyPerception.sync(player);

        if (MetalTargetSelector.hasAimedTarget(
                player,
                ExternalAllomancyPerception.current(player),
                radius
        )) {
            return null;
        }

        ItemStack belt = player.getInventory().getItem(beltSlot);
        var contents = EquipmentStorage.read(belt);

        ItemStack pouch = contents.get(
                EquipmentStorage.POUCH_SLOT
        );

        CoinProjectile coin = shoot(player, pouch);

        if (coin != null) {
            contents.set(EquipmentStorage.POUCH_SLOT, pouch);
            EquipmentStorage.write(belt, contents);
            player.inventoryMenu.broadcastChanges();
        }

        return coin;
    }

    private static CoinProjectile shoot(
            ServerPlayer player,
            ItemStack pouch
    ) {
        int index = EquipmentStorage.firstCoin(pouch);

        if (index < 0
                || player.getCooldowns().isOnCooldown(
                ScadrialItems.COIN_POUCH.get()
        )) {
            return null;
        }

        var contents = EquipmentStorage.read(pouch);
        ItemStack stack = contents.get(index);
        var data = ScadrialAttachments.get(player);

        boolean duralumin =
                data.isUsableAndBurning(AllomanticFuel.DURALUMIN);

        double strength = ExternalAllomancyMath.externalStrength(
                AllomancyBurnManager.effectiveStrength(player, data),
                duralumin
        );

        float speed = (float) ExternalAllomancyMath.projectileSpeedLimit(
                strength
        );

        CoinProjectile coin = new CoinProjectile(
                player.level(),
                player,
                stack
        );

        var look = player.getLookAngle();
        coin.shoot(look.x, look.y, look.z, speed, 0.0F);

        if (!player.serverLevel().addFreshEntity(coin)) {
            return null;
        }

        stack.shrink(1);
        EquipmentStorage.write(pouch, contents);

        player.getCooldowns().addCooldown(
                ScadrialItems.COIN_POUCH.get(),
                SHOT_COOLDOWN_TICKS
        );

        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();

        player.serverLevel().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ScadrialSounds.COINSHOT.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        return coin;
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        DrawnItem drawn = DRAWN.get(player.getUUID());

        if (drawn == null) {
            return;
        }

        if (!player.isAlive()) {
            DRAWN.remove(player.getUUID());
            return;
        }

        ItemStack held = player.getInventory().getItem(
                drawn.handSlot()
        );

        if (player.getInventory().selected != drawn.handSlot()
                || player.getInventory().getItem(drawn.beltSlot())
                != drawn.belt()
                || held.isEmpty()
                || !belongsToSelection(drawn, held)) {
            stow(player);
        }
    }

    @SubscribeEvent
    public static void logout(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            stow(player);
        }
    }

    @SubscribeEvent
    public static void dimension(
            PlayerEvent.PlayerChangedDimensionEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            stow(player);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        DRAWN.clear();
    }
}