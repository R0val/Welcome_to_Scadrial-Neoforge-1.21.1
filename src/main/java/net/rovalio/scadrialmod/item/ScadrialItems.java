package net.rovalio.scadrialmod.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.item.custom.AllomanticContainerItem;

public final class ScadrialItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(
                    ScadrialMod.MOD_ID
            );

    /// # Metals
    ///
    /// Every metal used for Metallic Arts and alloys has six variants: **DUST**, **SHAVINGS**, **BEAD**,
    /// **NUGGET**, **INGOT** and **BLOCK**
    ///
    /// ---
    ///
    /// **DUST**, **SHAVINGS** and **BEAD** are the three available forms in which metals can be burned.
    ///
    /// **DUST** form allows the metal vial to contain more than 4 metals with the disadvantage that it
    /// otorgues less allomantic reserves. Vials filled with **DUST** are the fastest to drink
    ///
    /// **SHAVINGS** form only allows to have 1 to 4 different metals within the same vial.
    /// It otorgues the standard allomantic reserves. Vials filled with **SHAVINGS** have standard drinking time
    ///
    /// **BEAD** form can fill vials with only one metal. It otorgues the largest allomantic reserve.
    /// Vials filled with **BEADS** are the slowest to drink.
    ///
    ///
    /// **DUST** can be stored in sacks. Sacks can contain a lot of metal, and it is slower to consume than
    /// from a **DUST VIAL** but faster than a **SHAVINGS VIAL**. Sacks can only contain one metal and consuming
    /// metal from them may hurt Player
    ///
    /// **BEADS** can also be consumed individually. They are fastest to consume by this way,
    /// but can only be stacked by 16
    ///
    /// ---
    ///
    /// The sixteen metals are:
    ///
    /// **IRON, STEEL, TIN, PEWTER,**
    ///
    /// **TIN, BRASS, BRONZE, COPPER**
    ///
    /// **CHROMIUM, NICROSIL, ALUMINIUM, DURALUMIN**
    ///
    /// **BENDALLOY, CADMIUM, GOLD, ELECTRUM**
    ///

    public static final DeferredItem<Item> NU_METAL = ITEMS.register("nu_metal",
            ()-> new Item(new Item.Properties()));

    /// # PHYSICAL METALS: IRON, STEEL, TIN, PEWTER
    // IRON
    public static final DeferredItem<Item> IRON_DUST = ITEMS.register("iron_dust",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IRON_SHAVINGS = ITEMS.register( "iron_shavings",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IRON_BEAD = ITEMS.register("iron_bead",
            ()-> new Item(new Item.Properties()));

    // Iron is a vanilla metal, so it already has  nugget, ingot and block forms

    // STEEL
    public static final DeferredItem<Item> STEEL_DUST = ITEMS.register("steel_dust",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STEEL_SHAVINGS = ITEMS.register("steel_shavings",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STEEL_BEAD = ITEMS.register("steel_bead",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> STEEL_NUGGET = ITEMS.register("steel_nugget",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.register("steel_ingot",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STEEL_BLOCK = ITEMS.register("steel_block",
            ()-> new Item(new Item.Properties()));

    // TIN
    public static final DeferredItem<Item> TIN_DUST = ITEMS.register("tin_dust",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TIN_SHAVINGS = ITEMS.register("tin_shavings",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TIN_BEAD = ITEMS.register("tin_bead",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> TIN_NUGGET = ITEMS.register("tin_nugget",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TIN_INGOT = ITEMS.register("tin_ingot",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TIN_BLOCK = ITEMS.register("tin_block",
            ()-> new Item(new Item.Properties()));

    // PEWTER
    public static final DeferredItem<Item> PEWTER_DUST = ITEMS.register("pewter_dust",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PEWTER_SHAVINGS= ITEMS.register("pewter_shavings",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PEWTER_BEAD = ITEMS.register("pewter_beads",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> PEWTER_NUGGET = ITEMS.register("pewter_nugget",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PEWTER_INGOT = ITEMS.register("pewter_ingot",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PEWTER_BLOCK = ITEMS.register("pewter_block",
            ()-> new Item(new Item.Properties()));

    /// # MENTAL METALS: ZINC, BRASS, COPPER, BRONZE

    //ZINC
    public static final DeferredItem<Item> ZINC_DUST = ITEMS.register("zinc_dust",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ZINC_SHAVINGS = ITEMS.register("zinc_shavings",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ZINC_BEAD = ITEMS.register("zinc_bead",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ZINC_NUGGET = ITEMS.register("zinc_nugget",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ZINC_INGOT = ITEMS.register("zinc_ingot",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ZINC_BLOCK = ITEMS.register("zinc_block",
            ()-> new Item(new Item.Properties()));

    //BRASS
    public static final DeferredItem<Item> BRASS_DUST = ITEMS.register("brass_dust",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRASS_SHAVINGS = ITEMS.register("brass_shavings",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRASS_BEAD = ITEMS.register("brass_bead",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> BRASS_NUGGET = ITEMS.register("brass_nugget",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRASS_INGOT = ITEMS.register("brass_ingot",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRASS_BLOCK = ITEMS.register("brass_block",
            ()-> new Item(new Item.Properties()));

    //COPPER
    public static final DeferredItem<Item> COPPER_DUST = ITEMS.register("copper_dust",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_SHAVINGS = ITEMS.register("copper_shavings",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_BEAD = ITEMS.register("copper_bead",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> COPPER_NUGGET = ITEMS.register("copper_nugget",
            ()-> new Item(new Item.Properties()));

    //Copper is a vanilla item, but copper nugget was implemented in minecraft 1.21.9,
    // so it does not exist yet in this version

    //BRONZE
    public static final DeferredItem<Item> BRONZE_DUST = ITEMS.register("bronze_dust",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRONZE_SHAVINGS = ITEMS.register("bronze_shavings",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRONZE_BEAD = ITEMS.register("bronze_bead",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> BRONZE_NUGGET = ITEMS.register("bronze_nugget",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRONZE_INGOT = ITEMS.register("bronze_ingot",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRONZE_BLOCK = ITEMS.register("bronze_block",
            ()-> new Item(new Item.Properties()));

    /// # ENHANCEMENT METALS: CHROMIUM, NICROSIL, ALUMINIUM, DURALUMIN

    //CHROMIUM
    public static final DeferredItem<Item> CHROMIUM_DUST = ITEMS.register("chromium_dust",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CHROMIUM_SHAVINGS = ITEMS.register("chromium_shavings",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CHROMIUM_BEAD = ITEMS.register("chromium_bead",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> CHROMIUM_NUGGET = ITEMS.register("chromium_nugget",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CHROMIUM_INGOT = ITEMS.register("chromium_ingot",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CHROMIUM_BLOCK = ITEMS.register("chromium_block",
            ()-> new Item(new Item.Properties()));

    //NICROSIL
    public static final DeferredItem<Item> NICROSIL_DUST = ITEMS.register("nicrosil_dust",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NICROSIL_SHAVINGS = ITEMS.register("nicrosil_shavings",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NICROSIL_BEAD = ITEMS.register("nicrosil_bead",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> NICROSIL_NUGGET = ITEMS.register("nicrosil_nugget",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NICROSIL_INGOT = ITEMS.register("nicrosil_ingot",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NICROSIL_BLOCK =ITEMS.register("nicrosil_block",
            ()-> new Item(new Item.Properties()));

    //ALUMINIUM
    public static final DeferredItem<Item> ALUMINIUM_DUST = ITEMS.register("aluminium_dust",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ALUMINIUM_SHAVINGS = ITEMS.register("aluminium_shavings",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ALUMINIUM_BEAD = ITEMS.register("aluminium_bead",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ALUMINIUM_NUGGET = ITEMS.register("aluminium_nugget",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ALUMINIUM_INGOT = ITEMS.register("aluminium_ingot",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ALUMINIUM_BLOCK =ITEMS.register("aluminium_block",
            ()-> new Item(new Item.Properties()));

    //DURALUMIN
    public static final DeferredItem<Item> DURALUMIN_DUST = ITEMS.register("duralumin_dust",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DURALUMIN_SHAVINGS = ITEMS.register("duralumin_shavings",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DURALUMIN_BEAD = ITEMS.register("duralumin_bead",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> DURALUMIN_NUGGET = ITEMS.register("duralumin_nugget",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DURALUMIN_INGOT = ITEMS.register("duralumin_ingot",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DURALUMIN_BLOCK =ITEMS.register("duralumin_block",
            ()-> new Item(new Item.Properties()));

    /// # TEMPORAL METALS: CADMIUM, BENDALLOY, GOLD, ELECTRUM

    //CADMIUM
    public static final DeferredItem<Item> CADMIUM_DUST = ITEMS.register("cadmium_dust",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CADMIUM_SHAVINGS = ITEMS.register("cadmium_shavings",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CADMIUM_BEAD = ITEMS.register("cadmium_bead",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> CADMIUM_NUGGET = ITEMS.register("cadmium_nugget",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CADMIUM_INGOT = ITEMS.register("cadmium_ingot",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CADMIUM_BLOCK =ITEMS.register("cadmium_block",
            ()-> new Item(new Item.Properties()));

    //BENDALLOY
    public static final DeferredItem<Item> BENDALLOY_DUST = ITEMS.register("bendalloy_dust",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BENDALLOY_SHAVINGS = ITEMS.register("bendalloy_shavings",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BENDALLOY_BEAD = ITEMS.register("bendalloy_bead",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> BENDALLOY_NUGGET = ITEMS.register("bendalloy_nugget",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BENDALLOY_INGOT = ITEMS.register("bendalloy_ingot",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BENDALLOY_BLOCK =ITEMS.register("bendalloy_block",
            ()-> new Item(new Item.Properties()));

    //GOLD
    public static final DeferredItem<Item> GOLD_DUST = ITEMS.register("gold_dust",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GOLD_SHAVINGS = ITEMS.register("gold_shavings",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GOLD_BEAD = ITEMS.register("gold_bead",
            ()-> new Item(new Item.Properties()));

    //GOLD is a vanilla item, so it already has  nugget, ingot and block forms

    //ELECTRUM
    public static final DeferredItem<Item> ELECTRUM_DUST = ITEMS.register("electrum_dust",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ELECTRUM_SHAVINGS = ITEMS.register("electrum_shavings",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ELECTRUM_BEAD = ITEMS.register("electrum_bead",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ELECTRUM_NUGGET = ITEMS.register("electrum_nugget",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ELECTRUM_INGOT = ITEMS.register("electrum_ingot",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ELECTRUM_BLOCK =ITEMS.register("electrum_block",
            ()-> new Item(new Item.Properties()));

    /// # GOD METALS: ATIUM, LERASIUM, ARMONIUM

    //ATIUM
    public static final DeferredItem<Item> ATIUM_DUST = ITEMS.register("atium_dust",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ATIUM_SHAVINGS = ITEMS.register("atium_shavings",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ATIUM_BEAD = ITEMS.register("atium_bead",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ATIUM_NUGGET = ITEMS.register("atium_nugget",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ATIUM_INGOT = ITEMS.register("atium_ingot",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ATIUM_BLOCK =ITEMS.register("atium_block",
            ()-> new Item(new Item.Properties()));

    //LERASIUM
    public static final DeferredItem<Item> LERASIUM_DUST = ITEMS.register("lerasium_dust",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LERASIUM_SHAVINGS = ITEMS.register("lerasium_shavings",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LERASIUM_BEAD = ITEMS.register("lerasium_bead",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> LERASIUM_NUGGET = ITEMS.register("lerasium_nugget",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LERASIUM_INGOT = ITEMS.register("lerasium_ingot",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LERASIUM_BLOCK =ITEMS.register("lerasium_block",
            ()-> new Item(new Item.Properties()));

    //ARMONIUM
    public static final DeferredItem<Item> ARMONIUM_DUST = ITEMS.register("armonium_dust",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ARMONIUM_SHAVINGS = ITEMS.register("armonium_shavings",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ARMONIUM_BEAD = ITEMS.register("armonium_bead",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ARMONIUM_NUGGET = ITEMS.register("armonium_nugget",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ARMONIUM_INGOT = ITEMS.register("armonium_ingot",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ARMONIUM_BLOCK =ITEMS.register("armonium_block",
            ()-> new Item(new Item.Properties()));

/// # ALLOMANCY
///
/// There are four allomancy containers: **VIAL**, **FLASK** (with its aluminium variation),
/// **BOTTLE** and **BUNDLE**
///
/// These containers but bundle can be filled with water or alcohol. The fluid used to fill these recipients
/// define the speed you drink the metals. It's been proposed recipients not filled with alcohol to suffer an
/// effect similar to the hemalurgy decay. Alcohol allows metals not to oxide, so recipients filled with water
/// would give less power and recipients without fluid may hurt the player although being harder to drink

public static final DeferredItem<Item> ALLOMANTIC_BEAD =
        ITEMS.register(
                "allomantic_bead",
                () -> new Item(new Item.Properties().stacksTo(16))
        );

public static final DeferredItem<AllomanticContainerItem> METAL_VIAL =
        ITEMS.register("metal_vial",
                () -> new AllomanticContainerItem(
                        AllomanticContainerItem.Kind.VIAL,
                        ScadrialItems.METAL_VIAL::get,
                        new Item.Properties()
                ));

    public static final DeferredItem<AllomanticContainerItem> WATER_METAL_VIAL =
            ITEMS.register("water_metal_vial",
                    () -> new AllomanticContainerItem(
                            AllomanticContainerItem.Kind.VIAL,
                            ScadrialItems.METAL_VIAL::get,
                            new Item.Properties()
                    ));

    public static final DeferredItem<AllomanticContainerItem> ALCOHOL_METAL_VIAL =
            ITEMS.register("alcohol_metal_vial",
                    () -> new AllomanticContainerItem(
                            AllomanticContainerItem.Kind.VIAL,
                            ScadrialItems.METAL_VIAL::get,
                            new Item.Properties()
                    ));

    public static final DeferredItem<AllomanticContainerItem> FLASK =
            ITEMS.register("flask",
                    () -> new AllomanticContainerItem(
                            AllomanticContainerItem.Kind.FLASK,
                            ScadrialItems.FLASK::get,
                            new Item.Properties()
                    ));

    public static final DeferredItem<AllomanticContainerItem> WATER_FLASK =
            ITEMS.register("water_flask",
                    () -> new AllomanticContainerItem(
                            AllomanticContainerItem.Kind.FLASK,
                            ScadrialItems.FLASK::get,
                            new Item.Properties()
                    ));

    public static final DeferredItem<AllomanticContainerItem> ALCOHOL_FLASK =
            ITEMS.register("alcohol_flask",
                    () -> new AllomanticContainerItem(
                            AllomanticContainerItem.Kind.FLASK,
                            ScadrialItems.FLASK::get,
                            new Item.Properties()
                    ));

    public static final DeferredItem<AllomanticContainerItem> ALUMINIUM_FLASK =
            ITEMS.register("aluminium_flask",
                    () -> new AllomanticContainerItem(
                            AllomanticContainerItem.Kind.FLASK,
                            ScadrialItems.ALUMINIUM_FLASK::get,
                            new Item.Properties()
                    ));

    public static final DeferredItem<AllomanticContainerItem> WATER_ALUMINIUM_FLASK =
            ITEMS.register("water_aluminium_flask",
                    () -> new AllomanticContainerItem(
                            AllomanticContainerItem.Kind.FLASK,
                            ScadrialItems.ALUMINIUM_FLASK::get,
                            new Item.Properties()
                    ));

    public static final DeferredItem<AllomanticContainerItem> ALCOHOL_ALUMINIUM_FLASK =
            ITEMS.register("alcohol_aluminium_flask",
                    () -> new AllomanticContainerItem(
                            AllomanticContainerItem.Kind.FLASK,
                            ScadrialItems.ALUMINIUM_FLASK::get,
                            new Item.Properties()
                    ));

    public static final DeferredItem<AllomanticContainerItem> ALCOHOL_BOTTLE =
            ITEMS.register("alcohol_bottle",
                    () -> new AllomanticContainerItem(
                            AllomanticContainerItem.Kind.BOTTLE,
                            () -> Items.GLASS_BOTTLE,
                            new Item.Properties()
                    ));

    public static final DeferredItem<Item> BUNDLE =
            ITEMS.register("bundle",
                    () -> new AllomanticContainerItem(
                            AllomanticContainerItem.Kind.BUNDLE,
                            ScadrialItems.BUNDLE::get,
                            new Item.Properties()
                    ));


    /// # FERUCHEMY

    //IRON
    public static final DeferredItem<Item> IRON_RING = ITEMS.register("iron_ring",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IRON_BRACELET = ITEMS.register("iron_bracelet",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IRON_BAND = ITEMS.register("iron_band",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> IRON_NECKLACE =ITEMS.register("iron_necklace",
            ()-> new Item(new Item.Properties()));

    //STEEL
    public static final DeferredItem<Item> STEEL_RING = ITEMS.register("steel_ring",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STEEL_BRACELET = ITEMS.register("steel_bracelet",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STEEL_BAND = ITEMS.register("steel_band",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STEEL_NECKLACE =ITEMS.register("steel_necklace",
            ()-> new Item(new Item.Properties()));

    //TIN
    public static final DeferredItem<Item> TIN_RING = ITEMS.register("tin_ring",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TIN_BRACELET = ITEMS.register("tin_bracelet",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TIN_BAND = ITEMS.register("tin_band",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TIN_NECKLACE =ITEMS.register("tin_necklace",
            ()-> new Item(new Item.Properties()));

    //PEWTER
    public static final DeferredItem<Item> PEWTER_RING = ITEMS.register("pewter_ring",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PEWTER_BRACELET = ITEMS.register("pewter_bracelet",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PEWTER_BAND = ITEMS.register("pewter_band",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PEWTER_NECKLACE =ITEMS.register("pewter_necklace",
            ()-> new Item(new Item.Properties()));

    //ZINC
    public static final DeferredItem<Item> ZINC_RING = ITEMS.register("zinc_ring",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ZINC_BRACELET = ITEMS.register("zinc_bracelet",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ZINC_BAND = ITEMS.register("zinc_band",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ZINC_NECKLACE =ITEMS.register("zinc_necklace",
            ()-> new Item(new Item.Properties()));

    //BRASS
    public static final DeferredItem<Item> BRASS_RING = ITEMS.register("brass_ring",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRASS_BRACELET = ITEMS.register("brass_bracelet",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRASS_BAND = ITEMS.register("brass_band",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRASS_NECKLACE =ITEMS.register("brass_necklace",
            ()-> new Item(new Item.Properties()));

    //COPPER
    public static final DeferredItem<Item> COPPER_RING = ITEMS.register("copper_ring",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_BRACELET = ITEMS.register("copper_bracelet",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_BAND = ITEMS.register("copper_band",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_NECKLACE =ITEMS.register("copper_necklace",
            ()-> new Item(new Item.Properties()));

    //BRONZE
    public static final DeferredItem<Item> BRONZE_RING = ITEMS.register("bronze_ring",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRONZE_BRACELET = ITEMS.register("bronze_bracelet",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRONZE_BAND = ITEMS.register("bronze_band",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRONZE_NECKLACE =ITEMS.register("bronze_necklace",
            ()-> new Item(new Item.Properties()));

    //CHROMIUM
    public static final DeferredItem<Item> CHROMIUM_RING = ITEMS.register("chromium_ring",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CHROMIUM_BRACELET = ITEMS.register("chromium_bracelet",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CHROMIUM_BAND = ITEMS.register("chromium_band",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CHROMIUM_NECKLACE =ITEMS.register("chromium_necklace",
            ()-> new Item(new Item.Properties()));

    //NICROSIL
    public static final DeferredItem<Item> NICROSIL_RING = ITEMS.register("nicrosil_ring",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NICROSIL_BRACELET = ITEMS.register("nicrosil_bracelet",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NICROSIL_BAND = ITEMS.register("nicrosil_band",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NICROSIL_NECKLACE =ITEMS.register("nicrosil_necklace",
            ()-> new Item(new Item.Properties()));

    //ALUMINIUM
    public static final DeferredItem<Item> ALUMINIUM_RING = ITEMS.register("aluminium_ring",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ALUMINIUM_BRACELET = ITEMS.register("aluminium_bracelet",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ALUMINIUM_BAND = ITEMS.register("aluminium_band",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ALUMINIUM_NECKLACE =ITEMS.register("aluminium_necklace",
            ()-> new Item(new Item.Properties()));

    //DURALUMIN
    public static final DeferredItem<Item> DURALUMIN_RING = ITEMS.register("duralumin_ring",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DURALUMIN_BRACELET = ITEMS.register("duralumin_bracelet",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DURALUMIN_BAND = ITEMS.register("duralumin_band",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DURALUMIN_NECKLACE =ITEMS.register("duralumin_necklace",
            ()-> new Item(new Item.Properties()));

    //CADMIUM
    public static final DeferredItem<Item> CADMIUM_RING = ITEMS.register("cadmium_ring",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CADMIUM_BRACELET = ITEMS.register("cadmium_bracelet",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CADMIUM_BAND = ITEMS.register("cadmium_band",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CADMIUM_NECKLACE =ITEMS.register("cadmium_necklace",
            ()-> new Item(new Item.Properties()));

    //BENDALLOY
    public static final DeferredItem<Item> BENDALLOY_RING = ITEMS.register("bendalloy_ring",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BENDALLOY_BRACELET = ITEMS.register("bendalloy_bracelet",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BENDALLOY_BAND = ITEMS.register("bendalloy_band",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BENDALLOY_NECKLACE =ITEMS.register("bendalloy_necklace",
            ()-> new Item(new Item.Properties()));

    //GOLD
    public static final DeferredItem<Item> GOLD_RING = ITEMS.register("gold_ring",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GOLD_BRACELET = ITEMS.register("gold_bracelet",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GOLD_BAND = ITEMS.register("gold_band",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GOLD_NECKLACE =ITEMS.register("gold_necklace",
            ()-> new Item(new Item.Properties()));

    //ELECTRUM
    public static final DeferredItem<Item> ELECTRUM_RING = ITEMS.register("electrum_ring",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ELECTRUM_BRACELET = ITEMS.register("electrum_bracelet",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ELECTRUM_BAND = ITEMS.register("electrum_band",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ELECTRUM_NECKLACE =ITEMS.register("electrum_necklace",
            ()-> new Item(new Item.Properties()));

    //ATIUM
    public static final DeferredItem<Item> ATIUM_RING = ITEMS.register("atium_ring",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ATIUM_BRACELET = ITEMS.register("atium_bracelet",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ATIUM_BAND = ITEMS.register("atium_band",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ATIUM_NECKLACE =ITEMS.register("atium_necklace",
            ()-> new Item(new Item.Properties()));

    //LERASIUM
    public static final DeferredItem<Item> LERASIUM_RING = ITEMS.register("lerasium_ring",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LERASIUM_BRACELET = ITEMS.register("lerasium_bracelet",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LERASIUM_BAND = ITEMS.register("lerasium_band",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LERASIUM_NECKLACE =ITEMS.register("lerasium_necklace",
            ()-> new Item(new Item.Properties()));


    /// # HEMALURGY

    public static final DeferredItem<Item> IRON_SPIKE = ITEMS.register("iron_spike",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STEEL_SPIKE = ITEMS.register("steel_spike",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> TIN_SPIKE = ITEMS.register("tin_spike",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PEWTER_SPIKE = ITEMS.register("pewter_spike",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ZINC_SPIKE = ITEMS.register("zinc_spike",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRASS_SPIKE = ITEMS.register("brass_spike",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> COPPER_SPIKE = ITEMS.register("copper_spike",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRONZE_SPIKE = ITEMS.register("bronze_spike",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> CHROMIUM_SPIKE = ITEMS.register("chromium_spike",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NICROSIL_SPIKE = ITEMS.register("nicrosil_spike",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ALUMINIUM_SPIKE = ITEMS.register("aluminium_spike",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DURALUMIN_SPIKE = ITEMS.register("duralumin_spike",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> CADMIUM_SPIKE = ITEMS.register("cadmium_spike",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BENDALLOY_SPIKE = ITEMS.register("bendalloy_spike",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> GOLD_SPIKE = ITEMS.register("gold_spike",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ELECTRUM_SPIKE = ITEMS.register("electrum_spike",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ATIUM_SPIKE = ITEMS.register("atium_spike",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LERASIUM_SPIKE = ITEMS.register("lerasium_spike",
            ()-> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ARMONIUM_SPIKE = ITEMS.register("armonium_spike",
            ()-> new Item(new Item.Properties()));

    public static final DeferredItem<Item> TRELLIUM_SPIKE = ITEMS.register("trellium_spike",
            ()-> new Item(new Item.Properties()));

    public static void register(
            IEventBus modEventBus
    ) {
        ITEMS.register(modEventBus);
    }
}
