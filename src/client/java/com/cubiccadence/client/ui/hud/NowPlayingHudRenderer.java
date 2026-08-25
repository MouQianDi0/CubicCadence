package com.cubiccadence.client.ui.hud;

import com.cubiccadence.client.config.HudSettings;
import com.cubiccadence.client.config.HudLyricFont;
import com.cubiccadence.client.font.CustomLyricFontManager;
import com.cubiccadence.client.ui.texture.RemoteTextureCache;
import com.cubiccadence.model.Artist;
import com.cubiccadence.model.LyricLine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

/** Shared renderer used by both the live HUD and its settings preview. */
public final class NowPlayingHudRenderer {
    private static final int DEFAULT_WIDTH = 228;
    private static final int LYRICS_MODE_WIDTH = 320;
    private static final int LYRICS_MODE_LINE_COUNT = 5;
    private static final int MAX_WRAPPED_LINES = 2;
    private static final int LYRICS_MODE_HORIZONTAL_INSET = 2;
    private static final int SCREEN_MARGIN = 8;
    private static final int PADDING = 6;
    private static final int COVER_SIZE = 38;
    private static final int GAP = 6;
    private static final int PROGRESS_HEIGHT = 2;
    private static final int BACKGROUND = 0xB8181E29;
    private static final int BORDER = 0x805B6575;
    private static final int PRIMARY = 0xFFF4F6FA;
    private static final int SECONDARY = 0xFFB7BDC8;
    private static final int PROGRESS_BACKGROUND = 0xFF3B4350;
    private static final int INACTIVE_LYRIC_ALPHA = 0x66;
    private static final float INACTIVE_LYRIC_SCALE = 0.82f;

    private NowPlayingHudRenderer() {
    }

    public static void render(
            GuiGraphicsExtractor graphics,
            Font font,
            HudSettings settings,
            HudContent content,
            int viewportWidth,
            int viewportHeight,
            int originX,
            int originY,
            float viewportScale,
            RemoteTextureCache textureCache
    ) {
        Objects.requireNonNull(graphics, "graphics");
        Objects.requireNonNull(font, "font");
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(content, "content");
        if (!settings.enabled() || viewportWidth <= 0 || viewportHeight <= 0 || viewportScale <= 0.0f) {
            return;
        }

        Layout layout = measure(font, settings, content, viewportWidth, viewportHeight);
        if (layout == null) {
            return;
        }

        int scaledWidth = Math.round(layout.width() * settings.scale());
        int scaledHeight = Math.round(layout.height() * settings.scale());
        int x = settings.position().x(viewportWidth, scaledWidth, settings.offsetX(), SCREEN_MARGIN);
        int y = settings.position().y(viewportHeight, scaledHeight, settings.offsetY(), SCREEN_MARGIN);

        graphics.pose().pushMatrix();
        graphics.pose().translate(originX, originY);
        graphics.pose().scale(viewportScale, viewportScale);
        graphics.pose().translate(x, y);
        graphics.pose().scale(settings.scale(), settings.scale());
        renderLocal(graphics, font, settings, content, layout, textureCache);
        graphics.pose().popMatrix();
    }

    public static HudContent fromSnapshot(NowPlayingSnapshot snapshot) {
        String artists = snapshot.track().artists().stream()
                .map(Artist::name)
                .collect(Collectors.joining(" / "));
        return new HudContent(
                snapshot.track().title(),
                artists.isBlank() ? "-" : artists,
                snapshot.track().coverUrl(),
                snapshot.positionMs(),
                snapshot.durationMs(),
                snapshot.lyricLines(),
                snapshot.currentLyricIndex()
        );
    }

    private static Layout measure(
            Font font,
            HudSettings settings,
            HudContent content,
            int viewportWidth,
            int viewportHeight
    ) {
        if (settings.lyricsMode()) {
            return measureLyricsMode(font, settings, content, viewportWidth, viewportHeight);
        }
        boolean lyrics = settings.showLyrics()
                && (!content.currentLyric().isBlank() || !content.nextLyric().isBlank());
        boolean hasDetails = settings.showTitle() || settings.showArtist() || settings.showProgress();
        if (!settings.showCover() && !hasDetails && !lyrics) {
            return null;
        }

        int availableWidth = viewportWidth - SCREEN_MARGIN * 2;
        int maximumBaseWidth = (int) Math.floor(availableWidth / settings.scale());
        if (maximumBaseWidth < 48) {
            return null;
        }
        int width = settings.showCover() && !hasDetails && !lyrics
                ? COVER_SIZE + PADDING * 2
                : Math.min(DEFAULT_WIDTH, maximumBaseWidth);
        int detailHeight = 0;
        if (settings.showTitle()) {
            detailHeight += scaledLineHeight(font, settings.titleScale());
        }
        if (settings.showArtist()) {
            detailHeight += font.lineHeight;
        }
        if (settings.showProgress()) {
            detailHeight += (detailHeight == 0 ? 0 : 5) + PROGRESS_HEIGHT;
        }
        int topHeight = Math.max(settings.showCover() ? COVER_SIZE : 0, detailHeight);
        int lyricHeight = lyrics ? scaledLineHeight(font, settings.lyricScale()) + 5 : 0;
        return new Layout(width, PADDING * 2 + topHeight + lyricHeight, topHeight, lyrics, null);
    }

    private static Layout measureLyricsMode(
            Font font,
            HudSettings settings,
            HudContent content,
            int viewportWidth,
            int viewportHeight
    ) {
        if (content.lyricLines().isEmpty()) {
            return null;
        }
        int availableWidth = viewportWidth - SCREEN_MARGIN * 2;
        int maximumBaseWidth = (int) Math.floor(availableWidth / settings.scale());
        if (maximumBaseWidth < 48) {
            return null;
        }
        int width = Math.min(LYRICS_MODE_WIDTH, maximumBaseWidth);
        List<Integer> indexes = visibleLyricIndexes(content.lyricLines().size(), content.currentLyricIndex());
        List<LyricsRow> rows = new ArrayList<>(indexes.size());
        for (int index : indexes) {
            boolean active = index == content.currentLyricIndex();
            float rowScale = settings.lyricScale() * (active ? 1.0f : INACTIVE_LYRIC_SCALE);
            LyricLine line = content.lyricLines().get(index);
            int wrappingWidth = availableTextWidth(width - LYRICS_MODE_HORIZONTAL_INSET * 2, rowScale);
            List<FormattedCharSequence> original = wrapLyric(font, lyricText(line.text(), settings), wrappingWidth);
            List<FormattedCharSequence> translated = wrapLyric(
                    font,
                    lyricText(line.translatedText(), settings),
                    wrappingWidth
            );
            if (original.isEmpty() && translated.isEmpty()) {
                continue;
            }
            int visualLines = original.size() + translated.size();
            int translationGap = !original.isEmpty() && !translated.isEmpty() ? 2 : 0;
            int height = visualLines * scaledLineHeight(font, rowScale) + translationGap;
            rows.add(new LyricsRow(original, translated, active, rowScale, height));
        }
        if (rows.isEmpty()) {
            return null;
        }
        int maximumBaseHeight = Math.max(1, (int) Math.floor(
                Math.max(1, viewportHeight - SCREEN_MARGIN * 2) / settings.scale()
        ));
        int height = totalLyricsHeight(rows);
        while (rows.size() > 1 && height > maximumBaseHeight) {
            int activeRow = -1;
            for (int index = 0; index < rows.size(); index++) {
                if (rows.get(index).active()) {
                    activeRow = index;
                    break;
                }
            }
            int removeIndex;
            if (activeRow < 0) {
                removeIndex = rows.size() - 1;
            } else {
                int before = activeRow;
                int after = rows.size() - activeRow - 1;
                removeIndex = after >= before ? rows.size() - 1 : 0;
            }
            rows.remove(removeIndex);
            height = totalLyricsHeight(rows);
        }
        return new Layout(width, height, 0, false, new LyricsModeLayout(List.copyOf(rows)));
    }

    private static void renderLocal(
            GuiGraphicsExtractor graphics,
            Font font,
            HudSettings settings,
            HudContent content,
            Layout layout,
            RemoteTextureCache textureCache
    ) {
        if (layout.lyricsMode() != null) {
            renderLyricsMode(graphics, font, settings, layout.lyricsMode());
            return;
        }
        if (settings.backgroundEnabled()) {
            graphics.fill(0, 0, layout.width(), layout.height(), BACKGROUND);
            graphics.outline(0, 0, layout.width(), layout.height(), BORDER);
        }

        int contentX = PADDING;
        if (settings.showCover()) {
            renderCover(graphics, content.coverUrl(), contentX, PADDING, textureCache);
            contentX += COVER_SIZE + GAP;
        }
        int contentRight = layout.width() - PADDING;
        int contentWidth = Math.max(1, contentRight - contentX);
        int rowY = PADDING;
        if (settings.showTitle()) {
            drawScaledText(
                    graphics,
                    font,
                    fit(font, content.title(), availableTextWidth(contentWidth, settings.titleScale())),
                    contentX,
                    rowY,
                    PRIMARY,
                    settings.titleScale()
            );
            rowY += scaledLineHeight(font, settings.titleScale());
        }
        if (settings.showArtist()) {
            graphics.text(font, fit(font, content.artist(), contentWidth), contentX, rowY, SECONDARY);
        }
        if (settings.showProgress()) {
            int progressY = PADDING + layout.topHeight() - PROGRESS_HEIGHT;
            graphics.fill(contentX, progressY, contentRight, progressY + PROGRESS_HEIGHT, PROGRESS_BACKGROUND);
            double ratio = content.durationMs() <= 0L
                    ? 0.0
                    : Math.max(0.0, Math.min(1.0, (double) content.positionMs() / content.durationMs()));
            int filled = (int) Math.round(contentWidth * ratio);
            if (filled > 0) {
                graphics.fill(contentX, progressY, contentX + filled, progressY + PROGRESS_HEIGHT, settings.lyricColor());
            }
        }

        if (layout.lyrics()) {
            int lyricY = PADDING + layout.topHeight() + 5;
            int lyricWidth = layout.width() - PADDING * 2;
            int columnGap = 8;
            int currentWidth = Math.max(1, (lyricWidth - columnGap) * 55 / 100);
            int nextWidth = Math.max(1, lyricWidth - columnGap - currentWidth);
            Component current = fitLyric(
                    font,
                    content.currentLyric(),
                    settings,
                    availableTextWidth(currentWidth, settings.lyricScale())
            );
            Component next = fitLyric(
                    font,
                    content.nextLyric(),
                    settings,
                    availableTextWidth(nextWidth, settings.lyricScale())
            );
            if (!content.currentLyric().isBlank()) {
                drawScaledText(
                        graphics,
                        font,
                        current,
                        PADDING,
                        lyricY,
                        settings.lyricColor(),
                        settings.lyricScale()
                );
            }
            if (!content.nextLyric().isBlank()) {
                int nextTextWidth = Math.round(font.width(next) * settings.lyricScale());
                drawScaledText(
                        graphics,
                        font,
                        next,
                        contentRight - nextTextWidth,
                        lyricY,
                        darken(settings.lyricColor(), 0.6f),
                        settings.lyricScale()
                );
            }
        }
    }

    private static void renderLyricsMode(
            GuiGraphicsExtractor graphics,
            Font font,
            HudSettings settings,
            LyricsModeLayout layout
    ) {
        int y = 0;
        for (LyricsRow row : layout.rows()) {
            int color = row.active()
                    ? settings.lyricColor()
                    : withAlpha(settings.lyricColor(), INACTIVE_LYRIC_ALPHA);
            int lineHeight = scaledLineHeight(font, row.scale());
            for (FormattedCharSequence line : row.original()) {
                drawScaledText(graphics, font, line, LYRICS_MODE_HORIZONTAL_INSET, y, color, row.scale());
                y += lineHeight;
            }
            if (!row.original().isEmpty() && !row.translated().isEmpty()) {
                y += 2;
            }
            for (FormattedCharSequence line : row.translated()) {
                drawScaledText(graphics, font, line, LYRICS_MODE_HORIZONTAL_INSET, y, color, row.scale());
                y += lineHeight;
            }
            y += 6;
        }
    }

    private static void renderCover(
            GuiGraphicsExtractor graphics,
            String url,
            int x,
            int y,
            RemoteTextureCache textureCache
    ) {
        if (textureCache != null && url != null && !url.isBlank()) {
            textureCache.getOrRequest(url).ifPresentOrElse(
                    identifier -> graphics.blit(identifier, x, y, x + COVER_SIZE, y + COVER_SIZE, 0f, 1f, 0f, 1f),
                    () -> renderCoverPlaceholder(graphics, x, y)
            );
            return;
        }
        renderCoverPlaceholder(graphics, x, y);
    }

    private static void renderCoverPlaceholder(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + COVER_SIZE, y + COVER_SIZE, 0xFF2A313D);
        graphics.outline(x, y, COVER_SIZE, COVER_SIZE, 0xFF596273);
        graphics.fill(x + 9, y + 9, x + 29, y + 29, 0xFF414B5A);
        graphics.fill(x + 15, y + 6, x + 29, y + 12, 0xFF7C8798);
    }

    private static void drawScaledText(
            GuiGraphicsExtractor graphics,
            Font font,
            Component text,
            int x,
            int y,
            int color,
            float scale
    ) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, 0, 0, color);
        graphics.pose().popMatrix();
    }

    private static void drawScaledText(
            GuiGraphicsExtractor graphics,
            Font font,
            FormattedCharSequence text,
            int x,
            int y,
            int color,
            float scale
    ) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, 0, 0, color);
        graphics.pose().popMatrix();
    }

    private static int availableTextWidth(int renderedWidth, float scale) {
        return Math.max(1, (int) Math.floor(renderedWidth / scale));
    }

    private static int scaledLineHeight(Font font, float scale) {
        return Math.max(1, (int) Math.ceil(font.lineHeight * scale));
    }

    private static Component fit(Font font, String value, int width) {
        if (value == null || value.isBlank()) {
            return Component.empty();
        }
        if (font.width(value) <= width) {
            return Component.literal(value);
        }
        int ellipsisWidth = font.width("…");
        return Component.literal(font.plainSubstrByWidth(value, Math.max(0, width - ellipsisWidth)) + "…");
    }

    private static Component fitLyric(Font font, String value, HudSettings settings, int width) {
        if (value == null || value.isBlank()) {
            return Component.empty();
        }
        Component full = lyricText(value, settings);
        if (font.width(full) <= width) {
            return full;
        }
        Component ellipsis = lyricText("…", settings);
        int available = Math.max(0, width - font.width(ellipsis));
        int acceptedEnd = 0;
        for (int offset = 0; offset < value.length(); ) {
            int next = offset + Character.charCount(value.codePointAt(offset));
            if (font.width(lyricText(value.substring(0, next), settings)) > available) {
                break;
            }
            acceptedEnd = next;
            offset = next;
        }
        return lyricText(value.substring(0, acceptedEnd) + "…", settings);
    }

    private static Component lyricText(String value, HudSettings settings) {
        if (value == null || value.isBlank()) {
            return Component.empty();
        }
        if (settings.lyricFont() != HudLyricFont.CUSTOM) {
            return styledLyricRun(value, settings.lyricFont().description(), settings);
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || !CustomLyricFontManager.isLoaded(minecraft)) {
            return styledLyricRun(value, HudLyricFont.DEFAULT.description(), settings);
        }

        MutableComponent text = Component.empty();
        for (LyricFontRun run : splitLyricFontRuns(value, CustomLyricFontManager.glyphSupport(minecraft))) {
            FontDescription description = run.customFont()
                    ? HudLyricFont.CUSTOM.description()
                    : HudLyricFont.DEFAULT.description();
            text.append(styledLyricRun(run.text(), description, settings));
        }
        return text;
    }

    private static Component styledLyricRun(
            String value,
            FontDescription description,
            HudSettings settings
    ) {
        return Component.literal(value).withStyle(style -> style
                .withFont(description)
                .withBold(settings.lyricWeight().bold()));
    }

    static List<LyricFontRun> splitLyricFontRuns(String value, IntPredicate customFontSupports) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(customFontSupports, "customFontSupports");
        if (value.isEmpty()) {
            return List.of();
        }

        List<LyricFontRun> runs = new ArrayList<>();
        int runStart = 0;
        int firstCodePoint = value.codePointAt(0);
        boolean currentUsesCustomFont = customFontSupports.test(firstCodePoint);
        for (int offset = Character.charCount(firstCodePoint); offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            boolean usesCustomFont = customFontSupports.test(codePoint);
            if (usesCustomFont != currentUsesCustomFont) {
                runs.add(new LyricFontRun(value.substring(runStart, offset), currentUsesCustomFont));
                runStart = offset;
                currentUsesCustomFont = usesCustomFont;
            }
            offset += Character.charCount(codePoint);
        }
        runs.add(new LyricFontRun(value.substring(runStart), currentUsesCustomFont));
        return List.copyOf(runs);
    }

    private static List<FormattedCharSequence> wrapLyric(Font font, Component text, int width) {
        if (text.getString().isBlank()) {
            return List.of();
        }
        List<FormattedCharSequence> wrapped = font.split(text, Math.max(1, width));
        return wrapped.size() <= MAX_WRAPPED_LINES
                ? List.copyOf(wrapped)
                : List.copyOf(wrapped.subList(0, MAX_WRAPPED_LINES));
    }

    private static List<Integer> visibleLyricIndexes(int lineCount, int currentIndex) {
        if (lineCount <= 0) {
            return List.of();
        }
        int center = currentIndex < 0 ? 0 : currentIndex;
        int start = Math.max(0, center - LYRICS_MODE_LINE_COUNT / 2);
        int end = Math.min(lineCount, start + LYRICS_MODE_LINE_COUNT);
        start = Math.max(0, end - LYRICS_MODE_LINE_COUNT);
        List<Integer> indexes = new ArrayList<>(end - start);
        for (int index = start; index < end; index++) {
            indexes.add(index);
        }
        return List.copyOf(indexes);
    }

    private static int withAlpha(int color, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | color & 0x00FFFFFF;
    }

    private static int totalLyricsHeight(List<LyricsRow> rows) {
        return rows.stream().mapToInt(LyricsRow::height).sum() + Math.max(0, rows.size() - 1) * 6;
    }

    private static int darken(int color, float factor) {
        int red = Math.round((color >> 16 & 0xFF) * factor);
        int green = Math.round((color >> 8 & 0xFF) * factor);
        int blue = Math.round((color & 0xFF) * factor);
        return color & 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private record Layout(
            int width,
            int height,
            int topHeight,
            boolean lyrics,
            LyricsModeLayout lyricsMode
    ) {
    }

    private record LyricsModeLayout(List<LyricsRow> rows) {
    }

    private record LyricsRow(
            List<FormattedCharSequence> original,
            List<FormattedCharSequence> translated,
            boolean active,
            float scale,
            int height
    ) {
    }

    public record HudContent(
            String title,
            String artist,
            String coverUrl,
            long positionMs,
            long durationMs,
            List<LyricLine> lyricLines,
            int currentLyricIndex
    ) {
        public HudContent {
            title = normalize(title);
            artist = normalize(artist);
            coverUrl = normalize(coverUrl);
            positionMs = Math.max(0L, positionMs);
            durationMs = Math.max(0L, durationMs);
            lyricLines = lyricLines == null ? List.of() : List.copyOf(lyricLines);
            if (currentLyricIndex < 0 || currentLyricIndex >= lyricLines.size()) {
                currentLyricIndex = -1;
            }
        }

        public String currentLyric() {
            return currentLyricIndex < 0 ? "" : lyricLines.get(currentLyricIndex).text();
        }

        public String nextLyric() {
            int nextIndex = currentLyricIndex < 0 ? 0 : currentLyricIndex + 1;
            return nextIndex >= lyricLines.size() ? "" : lyricLines.get(nextIndex).text();
        }

        private static String normalize(String value) {
            return value == null ? "" : value.trim();
        }
    }

    record LyricFontRun(String text, boolean customFont) {
    }
}
