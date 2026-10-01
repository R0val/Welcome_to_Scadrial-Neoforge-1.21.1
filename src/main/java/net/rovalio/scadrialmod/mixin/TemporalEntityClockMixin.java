package net.rovalio.scadrialmod.mixin;

import net.minecraft.world.entity.Entity;
import net.rovalio.scadrialmod.power.allomancy.temporal.external.TemporalEntityTicks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class TemporalEntityClockMixin
        implements TemporalEntityTicks.Clock {

    @Unique
    private double scadrial$temporalRemainder;

    @Unique
    public int scadrial$temporalSteps(double rate){
        if (rate == 1.0){
            scadrial$temporalRemainder = 0.0;
            return 1;
        }

        scadrial$temporalRemainder += rate;

        int steps = (int) scadrial$temporalRemainder;;

        scadrial$temporalRemainder -= steps;

        return steps;
    }
}
