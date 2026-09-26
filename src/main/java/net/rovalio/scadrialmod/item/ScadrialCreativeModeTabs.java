package net.rovalio.scadrialmod.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rovalio.scadrialmod.ScadrialMod;

import java.util.function.Supplier;

public class ScadrialCreativeModeTabs {
    public static DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ScadrialMod.MOD_ID);

    /// # METALURGY AND METALLIC ARTS ITEMS
    public static final Supplier<CreativeModeTab> METALLIC_ARTS = CREATIVE_MODE_TAB.register("metallic_arts_item",
            ()-> CreativeModeTab.builder().icon(() -> new ItemStack(ScadrialItems.ATIUM_BEAD.get()))
                    .title(Component.translatable("creativetab.welcome_to_scadrial.metallic_arts"))
                    .displayItems((itemDisplayParameters, output) -> {

                        //IRON
                        output.accept(ScadrialItems.IRON_DUST);
                        output.accept(ScadrialItems.IRON_SHAVINGS);
                        output.accept(ScadrialItems.IRON_BEAD);

                        output.accept(Items.IRON_NUGGET);
                        output.accept(Items.IRON_INGOT);
                        output.accept(Items.IRON_BLOCK);

                        //STEEL
                        output.accept(ScadrialItems.STEEL_DUST);
                        output.accept(ScadrialItems.STEEL_SHAVINGS);
                        output.accept(ScadrialItems.STEEL_BEAD);

                        output.accept(ScadrialItems.STEEL_NUGGET);
                        output.accept(ScadrialItems.STEEL_INGOT);
                        output.accept(ScadrialItems.STEEL_BLOCK);

                        //TIN
                        output.accept(ScadrialItems.TIN_DUST);
                        output.accept(ScadrialItems.TIN_SHAVINGS);
                        output.accept(ScadrialItems.TIN_BEAD);

                        output.accept(ScadrialItems.TIN_NUGGET);
                        output.accept(ScadrialItems.TIN_INGOT);
                        output.accept(ScadrialItems.TIN_BLOCK);

                        //PEWTER
                        output.accept(ScadrialItems.PEWTER_DUST);
                        output.accept(ScadrialItems.PEWTER_SHAVINGS);
                        output.accept(ScadrialItems.PEWTER_BEAD);

                        output.accept(ScadrialItems.PEWTER_NUGGET);
                        output.accept(ScadrialItems.PEWTER_INGOT);
                        output.accept(ScadrialItems.PEWTER_BLOCK);

                        //ZINC
                        output.accept(ScadrialItems.ZINC_DUST);
                        output.accept(ScadrialItems.ZINC_SHAVINGS);
                        output.accept(ScadrialItems.ZINC_BEAD);

                        output.accept(ScadrialItems.ZINC_NUGGET);
                        output.accept(ScadrialItems.ZINC_INGOT);
                        output.accept(ScadrialItems.ZINC_BLOCK);

                        //BRASS
                        output.accept(ScadrialItems.BRASS_DUST);
                        output.accept(ScadrialItems.BRASS_SHAVINGS);
                        output.accept(ScadrialItems.BRASS_BEAD);

                        output.accept(ScadrialItems.BRASS_NUGGET);
                        output.accept(ScadrialItems.BRASS_INGOT);
                        output.accept(ScadrialItems.BRASS_BLOCK);

                        //COPPER
                        output.accept(ScadrialItems.COPPER_DUST);
                        output.accept(ScadrialItems.COPPER_SHAVINGS);
                        output.accept(ScadrialItems.COPPER_BEAD);

                        output.accept(ScadrialItems.COPPER_NUGGET);
                        output.accept(Items.COPPER_INGOT);
                        output.accept(Items.COPPER_BLOCK);

                        //BRONZE
                        output.accept(ScadrialItems.BRONZE_DUST);
                        output.accept(ScadrialItems.BRONZE_SHAVINGS);
                        output.accept(ScadrialItems.BRASS_BEAD);

                        //CHROMIUM
                        output.accept(ScadrialItems.CHROMIUM_DUST);
                        output.accept(ScadrialItems.CHROMIUM_SHAVINGS);
                        output.accept(ScadrialItems.CHROMIUM_BEAD);

                        output.accept(ScadrialItems.CHROMIUM_NUGGET);
                        output.accept(ScadrialItems.CHROMIUM_INGOT);
                        output.accept(ScadrialItems.CHROMIUM_BLOCK);

                        //NICROSIL
                        output.accept(ScadrialItems.NICROSIL_DUST);
                        output.accept(ScadrialItems.NICROSIL_SHAVINGS);
                        output.accept(ScadrialItems.NICROSIL_BEAD);

                        output.accept(ScadrialItems.NICROSIL_NUGGET);
                        output.accept(ScadrialItems.NICROSIL_INGOT);
                        output.accept(ScadrialItems.NICROSIL_BLOCK);

                        //ALUMINIUM
                        output.accept(ScadrialItems.ALUMINIUM_DUST);
                        output.accept(ScadrialItems.ALUMINIUM_SHAVINGS);
                        output.accept(ScadrialItems.ALUMINIUM_BEAD);

                        output.accept(ScadrialItems.ALUMINIUM_NUGGET);
                        output.accept(ScadrialItems.ALUMINIUM_INGOT);
                        output.accept(ScadrialItems.ALUMINIUM_BLOCK);

                        //DURALUMIN
                        output.accept(ScadrialItems.DURALUMIN_DUST);
                        output.accept(ScadrialItems.DURALUMIN_SHAVINGS);
                        output.accept(ScadrialItems.DURALUMIN_BEAD);

                        output.accept(ScadrialItems.DURALUMIN_NUGGET);
                        output.accept(ScadrialItems.DURALUMIN_INGOT);
                        output.accept(ScadrialItems.DURALUMIN_BLOCK);

                        //CADMIUM
                        output.accept(ScadrialItems.CADMIUM_DUST);
                        output.accept(ScadrialItems.CADMIUM_SHAVINGS);
                        output.accept(ScadrialItems.CADMIUM_BEAD);

                        output.accept(ScadrialItems.CADMIUM_NUGGET);
                        output.accept(ScadrialItems.CADMIUM_INGOT);
                        output.accept(ScadrialItems.CADMIUM_BLOCK);

                        //BENDALLOY
                        output.accept(ScadrialItems.BENDALLOY_DUST);
                        output.accept(ScadrialItems.BENDALLOY_SHAVINGS);
                        output.accept(ScadrialItems.BENDALLOY_BEAD);

                        output.accept(ScadrialItems.BENDALLOY_NUGGET);
                        output.accept(ScadrialItems.BENDALLOY_INGOT);
                        output.accept(ScadrialItems.BENDALLOY_BLOCK);

                        //GOLD
                        output.accept(ScadrialItems.GOLD_DUST);
                        output.accept(ScadrialItems.GOLD_SHAVINGS);
                        output.accept(ScadrialItems.GOLD_BEAD);

                        output.accept(Items.GOLD_NUGGET);
                        output.accept(Items.GOLD_INGOT);
                        output.accept(Items.GOLD_BLOCK);

                        //ELECTRUM
                        output.accept(ScadrialItems.ELECTRUM_DUST);
                        output.accept(ScadrialItems.ELECTRUM_SHAVINGS);
                        output.accept(ScadrialItems.ELECTRUM_BEAD);

                        output.accept(ScadrialItems.ELECTRUM_NUGGET);
                        output.accept(ScadrialItems.ELECTRUM_INGOT);
                        output.accept(ScadrialItems.ELECTRUM_BLOCK);

                        //ATIUM
                        output.accept(ScadrialItems.ATIUM_DUST);
                        output.accept(ScadrialItems.ATIUM_SHAVINGS);
                        output.accept(ScadrialItems.ATIUM_BEAD);

                        output.accept(ScadrialItems.ATIUM_NUGGET);
                        output.accept(ScadrialItems.ATIUM_INGOT);
                        output.accept(ScadrialItems.ATIUM_BLOCK);

                        //LERASIUM
                        output.accept(ScadrialItems.LERASIUM_DUST);
                        output.accept(ScadrialItems.LERASIUM_SHAVINGS);
                        output.accept(ScadrialItems.LERASIUM_BEAD);

                        output.accept(ScadrialItems.LERASIUM_NUGGET);
                        output.accept(ScadrialItems.LERASIUM_INGOT);
                        output.accept(ScadrialItems.LERASIUM_BLOCK);

                        //ARMONIUM
                        output.accept(ScadrialItems.ARMONIUM_DUST);
                        output.accept(ScadrialItems.ARMONIUM_SHAVINGS);
                        output.accept(ScadrialItems.ARMONIUM_BEAD);

                        output.accept(ScadrialItems.ARMONIUM_NUGGET);
                        output.accept(ScadrialItems.ARMONIUM_INGOT);
                        output.accept(ScadrialItems.ARMONIUM_BLOCK);

                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }

}
