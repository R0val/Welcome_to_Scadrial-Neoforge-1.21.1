package net.rovalio.scadrialmod.power.allomancy.temporal.external;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.rovalio.scadrialmod.ScadrialMod;

public final class TemporalPlayerEffects {

    private static final ResourceLocation MODIFIER =
            ResourceLocation.fromNamespaceAndPath(
                    ScadrialMod.MOD_ID,
                    "temporal_rate"
            );

    private TemporalPlayerEffects() {}

    public static void update(ServerPlayer player, double rate){
        modifier(player, Attributes.MOVEMENT_SPEED, rate - 1.0);
        modifier(player, Attributes.ATTACK_SPEED, rate - 1.0);
        modifier(player, Attributes.BLOCK_BREAK_SPEED, rate - 1.0);
    }

    private static void modifier(
            ServerPlayer player,
            Holder<Attribute> attribute,
            double amount
    ){
        var instance = player.getAttribute(attribute);

        if (instance == null){
            return;
        }

        var previous = instance.getModifier(MODIFIER);

        if (previous != null && previous.amount() == amount){
            return;
        }

        if (previous == null && amount == 0.0) {
            return;
        }

        instance.removeModifier(MODIFIER);

        if (amount != 0.0){
            instance.addTransientModifier(
                    new  AttributeModifier(
                            MODIFIER,
                            amount,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    )
            );
        }
    }
}
