package net.rovalio.scadrialmod.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.rovalio.scadrialmod.power.allomancy.temporal.external.TemporalAllomancyMath;
import net.rovalio.scadrialmod.power.allomancy.temporal.external.TemporalField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Level.class)
public abstract class TemporalLevelMixin
        implements TemporalField.Access {

    @Unique
    private final TemporalField scadrial$temporalField =
            new TemporalField();

    @Override
    public TemporalField scadrial$temporalField() {
        return scadrial$temporalField;
    }

    @Redirect(
            method = "tickBlockEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"
            )
    )
    private void scadrial$tickBlockEntity(
            TickingBlockEntity ticker
    ) {
        Level level = (Level) (Object) this;

        double rate = scadrial$temporalField.rate(
                Vec3.atCenterOf(ticker.getPos())
        );

        int steps = TemporalAllomancyMath.scheduledSteps(
                rate,
                level.getGameTime(),
                ticker.getPos().hashCode()
        );

        for (int i = 0; i < steps && !ticker.isRemoved(); i++) {
            ticker.tick();
        }
    }
}