package net.rovalio.scadrialmod.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.rovalio.CosmereAPI.commands.InvestedArtCommandRegistry;
import net.rovalio.CosmereAPI.player.CosmereAttachments;
import net.rovalio.CosmereAPI.player.CosmerePlayerData;
import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.power.MetalType;
import net.rovalio.scadrialmod.power.ScadrialPowerAssigner;
import net.rovalio.scadrialmod.power.ScadrialPowerManager;
import net.rovalio.scadrialmod.registry.ScadrialInvestedArts;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class ScadrialInvestedArtCommands {

    private static final ResourceLocation
            POWERS_SCOPE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    ScadrialMod.MOD_ID,
                    "powers"
            );

    private static final List<String>
            METAL_SUGGESTIONS =
            Arrays.stream(MetalType.values())
                    .map(
                            MetalType::getSerializedName
                    )
                    .sorted()
                    .toList();

    private ScadrialInvestedArtCommands() {
    }

    public static void register() {

        registerAllomancyConfiguration();
        registerFeruchemyConfiguration();

        InvestedArtCommandRegistry.registerAction(
                ScadrialInvestedArts
                        .ALLOMANCY
                        .getId(),
                ScadrialInvestedArtCommands
                        ::configureAllomancyActions
        );

        InvestedArtCommandRegistry.registerAction(
                POWERS_SCOPE_ID,
                ScadrialInvestedArtCommands
                        ::configurePowerActions
        );

        InvestedArtCommandRegistry.registerInfo(
                POWERS_SCOPE_ID,
                ScadrialInvestedArtCommands
                        ::configurePowerInfo
        );
    }

    private static void registerAllomancyConfiguration() {
        InvestedArtCommandRegistry.register(
                ScadrialInvestedArts
                        .ALLOMANCY
                        .getId(),

                new InvestedArtCommandRegistry
                        .CommandExtension() {

                    @Override
                    public void configureGrant(
                            LiteralArgumentBuilder<
                                    CommandSourceStack
                                    > branch
                    ) {
                        configureAllomancyGrant(
                                branch
                        );
                    }

                    @Override
                    public void configureRevoke(
                            LiteralArgumentBuilder<
                                    CommandSourceStack
                                    > branch
                    ) {
                        branch.executes(
                                ScadrialInvestedArtCommands
                                        ::revokeAllomancy
                        );
                    }
                }
        );
    }

    private static void registerFeruchemyConfiguration() {
        InvestedArtCommandRegistry.register(
                ScadrialInvestedArts
                        .FERUCHEMY
                        .getId(),

                new InvestedArtCommandRegistry
                        .CommandExtension() {

                    @Override
                    public void configureGrant(
                            LiteralArgumentBuilder<
                                    CommandSourceStack
                                    > branch
                    ) {
                        configureFeruchemyGrant(
                                branch
                        );
                    }

                    @Override
                    public void configureRevoke(
                            LiteralArgumentBuilder<
                                    CommandSourceStack
                                    > branch
                    ) {
                        branch.executes(
                                ScadrialInvestedArtCommands
                                        ::revokeFeruchemy
                        );
                    }
                }
        );
    }

    private static void configureAllomancyGrant(
            LiteralArgumentBuilder<
                    CommandSourceStack
                    > branch
    ) {
        branch.then(
                Commands.literal("misting")

                        .then(
                                Commands.argument(
                                                "metal",
                                                StringArgumentType.word()
                                        )

                                        .suggests(
                                                (context, builder) ->
                                                        SharedSuggestionProvider
                                                                .suggest(
                                                                        METAL_SUGGESTIONS,
                                                                        builder
                                                                )
                                        )

                                        .executes(
                                                ScadrialInvestedArtCommands
                                                        ::grantMisting
                                        )
                        )
        );

        branch.then(
                Commands.literal("mistborn")

                        .executes(
                                ScadrialInvestedArtCommands
                                        ::grantMistborn
                        )
        );
    }

    private static void configureFeruchemyGrant(
            LiteralArgumentBuilder<
                    CommandSourceStack
                    > branch
    ) {
        branch.then(
                Commands.literal("ferring")

                        .then(
                                Commands.argument(
                                                "metal",
                                                StringArgumentType.word()
                                        )

                                        .suggests(
                                                (context, builder) ->
                                                        SharedSuggestionProvider
                                                                .suggest(
                                                                        METAL_SUGGESTIONS,
                                                                        builder
                                                                )
                                        )

                                        .executes(
                                                ScadrialInvestedArtCommands
                                                        ::grantFerring
                                        )
                        )
        );

        branch.then(
                Commands.literal("feruchemist")

                        .executes(
                                ScadrialInvestedArtCommands
                                        ::grantFeruchemist
                        )
        );
    }

    private static void configureAllomancyActions(
            LiteralArgumentBuilder<
                    CommandSourceStack
                    > branch
    ) {
        branch.then(
                Commands.literal("snap")

                        .executes(
                                ScadrialInvestedArtCommands
                                        ::snapAllomancy
                        )
        );
    }

    private static void configurePowerActions(
            LiteralArgumentBuilder<
                    CommandSourceStack
                    > branch
    ) {
        branch.then(
                Commands.literal("reroll")

                        .executes(
                                ScadrialInvestedArtCommands
                                        ::rerollPowers
                        )
        );
    }

    private static void configurePowerInfo(
            LiteralArgumentBuilder<
                    CommandSourceStack
                    > branch
    ) {
        branch.executes(
                ScadrialInvestedArtCommands
                        ::showPowerInfo
        );
    }

    private static int grantMisting(
            CommandContext<CommandSourceStack>
                    context
    ) throws CommandSyntaxException {

        MetalType metal =
                getMetal(context);

        if (metal == null) {
            return 0;
        }

        Collection<ServerPlayer> targets =
                getTargets(context);

        for (ServerPlayer player : targets) {
            ScadrialPowerManager.grantMisting(
                    player,
                    metal
            );
        }

        sendSuccess(
                context.getSource(),
                targets.size(),
                "Misting profile configured: "
                        + metal.getSerializedName()
        );

        return targets.size();
    }

    private static int grantMistborn(
            CommandContext<CommandSourceStack>
                    context
    ) throws CommandSyntaxException {

        Collection<ServerPlayer> targets =
                getTargets(context);

        for (ServerPlayer player : targets) {
            ScadrialPowerManager.grantMistborn(
                    player
            );
        }

        sendSuccess(
                context.getSource(),
                targets.size(),
                "Mistborn profile configured"
        );

        return targets.size();
    }

    private static int snapAllomancy(
            CommandContext<CommandSourceStack>
                    context
    ) throws CommandSyntaxException {

        Collection<ServerPlayer> targets =
                getTargets(context);

        int affectedPlayers = 0;

        for (ServerPlayer player : targets) {

            if (ScadrialPowerManager
                    .snapAllomancy(player)) {

                affectedPlayers++;
            }
        }

        if (affectedPlayers == 0) {
            context.getSource()
                    .sendFailure(
                            Component.literal(
                                    "No selected player could Snap"
                            )
                    );

            return 0;
        }

        sendSuccess(
                context.getSource(),
                affectedPlayers,
                "Allomancy Snapped"
        );

        return affectedPlayers;
    }

    private static int rerollPowers(
            CommandContext<CommandSourceStack>
                    context
    ) throws CommandSyntaxException {

        Collection<ServerPlayer> targets =
                getTargets(context);

        int affectedPlayers = 0;
        int skippedPlayers = 0;

        for (ServerPlayer player : targets) {
            CosmerePlayerData cosmereData =
                    CosmereAttachments.get(player);

            ResourceLocation originId =
                    cosmereData.getOriginId();

            if (!cosmereData.isOnboardingComplete()
                    || !ScadrialPowerAssigner
                    .supportsOrigin(originId)) {

                skippedPlayers++;
                continue;
            }

            if (ScadrialPowerAssigner.rerollPowers(
                    player,
                    originId
            )) {
                affectedPlayers++;
            }
        }

        if (affectedPlayers == 0) {
            context.getSource()
                    .sendFailure(
                            Component.literal(
                                    "No selected player has a completed "
                                            + "supported Scadrian origin"
                            )
                    );

            return 0;
        }

        sendSuccess(
                context.getSource(),
                affectedPlayers,
                "Scadrian powers rerolled"
        );

        if (skippedPlayers > 0) {
            int skipped = skippedPlayers;

            context.getSource()
                    .sendSuccess(
                            () -> Component.literal(
                                            skipped
                                                    + " player(s) skipped: "
                                                    + "no supported completed "
                                                    + "Scadrian origin"
                                    )
                                    .withStyle(
                                            ChatFormatting.YELLOW
                                    ),
                            false
                    );
        }

        return affectedPlayers;
    }

    private static int grantFerring(
            CommandContext<CommandSourceStack>
                    context
    ) throws CommandSyntaxException {

        MetalType metal =
                getMetal(context);

        if (metal == null) {
            return 0;
        }

        Collection<ServerPlayer> targets =
                getTargets(context);

        for (ServerPlayer player : targets) {
            ScadrialPowerManager.grantFerring(
                    player,
                    metal
            );
        }

        sendSuccess(
                context.getSource(),
                targets.size(),
                "Ferring profile configured: "
                        + metal.getSerializedName()
        );

        return targets.size();
    }

    private static int grantFeruchemist(
            CommandContext<CommandSourceStack>
                    context
    ) throws CommandSyntaxException {

        Collection<ServerPlayer> targets =
                getTargets(context);

        for (ServerPlayer player : targets) {
            ScadrialPowerManager
                    .grantFeruchemist(player);
        }

        sendSuccess(
                context.getSource(),
                targets.size(),
                "Full Feruchemist profile configured"
        );

        return targets.size();
    }

    private static int revokeAllomancy(
            CommandContext<CommandSourceStack>
                    context
    ) throws CommandSyntaxException {

        Collection<ServerPlayer> targets =
                getTargets(context);

        int affectedPlayers = 0;

        for (ServerPlayer player : targets) {

            if (ScadrialPowerManager
                    .revokeAllomancy(player)) {

                affectedPlayers++;
            }
        }

        if (affectedPlayers == 0) {
            context.getSource()
                    .sendFailure(
                            Component.literal(
                                    "No selected player possessed Allomancy"
                            )
                    );

            return 0;
        }

        sendSuccess(
                context.getSource(),
                affectedPlayers,
                "Allomancy revoked"
        );

        return affectedPlayers;
    }

    private static int revokeFeruchemy(
            CommandContext<CommandSourceStack>
                    context
    ) throws CommandSyntaxException {

        Collection<ServerPlayer> targets =
                getTargets(context);

        int affectedPlayers = 0;

        for (ServerPlayer player : targets) {

            if (ScadrialPowerManager
                    .revokeFeruchemy(player)) {

                affectedPlayers++;
            }
        }

        if (affectedPlayers == 0) {
            context.getSource()
                    .sendFailure(
                            Component.literal(
                                    "No selected player possessed Feruchemy"
                            )
                    );

            return 0;
        }

        sendSuccess(
                context.getSource(),
                affectedPlayers,
                "Feruchemy revoked"
        );

        return affectedPlayers;
    }

    private static int showPowerInfo(
            CommandContext<CommandSourceStack>
                    context
    ) throws CommandSyntaxException {

        Collection<ServerPlayer> targets =
                getTargets(context);

        for (ServerPlayer player : targets) {
            ScadrialPowerDisplay.send(
                    context.getSource(),
                    player
            );
        }

        return targets.size();
    }

    private static MetalType getMetal(
            CommandContext<CommandSourceStack>
                    context
    ) {
        String metalName =
                StringArgumentType.getString(
                        context,
                        "metal"
                );

        MetalType metal =
                MetalType.fromSerializedName(
                        metalName
                ).orElse(null);

        if (metal == null) {
            context.getSource()
                    .sendFailure(
                            Component.literal(
                                    "Unknown metal: "
                                            + metalName
                            )
                    );
        }

        return metal;
    }

    private static Collection<ServerPlayer>
    getTargets(
            CommandContext<CommandSourceStack>
                    context
    ) throws CommandSyntaxException {

        return EntityArgument.getPlayers(
                context,
                "targets"
        );
    }

    private static void sendSuccess(
            CommandSourceStack source,
            int affectedPlayers,
            String action
    ) {
        source.sendSuccess(
                () -> Component.literal(
                        action
                                + " for "
                                + affectedPlayers
                                + " player(s)"
                ),
                true
        );
    }
}
