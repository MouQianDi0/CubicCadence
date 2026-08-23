package com.cubiccadence.client.ui.screen;

import com.cubiccadence.client.config.HudPosition;
import com.cubiccadence.client.config.HudSettings;
import com.cubiccadence.client.config.HudLyricFont;
import com.cubiccadence.client.config.HudLyricWeight;
import com.cubiccadence.client.config.ModConfig;
import com.cubiccadence.client.font.CustomLyricFontManager;
import com.cubiccadence.client.ui.hud.NowPlayingHudRenderer;
import com.cubiccadence.model.LyricLine;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;
import java.util.List;

/** Live HUD customization with a game-scale preview and grouped controls. */
public final class HudSettingsScreen extends Screen {
    private static final int PAGE_MARGIN = 12;
    private static final int COLUMN_GAP = 12;
    private static final int BODY_TOP = 42;
    private static final int FOOTER_HEIGHT = 32;
    private static final int TAB_HEIGHT = 20;
    private static final int CONTROL_HEIGHT = 20;
    private static final int CONTROL_GAP = 6;
    private static final int PREVIEW_LABEL_HEIGHT = 16;
    private static final int PANEL_BORDER = 0xFF4B5666;

    private final Screen parent;
    private Tab activeTab = Tab.DISPLAY;

    private boolean enabled;
    private boolean showCover;
    private boolean showTitle;
    private boolean showArtist;
    private boolean showProgress;
    private boolean showLyrics;
    private boolean lyricsMode;
    private float hudScale;
    private float titleScale;
    private float lyricScale;
    private int lyricRed;
    private int lyricGreen;
    private int lyricBlue;
    private HudLyricFont lyricFont;
    private HudLyricWeight lyricWeight;
    private boolean backgroundEnabled;
    private HudPosition position;
    private int offsetX;
    private int offsetY;
    private boolean customFontLoading;
    private Component customFontStatus = Component.empty();

    public HudSettingsScreen(Screen parent) {
        super(Component.translatable("screen.cubic-cadence.hud_settings"));
        this.parent = parent;
        loadDraft(ModConfig.getInstance().getHudSettings());
    }

    @Override
    protected void init() {
        PageLayout layout = pageLayout();
        addTabs(layout);
        switch (activeTab) {
            case DISPLAY -> addDisplayControls(layout);
            case SIZE -> addSizeControls(layout);
            case APPEARANCE -> addAppearanceControls(layout);
            case LYRICS -> addLyricsControls(layout);
            case POSITION -> addPositionControls(layout);
        }
        addFooter(layout);
    }

    private void addTabs(PageLayout layout) {
        int tabGap = 3;
        int tabWidth = Math.max(1, (layout.controlWidth() - tabGap * (Tab.values().length - 1)) / Tab.values().length);
        int x = layout.controlX();
        for (Tab tab : Tab.values()) {
            Button button = this.addRenderableWidget(
                    Button.builder(Component.translatable(tab.translationKey), ignored -> selectTab(tab))
                            .bounds(x, BODY_TOP, tabWidth, TAB_HEIGHT)
                            .build()
            );
            button.active = tab != activeTab;
            x += tabWidth + tabGap;
        }
    }

    private void addDisplayControls(PageLayout layout) {
        int x = layout.controlX();
        int y = layout.controlsTop();
        int displayGap = 3;
        addOption(x, y, layout.controlWidth(), "checkbox.cubic-cadence.hud_enabled", enabled, value -> enabled = value);
        y += CONTROL_HEIGHT + displayGap;
        addOption(x, y, layout.controlWidth(), "checkbox.cubic-cadence.hud_cover", showCover, value -> showCover = value);
        y += CONTROL_HEIGHT + displayGap;
        addOption(x, y, layout.controlWidth(), "checkbox.cubic-cadence.hud_title", showTitle, value -> showTitle = value);
        y += CONTROL_HEIGHT + displayGap;
        addOption(x, y, layout.controlWidth(), "checkbox.cubic-cadence.hud_artist", showArtist, value -> showArtist = value);
        y += CONTROL_HEIGHT + displayGap;
        addOption(x, y, layout.controlWidth(), "checkbox.cubic-cadence.hud_progress", showProgress, value -> showProgress = value);
    }

    private void addSizeControls(PageLayout layout) {
        int x = layout.controlX();
        int y = layout.controlsTop();
        addScaleSlider(x, y, layout.controlWidth(), "slider.cubic-cadence.hud_scale", () -> hudScale, value -> hudScale = value);
        y += CONTROL_HEIGHT + CONTROL_GAP;
        addScaleSlider(x, y, layout.controlWidth(), "slider.cubic-cadence.hud_title_scale", () -> titleScale, value -> titleScale = value);
        y += CONTROL_HEIGHT + CONTROL_GAP;
        addScaleSlider(x, y, layout.controlWidth(), "slider.cubic-cadence.hud_lyric_scale", () -> lyricScale, value -> lyricScale = value);
    }

    private void addAppearanceControls(PageLayout layout) {
        int x = layout.controlX();
        int y = layout.controlsTop();
        addOption(
                x,
                y,
                layout.controlWidth(),
                "checkbox.cubic-cadence.hud_background",
                backgroundEnabled,
                value -> backgroundEnabled = value
        );
        y += CONTROL_HEIGHT + CONTROL_GAP;
        addColorSlider(x, y, layout.controlWidth(), "slider.cubic-cadence.hud_lyric_red", () -> lyricRed, value -> lyricRed = value);
        y += CONTROL_HEIGHT + CONTROL_GAP;
        addColorSlider(x, y, layout.controlWidth(), "slider.cubic-cadence.hud_lyric_green", () -> lyricGreen, value -> lyricGreen = value);
        y += CONTROL_HEIGHT + CONTROL_GAP;
        addColorSlider(x, y, layout.controlWidth(), "slider.cubic-cadence.hud_lyric_blue", () -> lyricBlue, value -> lyricBlue = value);
    }

    private void addLyricsControls(PageLayout layout) {
        int x = layout.controlX();
        int y = layout.controlsTop();
        int lyricsGap = 3;
        addOption(
                x,
                y,
                layout.controlWidth(),
                "checkbox.cubic-cadence.hud_lyrics_mode",
                lyricsMode,
                value -> lyricsMode = value
        );
        y += CONTROL_HEIGHT + lyricsGap;
        addOption(
                x,
                y,
                layout.controlWidth(),
                "checkbox.cubic-cadence.hud_lyrics",
                showLyrics,
                value -> showLyrics = value
        );
        y += CONTROL_HEIGHT + lyricsGap;
        this.addRenderableWidget(
                Button.builder(lyricFontMessage(), ignored -> cycleLyricFont())
                        .bounds(x, y, layout.controlWidth(), CONTROL_HEIGHT)
                        .build()
        );
        y += CONTROL_HEIGHT + lyricsGap;
        this.addRenderableWidget(
                Button.builder(lyricWeightMessage(), ignored -> cycleLyricWeight())
                        .bounds(x, y, layout.controlWidth(), CONTROL_HEIGHT)
                        .build()
        );
        y += CONTROL_HEIGHT + lyricsGap;
        Button customFontButton = this.addRenderableWidget(
                Button.builder(customFontButtonMessage(), ignored -> chooseCustomFont())
                        .bounds(x, y, layout.controlWidth(), CONTROL_HEIGHT)
                        .build()
        );
        customFontButton.active = !customFontLoading;
    }

    private void addPositionControls(PageLayout layout) {
        int x = layout.controlX();
        int y = layout.controlsTop();
        int anchorGap = 4;
        int anchorWidth = Math.max(1, (layout.controlWidth() - anchorGap * 2) / 3);
        HudPosition[] anchors = HudPosition.values();
        for (int index = 0; index < anchors.length; index++) {
            HudPosition anchor = anchors[index];
            int column = index % 3;
            int row = index / 3;
            Button button = this.addRenderableWidget(
                    Button.builder(
                                    Component.translatable("hud_position.cubic-cadence." + anchor.name().toLowerCase()),
                                    ignored -> selectPosition(anchor)
                            )
                            .bounds(
                                    x + column * (anchorWidth + anchorGap),
                                    y + row * (CONTROL_HEIGHT + anchorGap),
                                    anchorWidth,
                                    CONTROL_HEIGHT
                            )
                            .build()
            );
            button.active = anchor != position;
        }
        y += CONTROL_HEIGHT * 3 + anchorGap * 2 + CONTROL_GAP;
        this.addRenderableWidget(new ValueSlider(
                x,
                y,
                layout.controlWidth(),
                normalizedOffset(offsetX),
                () -> Component.translatable("slider.cubic-cadence.hud_offset_x", offsetX),
                value -> offsetX = denormalizedOffset(value)
        ));
        y += CONTROL_HEIGHT + CONTROL_GAP;
        this.addRenderableWidget(new ValueSlider(
                x,
                y,
                layout.controlWidth(),
                normalizedOffset(offsetY),
                () -> Component.translatable("slider.cubic-cadence.hud_offset_y", offsetY),
                value -> offsetY = denormalizedOffset(value)
        ));
    }

    private void addFooter(PageLayout layout) {
        int gap = 6;
        int buttonWidth = Math.max(1, (layout.controlWidth() - gap) / 2);
        int y = this.height - FOOTER_HEIGHT + 6;
        this.addRenderableWidget(
                Button.builder(Component.translatable("button.cubic-cadence.restore_defaults"), ignored -> restoreDefaults())
                        .bounds(layout.controlX(), y, buttonWidth, CONTROL_HEIGHT)
                        .build()
        );
        this.addRenderableWidget(
                Button.builder(Component.translatable("button.cubic-cadence.done"), ignored -> onClose())
                        .bounds(layout.controlX() + buttonWidth + gap, y, buttonWidth, CONTROL_HEIGHT)
                        .build()
        );
    }

    private void addOption(int x, int y, int width, String key, boolean selected, Consumer<Boolean> setter) {
        this.addRenderableWidget(
                Checkbox.builder(Component.translatable(key), this.font)
                        .pos(x, y)
                        .maxWidth(width)
                        .selected(selected)
                        .onValueChange((checkbox, value) -> setter.accept(value))
                        .build()
        );
    }

    private void addScaleSlider(
            int x,
            int y,
            int width,
            String key,
            Supplier<Float> getter,
            Consumer<Float> setter
    ) {
        float initial = getter.get();
        this.addRenderableWidget(new ValueSlider(
                x,
                y,
                width,
                normalizedScale(initial),
                () -> Component.translatable(key, Math.round(getter.get() * 100.0f)),
                value -> setter.accept(denormalizedScale(value))
        ));
    }

    private void addColorSlider(
            int x,
            int y,
            int width,
            String key,
            Supplier<Integer> getter,
            Consumer<Integer> setter
    ) {
        this.addRenderableWidget(new ValueSlider(
                x,
                y,
                width,
                getter.get() / 255.0,
                () -> Component.translatable(key, getter.get()),
                value -> setter.accept((int) Math.round(value * 255.0))
        ));
    }

    private void selectTab(Tab tab) {
        this.activeTab = tab;
        rebuildWidgets();
    }

    private void selectPosition(HudPosition position) {
        this.position = position;
        rebuildWidgets();
    }

    private void cycleLyricFont() {
        lyricFont = switch (lyricFont) {
            case DEFAULT -> HudLyricFont.UNIFORM;
            case UNIFORM -> CustomLyricFontManager.isInstalled(this.minecraft)
                    ? HudLyricFont.CUSTOM
                    : HudLyricFont.DEFAULT;
            case CUSTOM -> HudLyricFont.DEFAULT;
        };
        rebuildWidgets();
    }

    private void cycleLyricWeight() {
        lyricWeight = lyricWeight == HudLyricWeight.REGULAR
                ? HudLyricWeight.BOLD
                : HudLyricWeight.REGULAR;
        rebuildWidgets();
    }

    private void chooseCustomFont() {
        if (customFontLoading || this.minecraft == null) {
            return;
        }
        customFontLoading = true;
        customFontStatus = Component.translatable("status.cubic-cadence.custom_font.loading");
        rebuildWidgets();
        boolean started = CustomLyricFontManager.chooseAndInstall(
                this.minecraft,
                Component.translatable("dialog.cubic-cadence.custom_font.title").getString(),
                Component.translatable("dialog.cubic-cadence.custom_font.filter").getString(),
                this::handleCustomFontResult
        );
        if (!started) {
            customFontLoading = false;
            customFontStatus = Component.translatable("status.cubic-cadence.custom_font.busy");
            rebuildWidgets();
        }
    }

    private void handleCustomFontResult(CustomLyricFontManager.LoadResult result) {
        customFontLoading = false;
        switch (result.outcome()) {
            case SUCCESS -> {
                lyricFont = HudLyricFont.CUSTOM;
                customFontStatus = Component.translatable(
                        "status.cubic-cadence.custom_font.success",
                        result.fileName()
                );
            }
            case CANCELLED -> customFontStatus = Component.translatable("status.cubic-cadence.custom_font.cancelled");
            case INVALID_FILE -> customFontStatus = Component.translatable("status.cubic-cadence.custom_font.invalid");
            case IO_ERROR -> customFontStatus = Component.translatable("status.cubic-cadence.custom_font.io_error");
            case PACK_ERROR -> customFontStatus = Component.translatable("status.cubic-cadence.custom_font.pack_error");
        }
        if (this.minecraft != null) {
            rebuildWidgets();
        }
    }

    private Component lyricFontMessage() {
        String installedName = this.minecraft == null
                ? ""
                : CustomLyricFontManager.installedFileName(this.minecraft);
        Component fontName = switch (lyricFont) {
            case DEFAULT -> Component.translatable("font.cubic-cadence.default");
            case UNIFORM -> Component.translatable("font.cubic-cadence.uniform");
            case CUSTOM -> installedName.isBlank()
                    ? Component.translatable("font.cubic-cadence.custom_missing")
                    : Component.translatable("font.cubic-cadence.custom", installedName);
        };
        return Component.translatable("setting.cubic-cadence.lyric_font", fontName);
    }

    private Component lyricWeightMessage() {
        return Component.translatable(
                "setting.cubic-cadence.lyric_weight",
                Component.translatable("font_weight.cubic-cadence." + lyricWeight.name().toLowerCase())
        );
    }

    private Component customFontButtonMessage() {
        return customFontLoading
                ? Component.translatable("button.cubic-cadence.custom_font.loading")
                : Component.translatable("button.cubic-cadence.custom_font.choose");
    }

    private void restoreDefaults() {
        loadDraft(HudSettings.defaults());
        rebuildWidgets();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float tickDelta) {
        super.extractRenderState(extractor, mouseX, mouseY, tickDelta);
        PageLayout layout = pageLayout();
        extractor.centeredText(this.font, this.title, this.width / 2, 18, 0xFFFFFFFF);
        extractor.text(
                this.font,
                Component.translatable("label.cubic-cadence.hud_preview"),
                layout.previewX(),
                BODY_TOP + 5,
                0xFFE6EAF0
        );
        renderPreview(extractor, layout);
        if (activeTab == Tab.APPEARANCE) {
            renderColorSwatch(extractor, layout);
        }
        if (activeTab == Tab.LYRICS && !customFontStatus.getString().isBlank()) {
            renderCustomFontStatus(extractor, layout);
        }
    }

    private void renderPreview(GuiGraphicsExtractor extractor, PageLayout layout) {
        int areaX = layout.previewX();
        int areaY = BODY_TOP + PREVIEW_LABEL_HEIGHT;
        int areaWidth = layout.previewWidth();
        int areaHeight = Math.max(1, this.height - PAGE_MARGIN - areaY);
        float viewportScale = Math.min(
                areaWidth / (float) Math.max(1, this.width),
                areaHeight / (float) Math.max(1, this.height)
        );
        int viewportWidth = Math.max(1, Math.round(this.width * viewportScale));
        int viewportHeight = Math.max(1, Math.round(this.height * viewportScale));
        int viewportX = areaX + (areaWidth - viewportWidth) / 2;
        int viewportY = areaY + (areaHeight - viewportHeight) / 2;

        renderMiniGameScene(extractor, viewportX, viewportY, viewportWidth, viewportHeight);
        extractor.outline(viewportX, viewportY, viewportWidth, viewportHeight, PANEL_BORDER);
        extractor.enableScissor(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight);
        NowPlayingHudRenderer.render(
                extractor,
                this.font,
                currentSettings(),
                previewContent(),
                this.width,
                this.height,
                viewportX,
                viewportY,
                viewportScale,
                null
        );
        extractor.disableScissor();
    }

    private void renderMiniGameScene(
            GuiGraphicsExtractor extractor,
            int x,
            int y,
            int width,
            int height
    ) {
        int horizon = y + height * 58 / 100;
        extractor.fill(x, y, x + width, horizon, 0xFF263A4D);
        extractor.fill(x, horizon, x + width, y + height, 0xFF233326);
        extractor.horizontalLine(x, x + width - 1, horizon, 0xFF48614E);
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        if (width >= 30 && height >= 24) {
            extractor.horizontalLine(centerX - 3, centerX + 3, centerY, 0xBFFFFFFF);
            extractor.verticalLine(centerX, centerY - 3, centerY + 3, 0xBFFFFFFF);
            int hotbarWidth = Math.min(72, Math.max(18, width / 3));
            int hotbarHeight = Math.max(3, Math.min(8, height / 12));
            int hotbarX = centerX - hotbarWidth / 2;
            int hotbarY = y + height - hotbarHeight - 3;
            extractor.fill(hotbarX, hotbarY, hotbarX + hotbarWidth, hotbarY + hotbarHeight, 0xA010141A);
            extractor.outline(hotbarX, hotbarY, hotbarWidth, hotbarHeight, 0x806F7783);
        }
        extractor.outline(x, y, width, height, 0xFF687586);
    }

    private void renderColorSwatch(GuiGraphicsExtractor extractor, PageLayout layout) {
        int y = layout.controlsTop() + (CONTROL_HEIGHT + CONTROL_GAP) * 4 + 2;
        int swatchWidth = Math.min(72, layout.controlWidth());
        extractor.text(
                this.font,
                Component.translatable("label.cubic-cadence.hud_lyric_color"),
                layout.controlX(),
                y,
                0xFFBFC7D2
        );
        int swatchX = layout.controlX() + layout.controlWidth() - swatchWidth;
        int swatchY = y + this.font.lineHeight + 4;
        extractor.fill(swatchX, swatchY, swatchX + swatchWidth, swatchY + 10, lyricColor());
        extractor.outline(swatchX, swatchY, swatchWidth, 10, 0xFF8993A1);
    }

    private void renderCustomFontStatus(GuiGraphicsExtractor extractor, PageLayout layout) {
        int y = layout.controlsTop() + (CONTROL_HEIGHT + 3) * 5;
        extractor.textWithWordWrap(
                this.font,
                customFontStatus,
                layout.controlX(),
                y,
                layout.controlWidth(),
                0xFFBFC7D2
        );
    }

    private PageLayout pageLayout() {
        int contentWidth = Math.max(1, this.width - PAGE_MARGIN * 2);
        int contentX = Math.max(0, (this.width - contentWidth) / 2);
        int gap = Math.min(COLUMN_GAP, Math.max(4, contentWidth / 30));
        int availableWidth = Math.max(2, contentWidth - gap);
        int preferredControlWidth = availableWidth < 600
                ? Math.max(180, availableWidth * 58 / 100)
                : Math.min(410, Math.max(220, availableWidth * 38 / 100));
        int minimumPreviewWidth = Math.min(120, availableWidth / 3);
        int controlWidth = Math.max(1, Math.min(preferredControlWidth, availableWidth - minimumPreviewWidth));
        int previewWidth = Math.max(1, availableWidth - controlWidth);
        return new PageLayout(
                contentX,
                previewWidth,
                contentX + previewWidth + gap,
                controlWidth,
                BODY_TOP + TAB_HEIGHT + 10
        );
    }

    private HudSettings currentSettings() {
        return new HudSettings(
                enabled,
                showCover,
                showTitle,
                showArtist,
                showProgress,
                showLyrics,
                lyricsMode,
                hudScale,
                titleScale,
                lyricScale,
                lyricColor(),
                lyricFont,
                lyricWeight,
                backgroundEnabled,
                position,
                offsetX,
                offsetY
        );
    }

    private NowPlayingHudRenderer.HudContent previewContent() {
        return new NowPlayingHudRenderer.HudContent(
                Component.translatable("preview.cubic-cadence.hud_title").getString(),
                Component.translatable("preview.cubic-cadence.hud_artist").getString(),
                "",
                86_000L,
                214_000L,
                List.of(
                        previewLyric("previous_2", 70_000L),
                        previewLyric("previous", 78_000L),
                        previewLyric("current", 86_000L),
                        previewLyric("next", 94_000L),
                        previewLyric("next_2", 102_000L)
                ),
                2
        );
    }

    private LyricLine previewLyric(String key, long startTimeMs) {
        return new LyricLine(
                startTimeMs,
                Component.translatable("preview.cubic-cadence.hud_lyric." + key).getString(),
                Component.translatable("preview.cubic-cadence.hud_lyric." + key + ".translated").getString()
        );
    }

    private void loadDraft(HudSettings settings) {
        this.enabled = settings.enabled();
        this.showCover = settings.showCover();
        this.showTitle = settings.showTitle();
        this.showArtist = settings.showArtist();
        this.showProgress = settings.showProgress();
        this.showLyrics = settings.showLyrics();
        this.lyricsMode = settings.lyricsMode();
        this.hudScale = settings.scale();
        this.titleScale = settings.titleScale();
        this.lyricScale = settings.lyricScale();
        this.lyricRed = settings.lyricRed();
        this.lyricGreen = settings.lyricGreen();
        this.lyricBlue = settings.lyricBlue();
        this.lyricFont = settings.lyricFont();
        this.lyricWeight = settings.lyricWeight();
        this.backgroundEnabled = settings.backgroundEnabled();
        this.position = settings.position();
        this.offsetX = settings.offsetX();
        this.offsetY = settings.offsetY();
    }

    private int lyricColor() {
        return 0xFF000000 | lyricRed << 16 | lyricGreen << 8 | lyricBlue;
    }

    @Override
    public void onClose() {
        if (customFontLoading) {
            return;
        }
        ModConfig.getInstance().setHudSettings(currentSettings());
        this.minecraft.setScreenAndShow(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static double normalizedScale(float scale) {
        return (scale - HudSettings.MIN_SCALE) / (HudSettings.MAX_SCALE - HudSettings.MIN_SCALE);
    }

    private static float denormalizedScale(double value) {
        return HudSettings.MIN_SCALE + (float) value * (HudSettings.MAX_SCALE - HudSettings.MIN_SCALE);
    }

    private static double normalizedOffset(int offset) {
        return (offset - HudSettings.MIN_OFFSET) / (double) (HudSettings.MAX_OFFSET - HudSettings.MIN_OFFSET);
    }

    private static int denormalizedOffset(double value) {
        return HudSettings.MIN_OFFSET
                + (int) Math.round(value * (HudSettings.MAX_OFFSET - HudSettings.MIN_OFFSET));
    }

    private enum Tab {
        DISPLAY("tab.cubic-cadence.hud_display"),
        SIZE("tab.cubic-cadence.hud_size"),
        APPEARANCE("tab.cubic-cadence.hud_appearance"),
        LYRICS("tab.cubic-cadence.hud_lyrics"),
        POSITION("tab.cubic-cadence.hud_position");

        private final String translationKey;

        Tab(String translationKey) {
            this.translationKey = translationKey;
        }
    }

    private record PageLayout(
            int previewX,
            int previewWidth,
            int controlX,
            int controlWidth,
            int controlsTop
    ) {
    }

    private static final class ValueSlider extends AbstractSliderButton {
        private final Supplier<Component> messageSupplier;
        private final DoubleConsumer setter;

        private ValueSlider(
                int x,
                int y,
                int width,
                double value,
                Supplier<Component> messageSupplier,
                DoubleConsumer setter
        ) {
            super(x, y, width, CONTROL_HEIGHT, Component.empty(), value);
            this.messageSupplier = messageSupplier;
            this.setter = setter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(messageSupplier.get());
        }

        @Override
        protected void applyValue() {
            setter.accept(this.value);
        }
    }
}
