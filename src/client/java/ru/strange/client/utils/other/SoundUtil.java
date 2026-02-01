package ru.strange.client.utils.other;

import ru.strange.client.Strange;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SoundUtil {
    private static Clip currentClip = null;
    public static void playSound_mp3(String sound, float value, boolean nonstop) {
        if (currentClip != null && currentClip.isRunning()) {
            currentClip.stop();
        }
        try {
            currentClip = AudioSystem.getClip();
            InputStream is = Strange.class.getResourceAsStream("/assets/" + Strange.get.rootRes + "/sounds/mp3/" + sound);
            BufferedInputStream bis = new BufferedInputStream(is);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bis);
            if (audioInputStream == null) {
                System.out.println("Sound not found!");
                return;
            }

            currentClip.open(audioInputStream);
            currentClip.start();
            FloatControl floatControl = (FloatControl) currentClip.getControl(FloatControl.Type.MASTER_GAIN);
            float min = floatControl.getMinimum();
            float max = floatControl.getMaximum();
            float volumeInDecibels = (float) (min * (1 - (value / 100.0)) + max * (value / 100.0));
            floatControl.setValue(volumeInDecibels);
            if (nonstop) {
                currentClip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        currentClip.setFramePosition(0);
                        currentClip.start();
                    }
                });
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static AudioInputStream stream;
    private static final List<Clip> CLIPS_LIST;

    public static void playSound_wav(String location, float volume) {
        CLIPS_LIST.stream().filter(Objects::nonNull).filter(clip -> clip.isOpen()).filter(clip -> !clip.isRunning()).forEach(Line::close);
        CLIPS_LIST.stream().filter(Objects::nonNull).filter(clip -> !clip.isOpen() || !clip.isRunning()).forEach(DataLine::stop);
        CLIPS_LIST.stream().filter(Objects::nonNull).collect(Collectors.toList()).forEach(clip -> {
            if (!clip.isRunning()) {
                CLIPS_LIST.remove(clip);
            }
        });
        try {
            String resourcePath = "/assets/" + Strange.get.rootRes + "/sounds/wav/" + location + ".wav";
            InputStream inputStream = SoundUtil.class.getResourceAsStream(resourcePath);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            stream = AudioSystem.getAudioInputStream(bufferedInputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (stream == null) {
            return;
        }
        try {
            CLIPS_LIST.add(AudioSystem.getClip());
        } catch (Exception e) {
            e.printStackTrace();
        }
        CLIPS_LIST.stream().filter(Objects::nonNull).filter(clip -> !clip.isOpen()).forEach(clip -> {
            try {
                clip.open(stream);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        CLIPS_LIST.stream().filter(Objects::nonNull).filter(Line::isOpen).forEach(clip -> {
            float volumeVal = volume < 0.0f ? 0.0f : (volume > 1.0f ? 1.0f : volume);
            FloatControl volumeControl = (FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);
            volumeControl.setValue((float)(Math.log(volumeVal) / Math.log(10.0) * 20.0));
        });
        CLIPS_LIST.stream().filter(Objects::nonNull).filter(Line::isOpen).filter(clip -> !clip.isRunning()).forEach(DataLine::start);
    }

    static {
        CLIPS_LIST = new ArrayList<Clip>();
    }
}
