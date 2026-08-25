package com.cubiccadence.client.font;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.PackSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomLyricFontManagerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void invalidTtfIsRejectedBeforeItCanBecomeAResourcePack() throws Exception {
        Path fakeFont = temporaryDirectory.resolve("fake.ttf");
        Files.writeString(fakeFont, "not a true type font", StandardCharsets.UTF_8);

        assertThrows(
                CustomLyricFontManager.InvalidFontException.class,
                () -> CustomLyricFontManager.validateFont(fakeFont)
        );
    }

    @Test
    void generatedPackUsesNamespacedTtfAndUnicodeFallback() throws Exception {
        Path pack = temporaryDirectory.resolve("pack");
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        CustomLyricFontManager.writePackDefinitions(pack, 88, 0);
        Path managedFont = pack.resolve("assets/cubic-cadence/font/custom.ttf");
        Files.createDirectories(managedFont.getParent());
        Files.writeString(managedFont, "path-resolution-fixture", StandardCharsets.UTF_8);

        JsonObject metadata = JsonParser.parseString(
                Files.readString(pack.resolve("pack.mcmeta"), StandardCharsets.UTF_8)
        ).getAsJsonObject();
        assertEquals(88, metadata.getAsJsonObject("pack").get("pack_format").getAsInt());

        JsonObject font = JsonParser.parseString(Files.readString(
                pack.resolve("assets/cubic-cadence/font/custom.json"),
                StandardCharsets.UTF_8
        )).getAsJsonObject();
        assertEquals("ttf", font.getAsJsonArray("providers").get(0).getAsJsonObject().get("type").getAsString());
        assertEquals(
                "cubic-cadence:custom.ttf",
                font.getAsJsonArray("providers").get(0).getAsJsonObject().get("file").getAsString()
        );
        assertEquals(
                "minecraft:include/unifont",
                font.getAsJsonArray("providers").get(1).getAsJsonObject().get("id").getAsString()
        );
        font.getAsJsonArray("providers").forEach(provider -> assertDoesNotThrow(
                () -> GlyphProviderDefinition.MAP_CODEC.codec().parse(JsonOps.INSTANCE, provider).getOrThrow()
        ));

        PackLocationInfo location = new PackLocationInfo(
                "test-font-pack",
                Component.literal("test-font-pack"),
                PackSource.DEFAULT,
                Optional.empty()
        );
        PathPackResources resources = new PathPackResources(location, pack);
        try {
            TrueTypeGlyphProviderDefinition ttf = TrueTypeGlyphProviderDefinition.CODEC.codec()
                    .parse(JsonOps.INSTANCE, font.getAsJsonArray("providers").get(0))
                    .getOrThrow();
            assertEquals("cubic-cadence:font/custom.ttf", ttf.location().withPrefix("font/").toString());
            assertNotNull(resources.getResource(
                    PackType.CLIENT_RESOURCES,
                    ttf.location().withPrefix("font/")
            ));
            PackMetadataSection decoded = resources.getMetadataSection(PackMetadataSection.CLIENT_TYPE);
            assertNotNull(decoded);
            assertTrue(decoded.supportedFormats().isValueInRange(PackFormat.of(88, 0)));
        } finally {
            resources.close();
        }
    }

    @Test
    void rollbackRestoresThePreviousManagedFont() throws Exception {
        Path pack = temporaryDirectory.resolve("pack");
        Path fontDirectory = pack.resolve("assets/cubic-cadence/font");
        Files.createDirectories(fontDirectory);
        Path target = fontDirectory.resolve("custom.ttf");
        Path backup = fontDirectory.resolve("custom.ttf.backup");
        Path staging = fontDirectory.resolve("custom.ttf.new");
        Files.writeString(target, "new", StandardCharsets.UTF_8);
        Files.writeString(backup, "old", StandardCharsets.UTF_8);
        Files.writeString(staging, "staging", StandardCharsets.UTF_8);
        CustomLyricFontManager.PreparedInstall install = new CustomLyricFontManager.PreparedInstall(
                pack,
                target,
                backup,
                staging,
                true,
                "chosen.ttf"
        );

        install.rollback();

        assertEquals("old", Files.readString(target, StandardCharsets.UTF_8));
        assertFalse(Files.exists(backup));
        assertFalse(Files.exists(staging));
    }

    @Test
    void commitKeepsOnlyTheBaseSourceName() throws Exception {
        Path pack = temporaryDirectory.resolve("pack");
        Path fontDirectory = pack.resolve("assets/cubic-cadence/font");
        Files.createDirectories(fontDirectory);
        Path target = fontDirectory.resolve("custom.ttf");
        Path backup = fontDirectory.resolve("custom.ttf.backup");
        Path staging = fontDirectory.resolve("custom.ttf.new");
        Files.writeString(target, "font", StandardCharsets.UTF_8);
        Files.writeString(backup, "old", StandardCharsets.UTF_8);
        CustomLyricFontManager.PreparedInstall install = new CustomLyricFontManager.PreparedInstall(
                pack,
                target,
                backup,
                staging,
                true,
                "chosen.ttf"
        );

        install.commit();

        assertEquals("chosen.ttf", Files.readString(pack.resolve("source-name.txt"), StandardCharsets.UTF_8));
        assertTrue(Files.isRegularFile(target));
        assertFalse(Files.exists(backup));
    }
}
