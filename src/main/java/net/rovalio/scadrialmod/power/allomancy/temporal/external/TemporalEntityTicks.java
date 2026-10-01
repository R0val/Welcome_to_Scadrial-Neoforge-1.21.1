package net.rovalio.scadrialmod.power.allomancy.temporal.external;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class TemporalEntityTicks {

    public interface Clock{
        int scadrial$temporalSteps(double rate);
    }

    private TemporalEntityTicks(){}

    public static boolean controlled(Entity entity) {
        if (entity instanceof Player){
            return false;
        }

        for (Entity passenger : entity.getIndirectPassengers()){
            if (passenger instanceof Player){
                return false;
            }
        }

        TemporalField field = TemporalField.of(entity.level());

        if (field.bubbles().isEmpty()) {
            ((Clock) entity).scadrial$temporalSteps(1.0);
            return false;
        }

        return true;
    }

    public static void tick(
            Entity entity,
            Runnable vanillaTick
    ){
        double rate = TemporalField.of(entity.level()).rate(entity);

        int steps = ((Clock) entity).scadrial$temporalSteps(rate);

        if (steps <= 0){
            entity.setOldPosAndRot();
        }

        for (int i = 0; i < steps && !entity.isRemoved(); i++){
            TemporalProjectiles.beforeTick(entity);
            vanillaTick.run();
        }
    }
}
