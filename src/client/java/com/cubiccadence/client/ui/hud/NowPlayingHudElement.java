package com.cubiccadence.client.ui.hud;

import com.cubiccadence.client.config.ModConfig;
import com.cubiccadence.client.ui.texture.RemoteTextureCache;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Objects;

/** Reusable now-playing HUD supporting both the compact panel and lyrics-only presentation. */
public final class NowPlayingHudElement implements HudElement {
    private final NowPlayingSource source;
    private final RemoteTextureCache textureCache;

    public NowPlayingHudElement(NowPlayingSource source, RemoteTextureCache textureCache) {
        this.source = Objects.requireNonNull(source, "source");
        this.textureCache = Objects.requireNonNull(textureCache, "textureCache");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        ModConfig config = ModConfig.getInstance();
        if (!config.isHudEnabled()
                || minecraft.player == null
                || minecraft.level == null
                || minecraft.gui.screen() != null
                || minecraft.getDebugOverlay().showDebugScreen()) {
            return;
        }
        source.snapshot().ifPresent(snapshot -> NowPlayingHudRenderer.render(
                graphics,
                minecraft.font,
                config.getHudSettings(),
                NowPlayingHudRenderer.fromSnapshot(snapshot),
                graphics.guiWidth(),
                graphics.guiHeight(),
                0,
                0,
                1.0f,
                textureCache
        ));
    }
}
