package net.rovalio.scadrialmod.commands;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.rovalio.scadrialmod.player.ScadrialAttachments;
import net.rovalio.scadrialmod.player.ScadrialPlayerData;
import net.rovalio.scadrialmod.power.MetalType;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

final class ScadrialPowerDisplay {

    private ScadrialPowerDisplay() {
    }

    /// # Warning
    /// I have no fucking idea of what I did here. I tried to copypaste my code from Forge but it didn't run
    /// because the first draft of this project was garbage.
    /// This class was done with 37 hours without sleeping and the company of a cheap shitty coffee.
    /// Colaborators? My insanity, neoforge documentation, what i think that I understood from my previous
    /// monolitic version of this shit and a bit of help from chatgpt. Don't blame me, I was desperate
    ///
    /// Kelsier cabrón
    ///
    /// Efectively (No tengo ni puta idea de cómo se dice efectivamente en inglés), he hecho una clase entera
    /// just to put colours to the powers. Surely it doesn't even look well but after all I don`t know ball
    ///
    /// Debería irme a dormir

    static void send(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        ScadrialPlayerData data =
                ScadrialAttachments.get(player);

        sendLine(
                source,
                Component.literal(
                                "------ SCADRIAL POWERS: "
                        )
                        .withStyle(
                                ChatFormatting.DARK_BLUE
                        )
                        .append(
                                Component.literal("SCADRIAL POWERS: ")
                                        .withStyle(
                                              ChatFormatting.WHITE
                                        )
                        )
                        .append(
                                Component.literal(
                                                player.getScoreboardName()
                                        )
                                        .withStyle(
                                                ChatFormatting.GOLD
                                        )
                        )
                        .append(
                                Component.literal(" ------")
                                        .withStyle(
                                                ChatFormatting.DARK_BLUE
                                        )
                        )
        );

        sendLine(
                source,
                labeledLine(
                        "Classification: ",
                        createClassification(data)
                )
        );

        sendLine(
                source,
                labeledLine(
                        "Allomancy: ",
                        createAllomancyDescription(data)
                )
        );

        sendLine(
                source,
                labeledLine(
                        "Feruchemy: ",
                        createFeruchemyDescription(data)
                )
        );

        if (data.isCompounder()) {
            sendLine(
                    source,
                    labeledLine(
                            "Compounding: ",
                            createCompoundingDescription(data)
                    )
            );
        }

        sendLine(
                source,
                Component.literal(
                                "--------------------------------"
                        )
                        .withStyle(
                                ChatFormatting.DARK_BLUE
                        )
        );
    }

    private static Component createClassification(
            ScadrialPlayerData data
    ) {
        if (data.isFullborn()) {
            return Component.literal("Fullborn")
                    .withStyle(
                            ChatFormatting.LIGHT_PURPLE
                    );
        }

        if (data.isTwinborn()) {
            MutableComponent classification =
                    Component.literal("Twinborn")
                            .withStyle(
                                    ChatFormatting.AQUA
                            );

            if (data.isCompounder()) {
                classification.append(
                        Component.literal(
                                        " / Compounder"
                                )
                                .withStyle(
                                        ChatFormatting.GREEN
                                )
                );
            }

            return classification;
        }

        if (data.isMistborn()) {
            return Component.literal("Mistborn")
                    .withStyle(
                            ChatFormatting.AQUA
                    );
        }

        if (data.isMisting()) {
            return Component.literal("Misting")
                    .withStyle(
                            ChatFormatting.BLUE
                    );
        }

        if (data.isFeruchemist()) {
            return Component.literal("Feruchemist")
                    .withStyle(
                            ChatFormatting.GOLD
                    );
        }

        if (data.isFerring()) {
            return Component.literal("Ferring")
                    .withStyle(
                            ChatFormatting.YELLOW
                    );
        }

        return Component.literal(
                        "No innate Metallic powers"
                )
                .withStyle(
                        ChatFormatting.DARK_GRAY
                );
    }

    private static Component createAllomancyDescription(
            ScadrialPlayerData data
    ) {
        return switch (data.getAllomanticProfile()) {

            case NONE ->
                    Component.literal("None")
                            .withStyle(
                                    ChatFormatting.DARK_GRAY
                            );

            case FULL ->
                    appendSnappingState(
                            Component.literal("Mistborn")
                                    .withStyle(
                                            ChatFormatting.AQUA
                                    ),
                            data
                    );

            case SINGLE -> {
                MetalType metal =
                        data.getAllomanticMetal()
                                .orElseThrow(() ->
                                        new IllegalStateException(
                                                "A Misting profile "
                                                        + "requires a metal"
                                        )
                                );

                yield appendSnappingState(
                        createMetalDescription(
                                metal.getAllomanticTitle(),
                                getAllomanticColor(metal),
                                capitalizeMetal(metal)
                                        + " Misting"
                        ),
                        data
                );
            }
        };
    }

    private static Component createFeruchemyDescription(
            ScadrialPlayerData data
    ) {
        return switch (data.getFeruchemicalProfile()) {

            case NONE ->
                    Component.literal("None")
                            .withStyle(
                                    ChatFormatting.DARK_GRAY
                            );

            case FULL ->
                    Component.literal("Feruchemist")
                            .withStyle(
                                    ChatFormatting.GOLD
                            );

            case SINGLE -> {
                MetalType metal =
                        data.getFeruchemicalMetal()
                                .orElseThrow(() ->
                                        new IllegalStateException(
                                                "A Ferring profile "
                                                        + "requires a metal"
                                        )
                                );

                yield createMetalDescription(
                        metal.getFeruchemicalTitle(),
                        getFeruchemicalColor(metal),
                        capitalizeMetal(metal)
                                + " Ferring"
                );
            }
        };
    }

    private static MutableComponent
    createMetalDescription(
            String canonicalTitle,
            ChatFormatting color,
            String genericProfile
    ) {
        MutableComponent description =
                Component.literal(
                                canonicalTitle
                        )
                        .withStyle(color);

        if (!canonicalTitle.equals(
                genericProfile
        )) {
            description.append(
                    Component.literal(
                                    " ("
                                            + genericProfile
                                            + ")"
                            )
                            .withStyle(
                                    ChatFormatting.GRAY
                            )
            );
        }

        return description;
    }

    private static Component appendSnappingState(
            MutableComponent description,
            ScadrialPlayerData data
    ) {
        description.append(
                Component.literal(" - ")
                        .withStyle(
                                ChatFormatting.DARK_GRAY
                        )
        );

        if (!data.isAllomancySnapped()) {
            return description.append(
                    Component.literal("Latent")
                            .withStyle(
                                    ChatFormatting.YELLOW
                            )
            );
        }

        description.append(
                Component.literal("Snapped")
                        .withStyle(
                                ChatFormatting.GREEN
                        )
        );

        double damage =
                data.getAllomancySnappingDamage();

        if (Double.isFinite(damage)
                && damage > 0.0) {

            description.append(
                    Component.literal(
                                    String.format(
                                            Locale.ROOT,
                                            " (-%.2f%% Integrity)",
                                            damage * 100.0
                                    )
                            )
                            .withStyle(
                                    ChatFormatting.DARK_GRAY
                            )
            );
        }

        return description;
    }

    private static Component
    createCompoundingDescription(
            ScadrialPlayerData data
    ) {
        Set<MetalType> compoundableMetals =
                data.getCompoundableMetals();

        String description;

        if (compoundableMetals.equals(
                MetalType.getStandardMetals()
        )) {
            description =
                    "All sixteen standard metals";

        } else {
            description =
                    compoundableMetals.stream()
                            .map(
                                    ScadrialPowerDisplay
                                            ::capitalizeMetal
                            )
                            .sorted()
                            .collect(
                                    Collectors.joining(", ")
                            );
        }

        return Component.literal(description)
                .withStyle(
                        ChatFormatting.GREEN
                );
    }

    private static ChatFormatting
    getAllomanticColor(
            MetalType metal
    ) {
        return switch (metal) {

            case IRON, STEEL ->
                    ChatFormatting.BLUE;

            case TIN, PEWTER ->
                    ChatFormatting.RED;

            case ZINC, BRASS ->
                    ChatFormatting.DARK_RED;

            case COPPER, BRONZE ->
                    ChatFormatting.YELLOW;

            case CHROMIUM, NICROSIL ->
                    ChatFormatting.DARK_GRAY;

            case ALUMINUM, DURALUMIN ->
                    ChatFormatting.GRAY;

            case CADMIUM, BENDALLOY ->
                    ChatFormatting.DARK_AQUA;

            case GOLD, ELECTRUM ->
                    ChatFormatting.GOLD;

            case ATIUM ->
                    ChatFormatting.WHITE;
        };
    }

    /// Ahora que lo pienso, en verdad lo de que no sé qué coño he hecho aplicaría a todo, sabes. No sé ni siquiera
    /// como funciona del todo la api ahora mismo, pero bueno

    private static ChatFormatting
    getFeruchemicalColor(
            MetalType metal
    ) {
        return switch (metal) {

            case IRON, STEEL ->
                    ChatFormatting.BLUE;

            case TIN, PEWTER ->
                    ChatFormatting.RED;

            case ZINC, BRASS ->
                    ChatFormatting.DARK_RED;

            case COPPER, BRONZE ->
                    ChatFormatting.YELLOW;

            case CHROMIUM, NICROSIL ->
                    ChatFormatting.DARK_GRAY;

            case ALUMINUM, DURALUMIN ->
                    ChatFormatting.GRAY;

            case CADMIUM, BENDALLOY ->
                    ChatFormatting.DARK_AQUA;

            case GOLD, ELECTRUM ->
                    ChatFormatting.GOLD;

            case ATIUM ->
                    ChatFormatting.WHITE;
        };
    }

    private static String capitalizeMetal(
            MetalType metal
    ) {
        String serializedName =
                metal.getSerializedName();

        return Character.toUpperCase(
                serializedName.charAt(0)
        ) + serializedName.substring(1);
    }

    private static MutableComponent labeledLine(
            String label,
            Component value
    ) {
        return Component.literal(label)
                .withStyle(
                        ChatFormatting.GRAY
                )
                .append(value);
    }

    private static void sendLine(
            CommandSourceStack source,
            Component line
    ) {
        source.sendSuccess(
                () -> line,
                false
        );
    }
}