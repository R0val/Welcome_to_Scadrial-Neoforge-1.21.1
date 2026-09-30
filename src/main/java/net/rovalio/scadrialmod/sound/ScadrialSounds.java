package net.rovalio.scadrialmod.sound;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rovalio.scadrialmod.ScadrialMod;

public final class ScadrialSounds {

    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(
                    Registries.SOUND_EVENT,
                    ScadrialMod.MOD_ID
            );

    public static final DeferredHolder<SoundEvent, SoundEvent> PUSH =
            sound("push");

    public static final DeferredHolder<SoundEvent, SoundEvent> PULL =
            sound("pull");

    public static final DeferredHolder<SoundEvent, SoundEvent> COINSHOT =
            sound("coinshot");

    public static final DeferredHolder<SoundEvent, SoundEvent> COIN_COLLISION =
            sound("coin_collision");

    private ScadrialSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(
                name,
                () -> SoundEvent.createVariableRangeEvent(
                        ResourceLocation.fromNamespaceAndPath(
                                ScadrialMod.MOD_ID,
                                name
                        )
                )
        );
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}