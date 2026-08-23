package com.cubiccadence.client.font;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.PackRepository;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Installs one user-selected TTF as an isolated, namespaced client resource pack. */
public final class CustomLyricFontManager {
    static final String PACK_DIRECTORY_NAME = "cubic-cadence-custom-font";
    static final String PACK_ID = "file/" + PACK_DIRECTORY_NAME;
    static final long MAX_FONT_BYTES = 64L * 1024L * 1024L;
    private static final int TRUE_TYPE_SFNT = 0x00010000;
    private static final int TRUE_TYPE_APPLE = 0x74727565;
    private static final String FONT_FILE_NAME = "custom.ttf";
    private static final String FONT_BACKUP_NAME = "custom.ttf.backup";
    private static final String FONT_STAGING_NAME = "custom.ttf.new";
    private static final String SOURCE_NAME_FILE = "source-name.txt";
    private static final Identifier FONT_DEFINITION = Identifier.fromNamespaceAndPath(
            "cubic-cadence",
            "font/custom.json"
    );
    private static final AtomicBoolean SELECTING = new AtomicBoolean();

    private CustomLyricFontManager() {
    }

    public static boolean chooseAndInstall(
            Minecraft minecraft,
            String dialogTitle,
            String filterDescription,
            Consumer<LoadResult> callback
    ) {
        Objects.requireNonNull(minecraft, "minecraft");
        Objects.requireNonNull(callback, "callback");
        if (!SELECTING.compareAndSet(false, true)) {
            return false;
        }

        startWorker("cubic-cadence-font-picker", () -> {
            String selected;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filters = stack.mallocPointer(1);
                filters.put(stack.UTF8("*.ttf")).flip();
                selected = TinyFileDialogs.tinyfd_openFileDialog(
                        dialogTitle,
                        null,
                        filters,
                        filterDescription,
                        false
                );
            } catch (RuntimeException exception) {
                finish(minecraft, callback, new LoadResult(Outcome.IO_ERROR, ""));
                return;
            }

            if (selected == null || selected.isBlank()) {
                finish(minecraft, callback, new LoadResult(Outcome.CANCELLED, ""));
                return;
            }

            PreparedInstall prepared;
            try {
                prepared = prepareInstall(
                        Path.of(selected),
                        minecraft.getResourcePackDirectory(),
                        SharedConstants.RESOURCE_PACK_FORMAT_MAJOR,
                        SharedConstants.RESOURCE_PACK_FORMAT_MINOR
                );
            } catch (InvalidFontException exception) {
                finish(minecraft, callback, new LoadResult(Outcome.INVALID_FILE, ""));
                return;
            } catch (IOException | RuntimeException exception) {
                finish(minecraft, callback, new LoadResult(Outcome.IO_ERROR, ""));
                return;
            }
            minecraft.execute(() -> activatePack(minecraft, prepared, callback));
        });
        return true;
    }

    public static boolean isInstalled(Minecraft minecraft) {
        return Files.isRegularFile(fontPath(minecraft.getResourcePackDirectory()));
    }

    public static boolean isLoaded(Minecraft minecraft) {
        return isInstalled(minecraft)
                && minecraft.getResourceManager().getResource(FONT_DEFINITION).isPresent();
    }

    public static String installedFileName(Minecraft minecraft) {
        Path sourceName = packPath(minecraft.getResourcePackDirectory()).resolve(SOURCE_NAME_FILE);
        if (!Files.isRegularFile(sourceName)) {
            return isInstalled(minecraft) ? FONT_FILE_NAME : "";
        }
        try {
            return Files.readString(sourceName, StandardCharsets.UTF_8).strip();
        } catch (IOException ignored) {
            return FONT_FILE_NAME;
        }
    }

    static PreparedInstall prepareInstall(
            Path source,
            Path resourcePackRoot,
            int packFormatMajor,
            int packFormatMinor
    ) throws IOException {
        validateFont(source);
        Path pack = packPath(resourcePackRoot);
        Path fontDirectory = pack.resolve("assets/cubic-cadence/font");
        Path target = fontDirectory.resolve(FONT_FILE_NAME);
        Path backup = fontDirectory.resolve(FONT_BACKUP_NAME);
        Path staging = fontDirectory.resolve(FONT_STAGING_NAME);
        Files.createDirectories(fontDirectory);

        boolean previousFont = Files.isRegularFile(target);
        Files.deleteIfExists(backup);
        if (previousFont) {
            Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
        }

        PreparedInstall prepared = new PreparedInstall(
                pack,
                target,
                backup,
                staging,
                previousFont,
                safeFileName(source)
        );
        try {
            Files.copy(source, staging, StandardCopyOption.REPLACE_EXISTING);
            replace(staging, target);
            writePackDefinitions(pack, packFormatMajor, packFormatMinor);
            return prepared;
        } catch (IOException | RuntimeException exception) {
            prepared.rollback();
            throw exception;
        }
    }

    private static void activatePack(
            Minecraft minecraft,
            PreparedInstall prepared,
            Consumer<LoadResult> callback
    ) {
        PackRepository repository = minecraft.getResourcePackRepository();
        List<String> previousSelection = List.copyOf(repository.getSelectedIds());
        try {
            repository.reload();
            if (!repository.isAvailable(PACK_ID)
                    || (!repository.getSelectedIds().contains(PACK_ID) && !repository.addPack(PACK_ID))) {
                rollbackAndFinish(minecraft, prepared, previousSelection, callback);
                return;
            }

            minecraft.options.updateResourcePacks(repository);
            minecraft.options.save();
            minecraft.reloadResourcePacks().whenComplete((unused, throwable) -> {
                if (throwable != null) {
                    rollbackAndFinish(minecraft, prepared, previousSelection, callback);
                    return;
                }
                startWorker("cubic-cadence-font-commit", () -> {
                    try {
                        prepared.commit();
                    } catch (IOException ignored) {
                        // The font is already active; metadata cleanup is best effort.
                    }
                    finish(minecraft, callback, new LoadResult(Outcome.SUCCESS, prepared.fileName));
                });
            });
        } catch (RuntimeException exception) {
            rollbackAndFinish(minecraft, prepared, previousSelection, callback);
        }
    }

    private static void rollbackAndFinish(
            Minecraft minecraft,
            PreparedInstall prepared,
            List<String> previousSelection,
            Consumer<LoadResult> callback
    ) {
        startWorker("cubic-cadence-font-rollback", () -> {
            try {
                prepared.rollback();
            } catch (IOException ignored) {
                finish(minecraft, callback, new LoadResult(Outcome.IO_ERROR, ""));
                return;
            }
            minecraft.execute(() -> {
                try {
                    PackRepository repository = minecraft.getResourcePackRepository();
                    repository.reload();
                    repository.setSelected(previousSelection);
                    minecraft.options.updateResourcePacks(repository);
                    minecraft.options.save();
                    minecraft.reloadResourcePacks().whenComplete((unused, throwable) -> finish(
                            minecraft,
                            callback,
                            new LoadResult(Outcome.PACK_ERROR, "")
                    ));
                } catch (RuntimeException exception) {
                    finish(minecraft, callback, new LoadResult(Outcome.PACK_ERROR, ""));
                }
            });
        });
    }

    static void validateFont(Path source) throws IOException {
        if (source == null || !Files.isRegularFile(source)) {
            throw new InvalidFontException();
        }
        String fileName = safeFileName(source).toLowerCase(Locale.ROOT);
        long size = Files.size(source);
        if (!fileName.endsWith(".ttf") || size <= 0L || size > MAX_FONT_BYTES) {
            throw new InvalidFontException();
        }
        try (DataInputStream input = new DataInputStream(Files.newInputStream(source))) {
            int signature = input.readInt();
            if (signature != TRUE_TYPE_SFNT && signature != TRUE_TYPE_APPLE) {
                throw new InvalidFontException();
            }
        } catch (EOFException exception) {
            throw new InvalidFontException();
        }
        try {
            Font.createFont(Font.TRUETYPE_FONT, source.toFile());
        } catch (FontFormatException exception) {
            throw new InvalidFontException();
        }
    }

    static void writePackDefinitions(Path pack, int packFormatMajor, int packFormatMinor) throws IOException {
        writeAtomically(
                pack.resolve("pack.mcmeta"),
                """
                        {
                          "pack": {
                            "pack_format": %d,
                            "min_format": [%d, %d],
                            "max_format": [%d, %d],
                            "description": "Cubic Cadence custom lyric font"
                          }
                        }
                        """.formatted(
                                packFormatMajor,
                                packFormatMajor,
                                packFormatMinor,
                                packFormatMajor,
                                packFormatMinor
                        )
        );
        writeAtomically(
                pack.resolve("assets/cubic-cadence/font/custom.json"),
                """
                        {
                          "providers": [
                            {
                              "type": "ttf",
                              "file": "cubic-cadence:font/custom.ttf",
                              "size": 11.0,
                              "oversample": 2.0,
                              "shift": [0.0, 0.0]
                            },
                            {
                              "type": "reference",
                              "id": "minecraft:include/unifont"
                            }
                          ]
                        }
                        """
        );
    }

    private static Path packPath(Path resourcePackRoot) {
        Path root = resourcePackRoot.toAbsolutePath().normalize();
        Path pack = root.resolve(PACK_DIRECTORY_NAME).normalize();
        if (!pack.startsWith(root)) {
            throw new IllegalArgumentException("Custom font pack escaped the resource-pack directory");
        }
        return pack;
    }

    private static Path fontPath(Path resourcePackRoot) {
        return packPath(resourcePackRoot).resolve("assets/cubic-cadence/font").resolve(FONT_FILE_NAME);
    }

    private static String safeFileName(Path source) {
        Path name = source.getFileName();
        return name == null ? FONT_FILE_NAME : name.toString().replace('\n', '_').replace('\r', '_');
    }

    private static void writeAtomically(Path target, String content) throws IOException {
        Files.createDirectories(target.getParent());
        Path staging = target.resolveSibling(target.getFileName() + ".new");
        Files.writeString(staging, content, StandardCharsets.UTF_8);
        replace(staging, target);
    }

    private static void replace(Path source, Path target) throws IOException {
        try {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void startWorker(String name, Runnable task) {
        Thread.ofPlatform().name(name).daemon(true).start(task);
    }

    private static void finish(Minecraft minecraft, Consumer<LoadResult> callback, LoadResult result) {
        minecraft.execute(() -> {
            SELECTING.set(false);
            callback.accept(result);
        });
    }

    public enum Outcome {
        SUCCESS,
        CANCELLED,
        INVALID_FILE,
        IO_ERROR,
        PACK_ERROR
    }

    public record LoadResult(Outcome outcome, String fileName) {
        public LoadResult {
            outcome = outcome == null ? Outcome.IO_ERROR : outcome;
            fileName = fileName == null ? "" : fileName;
        }
    }

    static final class PreparedInstall {
        private final Path pack;
        private final Path target;
        private final Path backup;
        private final Path staging;
        private final boolean previousFont;
        private final String fileName;

        PreparedInstall(
                Path pack,
                Path target,
                Path backup,
                Path staging,
                boolean previousFont,
                String fileName
        ) {
            this.pack = pack;
            this.target = target;
            this.backup = backup;
            this.staging = staging;
            this.previousFont = previousFont;
            this.fileName = fileName;
        }

        void commit() throws IOException {
            Files.writeString(pack.resolve(SOURCE_NAME_FILE), fileName, StandardCharsets.UTF_8);
            Files.deleteIfExists(backup);
            Files.deleteIfExists(staging);
        }

        void rollback() throws IOException {
            Files.deleteIfExists(staging);
            if (previousFont && Files.isRegularFile(backup)) {
                replace(backup, target);
            } else {
                Files.deleteIfExists(target);
                Files.deleteIfExists(backup);
            }
        }

        Path pack() {
            return pack;
        }
    }

    static final class InvalidFontException extends IOException {
    }
}
