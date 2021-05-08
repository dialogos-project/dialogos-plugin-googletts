package de.saar.coli.dialogos.googletts;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * A class for playing audio files.
 */
public class AudioPlayer {
    private Clip clip = null;
    private CountDownLatch syncLatch;

    /**
     * Load an audio file and start playing it concurrently.
     *
     * @param file
     * @throws IOException
     * @throws UnsupportedAudioFileException
     * @throws LineUnavailableException
     */
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

    /**
     * Wait until the current audio stream has finished playing.
     *
     * @throws InterruptedException
     */
    public synchronized void waitUntilFinished() throws InterruptedException {
        if( clip != null ) {
            syncLatch.await();
        }
    }

    /**
     * Stop the currently playing audio stream.
     */
    public synchronized void stop() {
        if( clip != null ) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }
}
