package manager;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class SoundManager {

    private Clip background,
                endMusic,
                marioDiesClip;
    private long clipTime = 0;

    public SoundManager() {
        background = getClip(loadAudio("background"));
        endMusic = getClip(loadAudio("gameOver"));
        marioDiesClip = getClip(loadAudio("marioDies"));
    }

    private AudioInputStream loadAudio(String url) {
        try {
            InputStream audioSrc = getClass().getResourceAsStream("/media/audio/" + url + ".wav");
            InputStream bufferedIn = new BufferedInputStream(audioSrc);
            return AudioSystem.getAudioInputStream(bufferedIn);

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return null;
    }

    private Clip getClip(AudioInputStream stream) {
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            return clip;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void stopAllMusic() {
        background.stop();
        endMusic.stop();
        marioDiesClip.stop();
        clipTime = 0;
    }

    public void resumeBackground(){
        background.setMicrosecondPosition(clipTime);
        background.start();
    }

    public void pauseBackground(){
        clipTime = background.getMicrosecondPosition();
        background.stop();
    }

    public void restartBackground() {
        marioDiesClip.stop();
        endMusic.stop();
        clipTime = 0;
        resumeBackground();
    }

    public void playJump() {
        Clip clip = getClip(loadAudio("jump"));
        clip.start();

    }

    public void playCoin() {
        Clip clip = getClip(loadAudio("coin"));
        clip.start();

    }

    public void playFireball() {
        Clip clip = getClip(loadAudio("fireball"));
        clip.start();

    }

    public void playGameOver() {
        background.stop();
        marioDiesClip.stop();
        clipTime = 0;
        
        endMusic.setMicrosecondPosition(0);
        endMusic.start();
    }

    public void playStomp() {
        Clip clip = getClip(loadAudio("stomp"));
        clip.start();

    }

    public void playOneUp() {
        Clip clip = getClip(loadAudio("oneUp"));
        clip.start();

    }

    public void playSuperMushroom() {

        Clip clip = getClip(loadAudio("superMushroom"));
        clip.start();

    }

    public void playMarioDies() {
        background.stop();
        clipTime = 0;
        marioDiesClip.setMicrosecondPosition(0);
        marioDiesClip.start();
    }

    public void playFireFlower() {

    }

    public boolean isClipPlaying() {
        return background.isActive() || marioDiesClip.isActive() || endMusic.isActive();
    }
}
