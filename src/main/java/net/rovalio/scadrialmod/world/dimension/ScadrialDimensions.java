package net.rovalio.scadrialmod.world.dimension;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.rovalio.scadrialmod.ScadrialMod;

public final class ScadrialDimensions {

    public static final ResourceKey<Level> SCADRIAL =
            ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath(
                            ScadrialMod.MOD_ID,
                            "scadrial"
                    )
            );

    private ScadrialDimensions() {
    }
}