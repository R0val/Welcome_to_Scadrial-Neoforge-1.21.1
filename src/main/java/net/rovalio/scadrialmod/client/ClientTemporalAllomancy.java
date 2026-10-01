package net.rovalio.scadrialmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rovalio.scadrialmod.network.TemporalAllomancyNetworking;
import net.rovalio.scadrialmod.power.allomancy.AllomanticFuel;
import net.rovalio.scadrialmod.power.allomancy.temporal.external.TemporalAllomancyMath;
import net.rovalio.scadrialmod.power.allomancy.temporal.external.TemporalBubble;
import net.rovalio.scadrialmod.power.allomancy.temporal.external.TemporalField;
import org.joml.Vector3f;

public final class ClientTemporalAllomancy {

    private static final DustParticleOptions CADMIUM =
            new DustParticleOptions(
                    new Vector3f(0.5F, 0.65F, 1.0F),
                    0.7F
            );

    private static final DustParticleOptions BENDALLOY =
            new DustParticleOptions(
                    new Vector3f(1.0F, 0.75F, 0.35F),
                    0.7F
            );

    private static boolean charging;
    private static boolean bendalloy;

    private static boolean previousPush;
    private static boolean previousPull;
    private static boolean previousShift;

    private static boolean locked;
    private static int heldTicks;

    private ClientTemporalAllomancy() {}

    public static boolean update(
            boolean active,
            boolean push,
            boolean pull
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null) {
            return false;
        }

        boolean shift = minecraft.options.keyShift.isDown();
        boolean captured = charging || locked;

        if (charging) {
            boolean held = bendalloy
                    ? push
                    : pull;

            if (!active || !shift || !burning(bendalloy) || (push && pull)) {
                send(TemporalAllomancyNetworking.CANCEL);

                charging = false;
                locked = true;
            } else if (!held) {
                send(TemporalAllomancyNetworking.RELEASE);

                charging = false;
                locked = true;
            } else {
                heldTicks++;

                if (heldTicks % 5 == 0) {
                    send(TemporalAllomancyNetworking.KEEP);
                }
            }

        } else if (active && shift && !locked && push != pull) {
            boolean startPush = push
                    && (!previousPush || !previousShift)
                    && burning(true);

            boolean startPull = pull
                    && (!previousPull || !previousShift)
                    && burning(false);

            if (startPush || startPull) {
                bendalloy = startPush;
                heldTicks = 0;
                charging = true;
                captured = true;

                send(TemporalAllomancyNetworking.START);
            }
        }

        if (!push && !pull) {
            locked = false;
        }

        previousPush = push;
        previousPull = pull;
        previousShift = shift;

        if (active) {
            particles();
        }

        return captured || charging || locked;
    }

    private static boolean burning(boolean bendalloy) {
        return !ClientAllomancyBurnState.isBurning(
                AllomanticFuel.ALUMINIUM
        ) && ClientAllomancyBurnState.isBurning(
                bendalloy
                        ? AllomanticFuel.BENDALLOY
                        : AllomanticFuel.CADMIUM
        );
    }

    private static void send(int action) {
        var level = Minecraft.getInstance().level;

        if (level != null) {
            PacketDistributor.sendToServer(
                    new TemporalAllomancyNetworking.Input(
                            level.dimension().location(),
                            bendalloy,
                            action
                    )
            );
        }
    }

    public static void receive(
            TemporalAllomancyNetworking.State state
    ) {
        var level = Minecraft.getInstance().level;

        if (level != null
                && level.dimension().location().equals(state.dimension())) {
            TemporalField.of(level).replace(state.bubbles());
        }
    }

    public static double subunitsPerTick(AllomanticFuel fuel) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null) {
            return 0.0;
        }

        for (TemporalBubble bubble
                : TemporalField.of(minecraft.level).bubbles()) {
            AllomanticFuel bubbleFuel = bubble.bendalloy()
                    ? AllomanticFuel.BENDALLOY
                    : AllomanticFuel.CADMIUM;

            if (bubble.owner().equals(minecraft.player.getUUID())
                    && bubbleFuel == fuel) {
                return TemporalAllomancyMath.subunitsPerTick(
                        bubble.radius()
                );
            }
        }

        return 0.0;
    }

    private static void particles() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level.getGameTime() % 3 != 0) {
            return;
        }

        if (charging) {
            var data = ClientAllomancyState.current();

            double radius = TemporalAllomancyMath.radius(
                    data.strength(),
                    data.isDuraluminBoostActive(),
                    TemporalAllomancyMath.charge(heldTicks)
            );

            Vec3 center = minecraft.player.position().add(0, 1.25, 0);

            int count = (int) (64 * radius);
            for (int i = 0; i < count; i++) {
                double angle = Math.PI * 2.0 * i / 48.0;

                particle(
                        center.add(
                                radius * Math.cos(angle),
                                0,
                                radius * Math.sin(angle)
                        ),
                        bendalloy
                );
            }
        }

        for (TemporalBubble bubble
                : TemporalField.of(minecraft.level).bubbles()) {
            if (minecraft.player.distanceToSqr(bubble.center()) > 96.0 * 96.0) {
                continue;
            }

            for (int i = 0; i < 16; i++) {
                double y = minecraft.level.random.nextDouble() * 2.0 - 1.0;

                double angle =
                        minecraft.level.random.nextDouble() * Math.PI * 2.0;

                double horizontal = Math.sqrt(1.0 - y * y);

                Vec3 normal = new Vec3(
                        horizontal * Math.cos(angle),
                        y,
                        horizontal * Math.sin(angle)
                );

                particle(
                        bubble.center().add(
                                normal.scale(bubble.radius())
                        ),
                        bubble.bendalloy()
                );
            }
        }
    }

    private static void particle(
            Vec3 position,
            boolean bendalloy
    ) {
        Minecraft.getInstance().level.addParticle(
                bendalloy ? BENDALLOY : CADMIUM,
                position.x,
                position.y,
                position.z,
                0,
                0,
                0
        );
    }

    public static void clear() {
        charging = false;

        previousPush = false;
        previousPull = false;
        previousShift = false;

        locked = false;
        heldTicks = 0;
    }
}