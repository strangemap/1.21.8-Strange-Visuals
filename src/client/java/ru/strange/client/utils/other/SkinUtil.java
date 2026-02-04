package ru.strange.client.utils.other;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.strange.client.Strange;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class SkinUtil {
    private static String customSkinPath = null;
    private static Identifier customSkinIdentifier = null;
    private static boolean lastSetSkinValue = false;

    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final boolean IS_LINUX = OS.contains("nix") || OS.contains("nux") || OS.contains("aix");

    public static void uiPickAndApplySkin() {
        new Thread(() -> {
            openSkinFileDialog();
        }, "SkinManager-FileDialog").start();
    }

    public static void uiResetSkin() {
        customSkinPath = null;
        customSkinIdentifier = null;

        try {
            File skinFile = new File(getSkinDirectory(), "custom_skin.png");
            if (skinFile.exists()) {
                boolean ok = skinFile.delete();
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.sendMessage(
                            Text.literal(ok ? "Skin reset successfully!" : "Failed to delete custom_skin.png"),
                            false
                    );
                }
            } else {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("No custom skin to reset."), false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("Error resetting skin: " + e.getMessage()), false);
            }
        }
    }

    private static Identifier loadCustomSkin() {
        if (customSkinPath == null || customSkinPath.isEmpty()) {
            return null;
        }

        File skinFile = new File(customSkinPath);
        if (!skinFile.exists()) {
            return null;
        }

        try {
            NativeImage image = NativeImage.read(new FileInputStream(skinFile));

            Identifier identifier = Identifier.of("night", "custom_skin_" + System.currentTimeMillis());
            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> identifier.toString(), image);

            MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, texture);

            customSkinIdentifier = identifier;
            return identifier;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static SkinTextures updatedPlayerSkin(SkinTextures originalSkin, PlayerEntity entity) {

        if ((customSkinPath == null || customSkinPath.isEmpty()) && !lastSetSkinValue) {
            loadSavedSkin();
            lastSetSkinValue = true;
        }

        if (customSkinPath == null || customSkinPath.isEmpty()) {
            return originalSkin;
        }

        if (entity == null || entity != MinecraftClient.getInstance().player) {
            return originalSkin;
        }

        Identifier customTexture;

        if (customSkinIdentifier == null) {
            customTexture = loadCustomSkin();
        } else {
            customTexture = customSkinIdentifier;
        }

        if (customTexture == null) {
            return originalSkin;
        }

        return new SkinTextures(
                customTexture,
                originalSkin.textureUrl(),
                originalSkin.capeTexture(),
                originalSkin.elytraTexture(),
                SkinTextures.Model.SLIM,
                originalSkin.secure()
        );
    }


    private static void openSkinFileDialog() {
        try {
            File selectedFile = null;
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                selectedFile = openWindowsFileDialog();
            } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                selectedFile = openLinuxFileDialog();
            } else if (os.contains("mac")) {

                try {
                    ProcessBuilder pb = new ProcessBuilder("osascript", "-e",
                            "choose file of type {\"public.png\"} with prompt \"Select Skin File\"");
                    Process process = pb.start();
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(process.getInputStream()));
                    String path = reader.readLine();
                    if (path != null && !path.isEmpty()) {

                        path = path.replaceFirst("^alias .*:", "").trim();
                        selectedFile = new File(path);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {

                selectedFile = openWindowsFileDialog();
            }

            if (selectedFile != null && selectedFile.exists() && selectedFile.getName().toLowerCase().endsWith(".png")) {
                saveSkinFile(selectedFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static File openWindowsFileDialog() {
        try {

            String powerShellScript =
                    "[System.Reflection.Assembly]::LoadWithPartialName('System.Windows.Forms') | Out-Null; " +
                            "$dialog = New-Object System.Windows.Forms.OpenFileDialog; " +
                            "$dialog.Filter = 'PNG Images (*.png)|*.png|All Files (*.*)|*.*'; " +
                            "$dialog.Title = 'Select Skin File'; " +
                            "$result = $dialog.ShowDialog(); " +
                            "if ($result -eq 'OK') { Write-Output $dialog.FileName }";

            ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe", "-Command", powerShellScript
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));

            String path = null;
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.length() > 0 &&
                        !line.contains("System.Windows.Forms") &&
                        !line.startsWith("PS ") &&
                        !line.contains("Microsoft") &&
                        (line.contains(":\\") || line.startsWith("/"))) {
                    path = line;
                    break;
                }
            }

            int exitCode = process.waitFor();
            reader.close();

            if (path != null && !path.isEmpty() && exitCode == 0) {
                return new File(path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static File openLinuxFileDialog() {
        try {
            ProcessBuilder pb = new ProcessBuilder("zenity", "--file-selection",
                    "--title=Select Skin File", "--file-filter=*.png");
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()));
                String path = reader.readLine();
                if (path != null && !path.isEmpty()) {
                    return new File(path.trim());
                }
            }


            try {
                ProcessBuilder kdialogPb = new ProcessBuilder("kdialog", "--getopenfilename",
                        "", "*.png");
                Process kdialogProcess = kdialogPb.start();
                int kdialogExitCode = kdialogProcess.waitFor();

                if (kdialogExitCode == 0) {
                    java.io.BufferedReader kdialogReader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(kdialogProcess.getInputStream()));
                    String kdialogPath = kdialogReader.readLine();
                    if (kdialogPath != null && !kdialogPath.isEmpty()) {
                        return new File(kdialogPath.trim());
                    }
                }
            } catch (Exception e) {

            }

            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void saveSkinFile(File sourceFile) {
        try {
            File skinDir = getSkinDirectory();
            if (!skinDir.exists()) {
                skinDir.mkdirs();
            }

            File destFile = new File(skinDir, "custom_skin.png");
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            setCustomSkinPath(destFile.getAbsolutePath());

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("Skin saved successfully!"), false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("Error saving skin: " + e.getMessage()), false);
            }
        }
    }

    private static File getSkinDirectory() {
        if (IS_LINUX) {
            String homeDir = System.getProperty("user.home");
            return new File(homeDir, ".nightdlc" + File.separator + "skins");
        } else {
            return new File(Strange.get.root, "skins");
        }
    }

    private static void loadSavedSkin() {
        File skinFile = new File(getSkinDirectory(), "custom_skin.png");
        if (skinFile.exists()) {
            setCustomSkinPath(skinFile.getAbsolutePath());
        }
    }

    public static void setCustomSkinPath(String path) {
        customSkinPath = path;
        customSkinIdentifier = null;
        lastSetSkinValue = true;
    }
}
