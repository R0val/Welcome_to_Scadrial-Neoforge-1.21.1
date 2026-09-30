package net.rovalio.scadrialmod.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rovalio.scadrialmod.client.ClientAllomancyBurnState;
import net.rovalio.scadrialmod.client.ClientAllomancyState;
import net.rovalio.scadrialmod.client.ClientMetalSources;
import net.rovalio.scadrialmod.player.ScadrialAttachments;
import net.rovalio.scadrialmod.player.ScadrialPlayerData;
import net.rovalio.scadrialmod.power.ScadrialPowerManager;
import net.rovalio.scadrialmod.power.allomancy.AllomancyBurnManager;
import net.rovalio.scadrialmod.power.allomancy.AllomanticFuel;
import net.rovalio.scadrialmod.power.allomancy.physical.external.ExternalAllomancyPerception;
import net.rovalio.scadrialmod.power.allomancy.physical.internal.PhysicalInternalAllomancyManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ScadrialNetworking {

    private static final Map<UUID, SyncAllomancyStateS2CPayload> LAST_STATE =
            new HashMap<>();

    private static final Map<UUID, SyncAllomanticBurnsS2CPayload> LAST_BURNS =
            new HashMap<>();

    private ScadrialNetworking() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(
                ScadrialNetworking::registerPayloads
        );

        NeoForge.EVENT_BUS.addListener(
                ScadrialNetworking::onLogin
        );
        NeoForge.EVENT_BUS.addListener(
                ScadrialNetworking::onRespawn
        );
        NeoForge.EVENT_BUS.addListener(
                ScadrialNetworking::onChangeDimension
        );
        NeoForge.EVENT_BUS.addListener(
                ScadrialNetworking::onLogout
        );
        NeoForge.EVENT_BUS.addListener(
                ScadrialNetworking::onServerStopped
        );
        NeoForge.EVENT_BUS.addListener(
                ScadrialNetworking::onPlayerTick
        );
    }

    private static void registerPayloads(
            RegisterPayloadHandlersEvent event
    ) {
        PayloadRegistrar registrar = event.registrar("3");

        registrar.playToClient(
                SyncAllomancyStateS2CPayload.TYPE,
                SyncAllomancyStateS2CPayload.STREAM_CODEC,
                ClientAllomancyState::handleSync
        );

        registrar.playToClient(
                SyncAllomanticBurnsS2CPayload.TYPE,
                SyncAllomanticBurnsS2CPayload.STREAM_CODEC,
                ClientAllomancyBurnState::handleSync
        );

        registrar.playToServer(
                ToggleAllomanticBurnC2SPayload.TYPE,
                ToggleAllomanticBurnC2SPayload.STREAM_CODEC,
                AllomancyBurnManager::handleToggle
        );

        registrar.playToClient(
                SyncMetalSourcesS2CPayload.TYPE,
                SyncMetalSourcesS2CPayload.STREAM_CODEC,
                ClientMetalSources::handleSync
        );
    }

    private static void onLogin(
            PlayerEvent.PlayerLoggedInEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ScadrialPowerManager.reconcile(player);
            ExternalAllomancyPerception.forget(player);
            sync(player);
        }
    }

    private static void onRespawn(
            PlayerEvent.PlayerRespawnEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ScadrialPowerManager.reconcile(player);
            ExternalAllomancyPerception.forget(player);
            sync(player);
        }
    }

    private static void onChangeDimension(
            PlayerEvent.PlayerChangedDimensionEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ExternalAllomancyPerception.forget(player);
            sync(player);
        }
    }

    private static void onLogout(
            PlayerEvent.PlayerLoggedOutEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ExternalAllomancyPerception.forget(player);
            PhysicalInternalAllomancyNetworking.forget(player);
            LAST_STATE.remove(player.getUUID());
            LAST_BURNS.remove(player.getUUID());
        }
    }

    private static void onServerStopped(
            ServerStoppedEvent event
    ) {
        ExternalAllomancyPerception.clear();
        PhysicalInternalAllomancyNetworking.clear();
        LAST_STATE.clear();
        LAST_BURNS.clear();
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        AllomancyBurnManager.onPlayerTick(event);

        if (event.getEntity() instanceof ServerPlayer player) {
            PhysicalInternalAllomancyManager.tick(player);
        }

        ExternalAllomancyPerception.onPlayerTick(event);
    }

    public static void sync(ServerPlayer player) {
        PhysicalInternalAllomancyManager.refresh(player);
        PhysicalInternalAllomancyNetworking.forceSync(player);

        LAST_STATE.remove(player.getUUID());
        LAST_BURNS.remove(player.getUUID());
        syncAllomancy(player);

        ExternalAllomancyPerception.sync(player);
    }

    public static void syncAllomancy(ServerPlayer player) {
        ScadrialPlayerData data = ScadrialAttachments.get(player);
        UUID id = player.getUUID();

        SyncAllomancyStateS2CPayload state =
                new SyncAllomancyStateS2CPayload(
                        data.isPowerAssignmentComplete(),
                        data.getAllomanticProfile().getSerializedName(),
                        data.getAllomanticMetal()
                                .map(metal -> metal.getSerializedName())
                                .orElse(""),
                        data.isAllomancySnapped(),
                        data.isAllomancyStrengthInitialized()
                                ? data.getAllomancyStrength()
                                : 0.0,
                        data.getAllomanticReservesSubunits()
                );

        if (!state.equals(LAST_STATE.put(id, state))) {
            PacketDistributor.sendToPlayer(player, state);
        }

        SyncAllomanticBurnsS2CPayload burns =
                new SyncAllomanticBurnsS2CPayload(
                        data.getBurningFuels().stream()
                                .map(AllomanticFuel::getSerializedName)
                                .toList()
                );

        if (!burns.equals(LAST_BURNS.put(id, burns))) {
            PacketDistributor.sendToPlayer(player, burns);
        }
    }
}