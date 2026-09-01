package net.rovalio.scadrialmod;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.rovalio.scadrialmod.commands.ScadrialInvestedArtCommands;
import net.rovalio.scadrialmod.origin.ScadrialOriginInitializer;
import net.rovalio.scadrialmod.player.ScadrialAttachments;
import net.rovalio.scadrialmod.player.ScadrialPlayerLifecycleHandler;
import net.rovalio.scadrialmod.registry.*;
import org.slf4j.Logger;

/// Hi. If you understand this and the API yuno ball. I dunno

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(ScadrialMod.MOD_ID)
public final class ScadrialMod {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "welcome_to_scadrial";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public ScadrialMod(IEventBus modEventBus) {

        ScadrialAttachments.register(modEventBus);

        ScadrialPlanets.register(modEventBus);
        ScadrialOrigins.register(modEventBus);
        ScadrialShards.register(modEventBus);
        ScadrialInvestedArts.register(modEventBus);

        ScadrialOriginConnections.register();

        ScadrialInvestedArtCommands.register();

        ScadrialOriginInitializer.register();

        ScadrialPlayerLifecycleHandler.register();

    }
}