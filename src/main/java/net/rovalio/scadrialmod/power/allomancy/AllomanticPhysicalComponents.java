package net.rovalio.scadrialmod.power.allomancy;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.rovalio.scadrialmod.ScadrialMod;

@EventBusSubscriber(
        modid = ScadrialMod.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD
)
public final class AllomanticPhysicalComponents {

    // Valores por unidad: dividir un stack conserva la masa y la carga totales.
    public static final DataComponentType<Double> MASS =
            DataComponentType.<Double>builder()
                    .persistent(Codec.doubleRange(0.0001, 1.0E8))
                    .build();

    public static final DataComponentType<Double> INVESTED_CHARGE =
            DataComponentType.<Double>builder()
                    .persistent(Codec.doubleRange(0.0, 1.0E8))
                    .build();

    private AllomanticPhysicalComponents() {
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(
                Registries.DATA_COMPONENT_TYPE,
                helper -> {
                    helper.register(id("allomantic_mass"), MASS);
                    helper.register(id("invested_charge"), INVESTED_CHARGE);
                }
        );
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ScadrialMod.MOD_ID, path);
    }
}