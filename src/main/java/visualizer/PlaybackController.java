package visualizer;

import algorithms.AlgorithmResult;
import algorithms.Step;

import javax.swing.Timer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Plays an algorithm's recorded steps like a video: play, pause, step and change speed.
 */
public class PlaybackController {

    public static final int DEFAULT_DELAY_MS = 750;

    private final Timer timer;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private AlgorithmResult result;
    private int index = -1;

    public PlaybackController() {
        timer = new Timer(DEFAULT_DELAY_MS, e -> advance());
        timer.setInitialDelay(DEFAULT_DELAY_MS);
    }

    //Loads a new run and starts playing it from the first step.
    public void load(AlgorithmResult newResult) {
        timer.stop();
        result = newResult;
        index = 0;
        fire();
        timer.start();
    }

    public void clear() {
        timer.stop();
        result = null;
        index = -1;
        fire();
    }

    public void play() {
        if (result == null) return;
        if (isAtEnd()) index = 0;
        timer.start();
        fire();
    }

    public void pause() {
        timer.stop();
        fire();
    }

    public void togglePlay() {
        if (isPlaying()) pause();
        else play();
    }

    public void stepForward() {
        timer.stop();
        if (result != null && !isAtEnd()) index++;
        fire();
    }

    public void stepBack() {
        timer.stop();
        if (result != null && index > 0) index--;
        fire();
    }

    public void restart() {
        if (result == null) return;
        index = 0;
        timer.restart();
        fire();
    }

    public void jumpTo(int stepIndex) {
        if (result == null) return;
        int clamped = Math.max(0, Math.min(stepIndex, result.steps().size() - 1));
        if (clamped == index) return;
        timer.stop();
        index = clamped;
        fire();
    }

    public void jumpToEnd() {
        if (result != null) jumpTo(result.steps().size() - 1);
    }

    public void setDelay(int milliseconds) {
        timer.setDelay(milliseconds);
        timer.setInitialDelay(milliseconds);
    }

    public int getDelay() {
        return timer.getDelay();
    }

    public boolean hasResult() {
        return result != null;
    }

    public AlgorithmResult getResult() {
        return result;
    }

    public int getIndex() {
        return index;
    }

    public int getStepCount() {
        return result == null ? 0 : result.steps().size();
    }

    public Step currentStep() {
        return result == null ? null : result.steps().get(index);
    }

    public boolean isPlaying() {
        return timer.isRunning();
    }

    public boolean isAtEnd() {
        return result != null && index == result.steps().size() - 1;
    }

    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    private void advance() {
        if (result == null || isAtEnd()) {
            timer.stop();
        } else {
            index++;
            if (isAtEnd()) timer.stop();
        }
        fire();
    }

    private void fire() {
        for (Runnable listener : listeners) listener.run();
    }
}
