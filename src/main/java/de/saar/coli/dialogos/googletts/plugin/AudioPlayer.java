package de.saar.coli.dialogos.googletts.plugin;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class AudioPlayer {
    private Clip clip = null;
    private CountDownLatch syncLatch;

    public synchronized void play(File file) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        if( clip != null ) {
            stop();
        }

        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file.getAbsoluteFile());
        clip = AudioSystem.getClip();

        syncLatch = new CountDownLatch(1);
        clip.addLineListener(e -> {
            if (e.getType() == LineEvent.Type.STOP) {
                clip = null;
                syncLatch.countDown();
            }
        });

        clip.open(audioInputStream);
        clip.start();
    }

    public synchronized void waitUntilFinished() throws InterruptedException {
        if( clip != null ) {
            syncLatch.await();
        }
    }

    public synchronized void stop() {
        if( clip != null ) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }

    public static void main(String[] args) throws UnsupportedAudioFileException, IOException, LineUnavailableException, InterruptedException {
        AudioPlayer a = new AudioPlayer();
        a.play(new File ("/Users/koller/Documents/workspace/mxml-mary/audio.wav"));
        a.waitUntilFinished();
    }
}
