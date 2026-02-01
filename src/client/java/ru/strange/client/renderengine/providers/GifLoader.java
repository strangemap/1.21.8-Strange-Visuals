package ru.strange.client.renderengine.providers;

import com.mojang.blaze3d.textures.GpuTextureView;
import me.x150.renderer.util.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Утилита для загрузки GIF изображений
 */
public final class GifLoader {

    private static final Map<String, GifData> cache = new HashMap<>();

    /**
     * Загружает GIF из файла и возвращает список текстур кадров и задержек
     * @param file Файл GIF
     * @return Пара [список текстур, список задержек в миллисекундах]
     */
    public static GifData loadGif(File file) throws IOException {
        String key = "file:" + file.getAbsolutePath();
        GifData cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        
        try (ImageInputStream stream = ImageIO.createImageInputStream(file)) {
            GifData data = loadGifFromStream(stream, key);
            cache.put(key, data);
            return data;
        }
    }

    /**
     * Загружает GIF из InputStream
     * @param inputStream InputStream с данными GIF
     * @return Пара [список текстур, список задержек в миллисекундах]
     */
    public static GifData loadGif(InputStream inputStream) throws IOException {
        // Для InputStream не кэшируем, так как нет уникального ключа
        try (ImageInputStream stream = ImageIO.createImageInputStream(inputStream)) {
            return loadGifFromStream(stream, "stream:" + System.currentTimeMillis());
        }
    }

    /**
     * Загружает GIF из Identifier (ресурс)
     * @param identifier Identifier ресурса
     * @return Пара [список текстур, список задержек в миллисекундах]
     */
    public static GifData loadGif(Identifier identifier) throws IOException {
        String key = "identifier:" + identifier.toString();
        GifData cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        
        try (InputStream inputStream = MinecraftClient.getInstance().getResourceManager().getResource(identifier).orElseThrow().getInputStream()) {
            try (ImageInputStream stream = ImageIO.createImageInputStream(inputStream)) {
                GifData data = loadGifFromStream(stream, key);
                cache.put(key, data);
                return data;
            }
        }
    }

    /**
     * Очищает кэш загруженных GIF
     */
    public static void clearCache() {
        cache.clear();
    }

    private static GifData loadGifFromStream(ImageInputStream stream, String cacheKey) throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
        if (!readers.hasNext()) {
            throw new IOException("GIF reader not found");
        }

        ImageReader reader = readers.next();
        reader.setInput(stream);

        List<GpuTextureView> frames = new ArrayList<>();
        List<Integer> delays = new ArrayList<>();

        int numFrames = reader.getNumImages(true);
        int gifWidth = reader.getWidth(0);
        int gifHeight = reader.getHeight(0);
        
        // Canvas для накопления кадров
        BufferedImage canvas = new BufferedImage(gifWidth, gifHeight, BufferedImage.TYPE_INT_ARGB);
        BufferedImage savedImage = null;
        
        String prevDisposalMethod = "none";
        
        for (int i = 0; i < numFrames; i++) {
            // Применяем disposal method от предыдущего кадра
            if (i > 0) {
                if ("restoreToBackgroundColor".equals(prevDisposalMethod)) {
                    java.awt.Graphics2D g = canvas.createGraphics();
                    g.setComposite(java.awt.AlphaComposite.Clear);
                    g.fillRect(0, 0, gifWidth, gifHeight);
                    g.dispose();
                } else if ("restoreToPrevious".equals(prevDisposalMethod) && savedImage != null) {
                    java.awt.Graphics2D g = canvas.createGraphics();
                    g.setComposite(java.awt.AlphaComposite.Src);
                    g.drawImage(savedImage, 0, 0, null);
                    g.dispose();
                }
            } else {
                // Первый кадр - очищаем canvas
                java.awt.Graphics2D g = canvas.createGraphics();
                g.setComposite(java.awt.AlphaComposite.Clear);
                g.fillRect(0, 0, gifWidth, gifHeight);
                g.dispose();
            }
            // Читаем metadata для disposal method текущего кадра
            String disposalMethod = "none";
            int frameX = 0, frameY = 0;
            int frameWidth = gifWidth, frameHeight = gifHeight;
            
            try {
                javax.imageio.metadata.IIOMetadata metadata = reader.getImageMetadata(i);
                if (metadata != null) {
                    org.w3c.dom.Node tree = metadata.getAsTree("javax_imageio_gif_image_1.0");
                    if (tree != null) {
                        org.w3c.dom.NodeList children = tree.getChildNodes();
                        for (int j = 0; j < children.getLength(); j++) {
                            org.w3c.dom.Node node = children.item(j);
                            if ("GraphicControlExtension".equals(node.getNodeName())) {
                                org.w3c.dom.NamedNodeMap attrs = node.getAttributes();
                                if (attrs != null) {
                                    org.w3c.dom.Node disp = attrs.getNamedItem("disposalMethod");
                                    if (disp != null) {
                                        disposalMethod = disp.getNodeValue();
                                    }
                                }
                            } else if ("ImageDescriptor".equals(node.getNodeName())) {
                                org.w3c.dom.NamedNodeMap attrs = node.getAttributes();
                                if (attrs != null) {
                                    org.w3c.dom.Node x = attrs.getNamedItem("imageLeftPosition");
                                    org.w3c.dom.Node y = attrs.getNamedItem("imageTopPosition");
                                    org.w3c.dom.Node w = attrs.getNamedItem("imageWidth");
                                    org.w3c.dom.Node h = attrs.getNamedItem("imageHeight");
                                    if (x != null) frameX = Integer.parseInt(x.getNodeValue());
                                    if (y != null) frameY = Integer.parseInt(y.getNodeValue());
                                    if (w != null) frameWidth = Integer.parseInt(w.getNodeValue());
                                    if (h != null) frameHeight = Integer.parseInt(h.getNodeValue());
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            
            // Читаем текущий кадр
            BufferedImage frame = reader.read(i);
            
            // Конвертируем в ARGB если нужно
            if (frame.getType() != BufferedImage.TYPE_INT_ARGB) {
                BufferedImage converted = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = converted.createGraphics();
                g.drawImage(frame, 0, 0, null);
                g.dispose();
                frame = converted;
            }
            
            // Применяем disposal method предыдущего кадра
            if (i > 0) {
                // Для первого кадра disposal method не применяется
            }
            
            // Рисуем текущий кадр на canvas
            java.awt.Graphics2D g = canvas.createGraphics();
            g.setComposite(java.awt.AlphaComposite.SrcOver);
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(frame, frameX, frameY, null);
            g.dispose();
            
            // Создаем копию текущего состояния для сохранения
            BufferedImage finalFrame = new BufferedImage(gifWidth, gifHeight, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D finalG2d = finalFrame.createGraphics();
            finalG2d.drawImage(canvas, 0, 0, null);
            finalG2d.dispose();
            
            // Конвертируем BufferedImage в текстуру
            NativeImageBackedTexture texture = RenderUtils.bufferedImageToNIBT(
                "gif_frame_" + cacheKey + "_" + i,
                finalFrame
            );
            
            frames.add(texture.getGlTextureView());

            // Получаем задержку кадра (в миллисекундах)
            int delay = 100; // Дефолтная задержка
            try {
                javax.imageio.metadata.IIOMetadata metadata = reader.getImageMetadata(i);
                if (metadata != null) {
                    String[] formats = metadata.getMetadataFormatNames();
                    for (String format : formats) {
                        try {
                            org.w3c.dom.Node tree = metadata.getAsTree(format);
                            org.w3c.dom.NodeList nodes = tree.getChildNodes();
                            for (int j = 0; j < nodes.getLength(); j++) {
                                org.w3c.dom.Node node = nodes.item(j);
                                if ("GraphicControlExtension".equals(node.getNodeName())) {
                                    org.w3c.dom.NamedNodeMap attrs = node.getAttributes();
                                    if (attrs != null) {
                                        org.w3c.dom.Node delayNode = attrs.getNamedItem("delayTime");
                                        if (delayNode != null) {
                                            // Задержка в GIF хранится в сотых долях секунды
                                            int delayValue = Integer.parseInt(delayNode.getNodeValue());
                                            delay = delayValue * 10;
                                            // Минимальная задержка 16мс для синхронизации с 60 FPS
                                            if (delay < 16) delay = 16;
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception e) {
                // Если не удалось получить задержку, используем дефолтную
            }
            // Убеждаемся, что задержка не меньше 16мс (60 FPS)
            if (delay < 16) delay = 16;
            delays.add(delay);
            
            // Сохраняем disposal method для следующей итерации
            prevDisposalMethod = disposalMethod;
        }
        
        reader.dispose();
        
        // Генерируем стабильный ID на основе cacheKey и количества кадров
        String gifId = "gif_" + cacheKey.hashCode() + "_" + frames.size();
        
        return new GifData(frames, delays, gifId);
    }

    /**
     * Данные загруженного GIF
     */
    public static class GifData {
        private final List<GpuTextureView> frames;
        private final List<Integer> delays;
        private final String gifId;

        public GifData(List<GpuTextureView> frames, List<Integer> delays, String gifId) {
            this.frames = frames;
            this.delays = delays;
            this.gifId = gifId;
        }

        public List<GpuTextureView> getFrames() {
            return frames;
        }

        public List<Integer> getDelays() {
            return delays;
        }

        public String getGifId() {
            return gifId;
        }
    }
}
