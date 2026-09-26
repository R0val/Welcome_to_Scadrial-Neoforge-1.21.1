package net.rovalio.scadrialmod.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rovalio.scadrialmod.ScadrialMod;
import net.rovalio.scadrialmod.client.ClientAllomancyBurnState;
import net.rovalio.scadrialmod.client.ClientAllomancyState;
import net.rovalio.scadrialmod.client.ClientAllomancyState.Snapshot;
import net.rovalio.scadrialmod.network.ToggleAllomanticBurnC2SPayload;
import net.rovalio.scadrialmod.player.ScadrialPlayerData.PowerProfile;
import net.rovalio.scadrialmod.power.allomancy.AllomancyBurnManager;
import net.rovalio.scadrialmod.power.allomancy.AllomanticFuel;
import net.rovalio.scadrialmod.power.MetalType;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

public final class AllomancyWheelScreen extends Screen {

    private static final int SIZE = 256;
    private static final int CENTER = SIZE / 2;

    private static final ResourceLocation BACKGROUND =
            texture("allomancy_wheel_background");
    private static final ResourceLocation LOWER_BORDER =
            texture("allomancy_wheel_lower_border");
    private static final ResourceLocation UPPER_BORDER =
            texture("allomancy_wheel_upper_border");

    private static final EnumMap<MetalType, Art> ART =
            new EnumMap<>(MetalType.class);

    private static final MetalType[] INNER = {
            MetalType.PEWTER, MetalType.TIN,
            MetalType.COPPER, MetalType.BRONZE,
            MetalType.ELECTRUM, MetalType.GOLD,
            MetalType.ALUMINIUM, MetalType.DURALUMIN
    };

    private static final MetalType[] OUTER = {
            MetalType.STEEL, MetalType.IRON,
            MetalType.ZINC, MetalType.BRASS,
            MetalType.BENDALLOY, MetalType.CADMIUM,
            MetalType.CHROMIUM, MetalType.NICROSIL
    };

    private static final MetalType[] METAL_LAYERS_TOP_TO_BOTTOM = {

            MetalType.DURALUMIN, MetalType.ELECTRUM,
            MetalType.NICROSIL, MetalType.BENDALLOY,
            MetalType.ALUMINIUM, MetalType.GOLD,
            MetalType.CHROMIUM, MetalType.CADMIUM,

            MetalType.PEWTER, MetalType.BRONZE,
            MetalType.STEEL, MetalType.BRASS,
            MetalType.TIN, MetalType.COPPER,
            MetalType.IRON, MetalType.ZINC
    };

    static {
        for (MetalType metal : MetalType.values()) {
            String name = metal.getSerializedName();

            ART.put(metal, new Art(
                    texture(name + "_allomantic_button"),
                    texture(name + "_allomantic_symbol")
            ));
        }
    }

    private static final int DISPLAY_WIDTH = 106;
    private static final int DISPLAY_HEIGHT = 22;
    private static final int DISPLAY_STEP = 24;
    private static final int DISPLAY_GAP = 5;

    private static final MetalType[] LEFT_DISPLAYS = {
            MetalType.STEEL,
            MetalType.IRON,
            MetalType.PEWTER,
            MetalType.TIN,
            MetalType.DURALUMIN,
            MetalType.ALUMINIUM,
            MetalType.NICROSIL,
            MetalType.CHROMIUM
    };

    private static final MetalType[] RIGHT_DISPLAYS = {
            MetalType.ZINC,
            MetalType.BRASS,
            MetalType.COPPER,
            MetalType.BRONZE,
            MetalType.GOLD,
            MetalType.ELECTRUM,
            MetalType.CADMIUM,
            MetalType.BENDALLOY
    };

    private static final EnumMap<MetalType, SymbolCrop> SYMBOL_CROPS =
            new EnumMap<>(MetalType.class);

    static {
        SYMBOL_CROPS.put(MetalType.ALUMINIUM, new SymbolCrop(90, 162, 29, 32));
        SYMBOL_CROPS.put(MetalType.BENDALLOY, new SymbolCrop(209, 157, 25, 28));
        SYMBOL_CROPS.put(MetalType.BRASS, new SymbolCrop(211, 80, 25, 27));
        SYMBOL_CROPS.put(MetalType.BRONZE, new SymbolCrop(167, 97, 20, 25));
        SYMBOL_CROPS.put(MetalType.CADMIUM, new SymbolCrop(155, 208, 28, 25));
        SYMBOL_CROPS.put(MetalType.CHROMIUM, new SymbolCrop(76, 211, 23, 22));
        SYMBOL_CROPS.put(MetalType.COPPER, new SymbolCrop(136, 66, 23, 27));
        SYMBOL_CROPS.put(MetalType.DURALUMIN, new SymbolCrop(62, 137, 30, 28));
        SYMBOL_CROPS.put(MetalType.ELECTRUM, new SymbolCrop(171, 138, 20, 22));
        SYMBOL_CROPS.put(MetalType.GOLD, new SymbolCrop(137, 164, 20, 33));
        SYMBOL_CROPS.put(MetalType.IRON, new SymbolCrop(76, 20, 19, 25));
        SYMBOL_CROPS.put(MetalType.NICROSIL, new SymbolCrop(24, 157, 22, 20));
        SYMBOL_CROPS.put(MetalType.PEWTER, new SymbolCrop(68, 96, 20, 22));
        SYMBOL_CROPS.put(MetalType.STEEL, new SymbolCrop(23, 77, 20, 26));
        SYMBOL_CROPS.put(MetalType.TIN, new SymbolCrop(96, 61, 22, 28));
        SYMBOL_CROPS.put(MetalType.ZINC, new SymbolCrop(155, 20, 23, 27)); //ARREGLA ESTO
    }

    private final EnumMap<AllomanticFuel, Long> displayedReserves =
            new EnumMap<>(AllomanticFuel.class);

    private Snapshot lastReserveSnapshot;
    private int displayScroll;

    private record SymbolCrop(int u, int v, int width, int height) {
    }

    private int wheelSize;
    private int wheelX;
    private int wheelY;
    private MetalType selected;

    public AllomancyWheelScreen() {
        super(Component.translatable(
                "screen.welcome_to_scadrial.allomancy_wheel"
        ));
    }

    @Override
    protected void init() {
        int availableWidth = width - 12;
        int availableHeight = height - 48;

        wheelSize = Math.min(
                SIZE,
                Math.max(
                        1,
                        Math.min(availableWidth, availableHeight)
                )
        );

        wheelX = (width - wheelSize) / 2;
        wheelY = (height - wheelSize) / 2;
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        Snapshot state = ClientAllomancyState.current();
        MetalType hovered = metalAt(mouseX, mouseY);

        // Minecraft dibuja lo último por encima.
        draw(graphics, BACKGROUND, 0, 0);
        draw(graphics, UPPER_BORDER, 0, 0);

        for (int i = METAL_LAYERS_TOP_TO_BOTTOM.length - 1;
             i >= 0;
             i--) {

            MetalType metal = METAL_LAYERS_TOP_TO_BOTTOM[i];

            if (!state.hasPower(metal)) {
                continue;
            }

            int offsetX = 0;
            int offsetY = 0;

            boolean burning = ClientAllomancyBurnState.isBurning(
                    fuelFor(metal)
            );

            if (burning || metal == hovered) {
                double angle = sectorAngle(metal);

                double movement = (burning ? -2.0 : 2.0)
                        * wheelSize / SIZE;

                offsetX = (int) Math.round(
                        movement * Math.cos(angle)
                );
                offsetY = (int) Math.round(
                        movement * Math.sin(angle)
                );
            }

            drawArt(graphics, metal, offsetX, offsetY);
        }

        draw(graphics, LOWER_BORDER, 0, 0);

        graphics.drawCenteredString(
                font,
                title,
                width / 2,
                wheelY - 12,
                0xFFE9DECA
        );

        graphics.drawCenteredString(
                font,
                statusText(state),
                width / 2,
                wheelY + wheelSize + 4,
                0xFFE9DECA
        );

        renderBurningDisplays(graphics);

        if (hovered != null) {
            graphics.renderComponentTooltip(
                    font,
                    List.of(
                            Component.translatable(
                                    "metal.welcome_to_scadrial."
                                            + hovered.getSerializedName()
                            ),
                            Component.literal(
                                    hovered.getAllomanticTitle()
                            ),
                            powerStatus(state, hovered),
                            Component.translatable(
                                    "screen.welcome_to_scadrial.reserve_amount",
                                    displayedReserve(fuelFor(hovered))
                            )
                    ),
                    mouseX,
                    mouseY
            );
        }
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0) {
            MetalType metal = metalAt(mouseX, mouseY);

            if (metal != null) {
                selected = metal;

                if (ClientAllomancyState.hasReceivedState()
                        && ClientAllomancyBurnState.hasReceivedState()) {

                    AllomanticFuel fuel = fuelFor(metal);
                    Snapshot state = ClientAllomancyState.current();

                    boolean canTurnOff =
                            ClientAllomancyBurnState.isBurning(fuel);

                    boolean canAskToTurnOn =
                            state.canUse(metal)
                                    && state.getReserveSubunits(fuel) > 0L;

                    if (canTurnOff || canAskToTurnOn) {
                        PacketDistributor.sendToServer(
                                new ToggleAllomanticBurnC2SPayload(
                                        fuel.getSerializedName()
                                )
                        );
                    }
                }

                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Component statusText(Snapshot state) {
        if (!ClientAllomancyState.hasReceivedState()) {
            return Component.translatable(
                    "screen.welcome_to_scadrial.synchronizing"
            );
        }

        if (!state.assignmentComplete()) {
            return Component.translatable(
                    "screen.welcome_to_scadrial.unassigned"
            );
        }

        if (selected != null) {
            return Component.translatable(
                            "metal.welcome_to_scadrial."
                                    + selected.getSerializedName()
                    ).copy().append(" | ")
                    .append(powerStatus(state, selected));
        }

        if (state.profile() == PowerProfile.NONE) {
            return Component.translatable(
                    "screen.welcome_to_scadrial.no_allomancy"
            );
        }

        if (!state.snapped()) {
            return Component.translatable(
                    "screen.welcome_to_scadrial.latent"
            );
        }

        return Component.translatable(
                "screen.welcome_to_scadrial.strength",
                String.format(
                        Locale.ROOT,
                        "%.1f",
                        state.effectiveStrength()
                )
        );
    }

    @Override
    public void tick() {
        super.tick();

        Snapshot snapshot = ClientAllomancyState.current();

        if (snapshot != lastReserveSnapshot) {
            displayedReserves.clear();

            for (AllomanticFuel fuel : AllomanticFuel.values()) {
                displayedReserves.put(
                        fuel,
                        Math.max(0L, snapshot.getReserveSubunits(fuel))
                );
            }

            lastReserveSnapshot = snapshot;
            return;
        }

        for (AllomanticFuel fuel : AllomanticFuel.values()) {
            if (!ClientAllomancyBurnState.isBurning(fuel)) {
                continue;
            }

            long current = displayedReserves.getOrDefault(
                    fuel,
                    snapshot.getReserveSubunits(fuel)
            );

            displayedReserves.put( ///esto puede que no tuviera que cambierlo
                    fuel,
                    Math.max(
                            0L,
                            current - snapshot.burnSubunitsPerTick(fuel)
                    )
            );
        }
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double deltaX,
            double deltaY
    ) {
        int visibleRows = visibleDisplayRows();
        int maxScroll = LEFT_DISPLAYS.length - visibleRows;

        boolean besideWheel =
                mouseX < wheelX
                        || mouseX >= wheelX + wheelSize;

        if (besideWheel && maxScroll > 0 && deltaY != 0.0) {
            displayScroll = Math.clamp(
                    displayScroll - (int) Math.signum(deltaY),
                    0,
                    maxScroll
            );
            return true;
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                deltaX,
                deltaY
        );
    }

    private long displayedReserve(AllomanticFuel fuel) {
        Snapshot snapshot = ClientAllomancyState.current();

        // Si acaba de llegar un paquete, mostramos inmediatamente
        // la cifra autorizada por el servidor.
        if (snapshot != lastReserveSnapshot) {
            return snapshot.getReserveSubunits(fuel);
        }

        return displayedReserves.getOrDefault(
                fuel,
                snapshot.getReserveSubunits(fuel)
        );
    }

    private void renderBurningDisplays(GuiGraphics graphics) {
        int sideSpace = (width - wheelSize) / 2 - DISPLAY_GAP - 3;
        int panelWidth = Math.min(DISPLAY_WIDTH, sideSpace);

        if (panelWidth < 34) {
            return;
        }

        int visibleRows = visibleDisplayRows();
        displayScroll = Math.min(
                displayScroll,
                LEFT_DISPLAYS.length - visibleRows
        );

        int top = (height - visibleRows * DISPLAY_STEP) / 2;

        renderDisplayColumn(
                graphics,
                LEFT_DISPLAYS,
                wheelX - DISPLAY_GAP - panelWidth,
                top,
                panelWidth,
                visibleRows
        );

        renderDisplayColumn(
                graphics,
                RIGHT_DISPLAYS,
                wheelX + wheelSize + DISPLAY_GAP,
                top,
                panelWidth,
                visibleRows
        );
    }

    private int visibleDisplayRows() {
        return Math.min(
                8,
                Math.max(1, (height - 8) / DISPLAY_STEP)
        );
    }

    private void renderDisplayColumn(
            GuiGraphics graphics,
            MetalType[] metals,
            int x,
            int top,
            int panelWidth,
            int visibleRows
    ) {
        for (int row = 0; row < visibleRows; row++) {
            MetalType metal = metals[displayScroll + row];
            AllomanticFuel fuel = fuelFor(metal);

            if (!ClientAllomancyBurnState.isBurning(fuel)) {
                continue;
            }

            int y = top + row * DISPLAY_STEP;

            graphics.fill(
                    x,
                    y,
                    x + panelWidth,
                    y + DISPLAY_HEIGHT,
                    0xB8000000
            );

            drawDisplaySymbol(graphics, metal, x + 3, y + 3);

            int textX = x + 23;
            int textRight = x + panelWidth - 2;

            if (textRight <= textX) {
                continue;
            }

            graphics.enableScissor(
                    textX,
                    y,
                    textRight,
                    y + DISPLAY_HEIGHT
            );

            graphics.drawString(
                    font,
                    Component.translatable(
                            "metal.welcome_to_scadrial."
                                    + metal.getSerializedName()
                    ),
                    textX,
                    y + 2,
                    0xFFE9DECA,
                    false
            );

            String details =
                    ClientAllomancyState.current().burnSubunitsPerTick(fuel)
                            + "/t  "
                            + displayedReserve(fuel)
                            + " u";

            graphics.drawString(
                    font,
                    details,
                    textX,
                    y + 12,
                    0xFFBBD7CC,
                    false
            );

            graphics.disableScissor();
        }
    }

    private void drawDisplaySymbol(
            GuiGraphics graphics,
            MetalType metal,
            int x,
            int y
    ) {
        SymbolCrop crop = SYMBOL_CROPS.get(metal);

        if (crop == null) {
            return;
        }

        int sourceMax = Math.max(crop.width(), crop.height());
        int drawnWidth = Math.max(
                1,
                crop.width() * 16 / sourceMax
        );
        int drawnHeight = Math.max(
                1,
                crop.height() * 16 / sourceMax
        );

        graphics.blit(
                ART.get(metal).symbol(),
                x + (16 - drawnWidth) / 2,
                y + (16 - drawnHeight) / 2,
                drawnWidth,
                drawnHeight,
                (float) crop.u(),
                (float) crop.v(),
                crop.width(),
                crop.height(),
                SIZE,
                SIZE
        );
    }

    private Component powerStatus(
            Snapshot state,
            MetalType metal
    ) {
        String suffix;

        if (!state.hasPower(metal)) {
            suffix = "locked";
        } else {
            AllomanticFuel fuel = fuelFor(metal);

            if (ClientAllomancyBurnState.isBurning(fuel)) {
                suffix = "burning";
            } else if (!state.canUse(metal)) {
                suffix = "latent";
            } else if (displayedReserve(fuel) <= 0L) {
                suffix = "empty";
            } else {
                suffix = "available";
            }
        }

        return Component.translatable(
                "screen.welcome_to_scadrial." + suffix
        );
    }

    private static AllomanticFuel fuelFor(MetalType metal) {
        return AllomanticFuel.fromSerializedName(
                metal.getSerializedName()
        ).orElseThrow(() -> new IllegalStateException(
                "No standard fuel for " + metal
        ));
    }

    private MetalType metalAt(
            double screenX,
            double screenY
    ) {
        double x = (screenX - wheelX) * SIZE / wheelSize - CENTER;
        double y = (screenY - wheelY) * SIZE / wheelSize - CENTER;
        double radiusSquared = x * x + y * y;

        if (radiusSquared < 12 * 12
                || radiusSquared > 125 * 125) {
            return null;
        }

        int sector = Math.min(
                7,
                (int) Math.floor(
                        (Math.atan2(y, x) + Math.PI)
                                / (Math.PI / 4.0)
                )
        );

        return radiusSquared < 80 * 80
                ? INNER[sector]
                : OUTER[sector];
    }

    private static double sectorAngle(MetalType metal) {
        for (int sector = 0; sector < 8; sector++) {
            if (INNER[sector] == metal
                    || OUTER[sector] == metal) {
                return -Math.PI
                        + (sector + 0.5) * Math.PI / 4.0;
            }
        }

        throw new IllegalArgumentException(
                "No wheel sector for " + metal
        );
    }

    private void drawArt(
            GuiGraphics graphics,
            MetalType metal,
            int offsetX,
            int offsetY
    ) {
        Art art = ART.get(metal);

        draw(graphics, art.button(), offsetX, offsetY);
        draw(graphics, art.symbol(), offsetX, offsetY);
    }

    private void draw(
            GuiGraphics graphics,
            ResourceLocation texture,
            int offsetX,
            int offsetY
    ) {
        int x = wheelX + offsetX;
        int y = wheelY + offsetY;

        if (wheelSize == SIZE) {
            graphics.blit(texture, x, y, 0, 0, SIZE, SIZE);
        } else {
            // En «Auto», reduce el dibujo para que quepa.
            graphics.blit(
                    texture,
                    x, y,
                    wheelSize, wheelSize,
                    0.0F, 0.0F,
                    SIZE, SIZE,
                    SIZE, SIZE
            );
        }
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                ScadrialMod.MOD_ID,
                "textures/gui/allomancy_wheel/" + name + ".png"
        );
    }

    private record Art(
            ResourceLocation button,
            ResourceLocation symbol
    ) {
    }
}