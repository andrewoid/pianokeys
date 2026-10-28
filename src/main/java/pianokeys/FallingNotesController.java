
package pianokeys;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class FallingNotesController
{
    private final double leadTimeSeconds = 5.0;
    private final int pixelsPerSecond = 70;
    private final int timerDelayMs = 16;
    private final Color noteColor = new Color(65, 180, 240);
    private final Composition composition;
    private final FallingNotesView view;
    private final Timer timer;
    private final Set<Note> hitNotes = new HashSet<>();
    private final Set<Note> soundingNotes = new HashSet<>();
    private final Map<Note, Double> visualHeights = new HashMap<>();

    private long startedAt;
    private double currentTime = -leadTimeSeconds;
    private double elapsedBeforePause;

    // 1.0 means normal speed
    private double playbackSpeed = 1.0;

    private Consumer<Note> noteHitListener = note ->
    {
    };
    private Consumer<Note> noteEndListener = note ->
    {
    };
    private Runnable finishedListener = () ->
    {
    };

    public FallingNotesController(Composition composition)
    {
        this.composition = composition;
        view = new FallingNotesView(this);
        view.setPreferredSize(new Dimension(1470, 400));
        view.setBackground(new Color(245, 245, 245));
        view.setOpaque(true);
        calculateVisualHeights();
        timer = new Timer(timerDelayMs, event -> update());
    }

    private double getElapsedTime()
    {
        if (timer.isRunning())
        {
            double realSeconds =
                    (System.nanoTime() - startedAt) / 1_000_000_000.0;

            return elapsedBeforePause + realSeconds * playbackSpeed;
        }

        return elapsedBeforePause;
    }

    private void update()
    {
        currentTime = getElapsedTime() - leadTimeSeconds;

        if (currentTime > composition.duration())
        {
            stop();
            finishedListener.run();
            return;
        }

        for (Note note : composition.getNoteList())
        {
            if (note.endTime() <= currentTime && soundingNotes.remove(note))
            {
                noteEndListener.accept(note);
            }

            if (note.startTime() <= currentTime && hitNotes.add(note))
            {
                if (note.endTime() > currentTime)
                {
                    soundingNotes.add(note);
                    noteHitListener.accept(note);
                }
            }
        }

        view.repaint();
    }

    private void calculateVisualHeights()
    {
        Map<Integer, Note> previousNotes = new HashMap<>();

        for (Note note : composition.getNoteList().stream()
                .sorted(java.util.Comparator.comparingDouble(Note::startTime))
                .toList())
        {
            double requestedHeight = Math.max(6,
                    (note.endTime() - note.startTime()) * pixelsPerSecond);

            Note previous = previousNotes.get(note.key());
            double height = requestedHeight;

            if (previous != null)
            {
                double availableHeight =
                        (note.startTime() - previous.startTime())
                                * pixelsPerSecond - 4;

                visualHeights.put(previous,
                        Math.max(6, Math.min(
                                visualHeights.get(previous), availableHeight)));
            }

            visualHeights.put(note, height);
            previousNotes.put(note.key(), note);
        }
    }

    /**
     * Replaces the falling notes with a new song and goes back to the start
     *
     * @param song the song to show
     */
    public void loadComposition(Composition song)
    {
        reset();
        composition.getNoteList().clear();
        composition.addNotes(song.getNoteList());
        visualHeights.clear();
        calculateVisualHeights();
        view.repaint();
    }


    public FallingNotesView getView()
    {
        return view;
    }

    public Composition getComposition()
    {
        return composition;
    }

    public double getCurrentTime()
    {
        return currentTime;
    }

    public double getVisualHeight(Note note)
    {
        return visualHeights.getOrDefault(note, 6.0);
    }

    public int getPixelsPerSecond()
    {
        return pixelsPerSecond;
    }

    public Color getNoteColor()
    {
        return noteColor;
    }

    public int noteXcoordinate(int key)
    {
        int octave = Math.floorDiv(key, 12) - 2;
        int pitchClass = Math.floorMod(key, 12);
        int[] whitePitches = {0, 2, 4, 5, 7, 9, 11};

        for (int i = 0; i < whitePitches.length; i++)
        {
            if (pitchClass == whitePitches[i])
            {
                return (octave * 7 + i) * 30;
            }
        }

        int whiteIndex = switch (pitchClass)
        {
            case 1 -> 0;
            case 3 -> 1;
            case 6 -> 3;
            case 8 -> 4;
            case 10 -> 5;
            default -> 0;
        };

        return (octave * 7 + whiteIndex) * 30 + 21;
    }

    public boolean isBlackNote(int key)
    {
        int pitchClass = Math.floorMod(key, 12);

        return pitchClass == 1 || pitchClass == 3 || pitchClass == 6
                || pitchClass == 8 || pitchClass == 10;
    }

    public void setNoteHitListener(Consumer<Note> noteHitListener)
    {
        this.noteHitListener = noteHitListener;
    }

    public void setNoteEndListener(Consumer<Note> noteEndListener)
    {
        this.noteEndListener = noteEndListener;
    }

    public void addFinishedListener(Runnable listener)
    {
        Runnable previous = finishedListener;

        finishedListener = () ->
        {
            previous.run();
            listener.run();
        };
    }

    public boolean wasRecentlyHit(int note)
    {
        if (!timer.isRunning())
        {
            return false;
        }

        return composition.getNoteList().stream()
                .filter(currentNote -> currentNote.key() == note)
                .anyMatch(currentNote ->
                        Math.abs(currentTime - currentNote.startTime()) <= 0.5);
    }

    public void start()
    {
        if (timer.isRunning())
        {
            return;
        }

        startedAt = System.nanoTime();
        timer.start();
        view.repaint();
    }

    public void stop()
    {
        if (!timer.isRunning())
        {
            return;
        }

        elapsedBeforePause = getElapsedTime();
        timer.stop();
        currentTime = elapsedBeforePause - leadTimeSeconds;
        view.repaint();
    }

    public boolean isRunning()
    {
        return timer.isRunning();
    }

    // Change speed without restarting the song
    public void setPlaybackSpeed(double speed)
    {
        if (speed <= 0)
        {
            throw new IllegalArgumentException("Speed must be greater than 0");
        }

        if (timer.isRunning())
        {
            elapsedBeforePause = getElapsedTime();
            startedAt = System.nanoTime();
        }

        playbackSpeed = speed;
        view.repaint();
    }

    public double getPlaybackSpeed()
    {
        return playbackSpeed;
    }

    public void setFinishedListener(Runnable finishedListener)
    {
        this.finishedListener = finishedListener;
    }

    public void reset()
    {
        stop();
        elapsedBeforePause = 0;
        hitNotes.clear();
        soundingNotes.clear();
        currentTime = -leadTimeSeconds;
        view.repaint();
    }
}
