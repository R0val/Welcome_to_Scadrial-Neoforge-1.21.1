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

public final class ScadrialNetworking {

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
        PayloadRegistrar registrar = event.registrar("2");

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
        }
    }

    private static void onServerStopped(
            ServerStoppedEvent event
    ) {
        ExternalAllomancyPerception.clear();
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
        PhysicalInternalAllomancyNetworking.sync(player);

        ScadrialPlayerData data = ScadrialAttachments.get(player);

        PacketDistributor.sendToPlayer(
                player,
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
                )
        );

        PacketDistributor.sendToPlayer(
                player,
                new SyncAllomanticBurnsS2CPayload(
                        data.getBurningFuels().stream()
                                .map(AllomanticFuel::getSerializedName)
                                .toList()
                )
        );

        ExternalAllomancyPerception.sync(player);
    }
}