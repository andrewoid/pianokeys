
package pianokeys;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/**
 * Drives the "falling tiles" view and song playback timing.
 */
public class FallingNotesController
{
    // Extra seconds before the first note so tiles can fall into view first (like a countdown).
    private final double leadTimeSeconds = 5.0;
    private final int pixelsPerSecond = 70;
    private final int timerDelayMs = 16;
    private final Color noteColor = new Color(65, 180, 240);
    private final Composition composition;
    private final FallingNotesView view;
    private final Timer timer;
    // Notes that have already been reached (their start time is at or before currentTime).
    private final Set<Note> hitNotes = new HashSet<>();
    // Notes that should be audible right now (started but not yet finished).
    private final Set<Note> soundingNotes = new HashSet<>();
    private final Map<Note, Double> visualHeights = new HashMap<>();

    private long startedAt;
    // Position in the song, in seconds. Negative values are the pre-song lead-in.
    private double currentTime = -leadTimeSeconds;
    // How far we have progressed (including lead-in) when paused, so Play can resume.
    private double elapsedBeforePause;

    // 1.0 means normal speed
    private double playbackSpeed = 1.0;

    private Consumer<Note> noteHitListener = note -> { };
    private Consumer<Note> noteEndListener = note -> { };
    private Runnable finishedListener = () -> { };
    // Notifies the scrubber bar whenever the playhead moves.
    private DoubleConsumer timeListener = time -> { };

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

        // Song finished: freeze at the end so the scrubber can be dragged back.
        if (currentTime >= composition.duration())
        {
            stop();
            currentTime = composition.duration();
            elapsedBeforePause = currentTime + leadTimeSeconds;
            soundingNotes.clear();
            finishedListener.run();
            notifyTime();
            view.repaint();
            return;
        }

        for (Note note : composition.getNoteList())
        {
            // Note's hold time ended — stop its sound.
            if (note.endTime() <= currentTime && soundingNotes.remove(note))
            {
                noteEndListener.accept(note);
            }
            // Note's start time reached — begin its sound (once).
            if (note.startTime() <= currentTime && hitNotes.add(note))
            {
                if (note.endTime() > currentTime)
                {
                    soundingNotes.add(note);
                    noteHitListener.accept(note);
                }
            }
        }
        notifyTime();
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

    /** Resume or begin playback from the current playhead position. */
    public void start()
    {
        if (timer.isRunning())
        {
            return;
        }

        startedAt = System.nanoTime();
        // Re-trigger notes that were already "held down" at the pause/seek position.
        for (Note note : soundingNotes)
        {
            noteHitListener.accept(note);
        }
        timer.start();
        notifyTime();
        view.repaint();
    }

    /** Pause playback, remembering where we stopped. */
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

    /** Called whenever the playhead moves so the scrubber UI can stay in sync. */
    public void setTimeListener(DoubleConsumer timeListener)
    {
        this.timeListener = timeListener;
    }

    /**
     * Jump the playhead to a new place in the song (like dragging a YouTube progress bar).
     * {@code timeSeconds} is seconds from the start of the song (0 = beginning).
     * If the song was playing, it keeps playing from the new spot; if paused, it stays paused.
     * Falling tiles are redrawn to match that moment in the song.
     */
    public void seekTo(double timeSeconds)
    {
        // Stop whatever was ringing at the old position.
        for (Note note : soundingNotes)
        {
            noteEndListener.accept(note);
        }
        soundingNotes.clear();
        hitNotes.clear();

        boolean wasRunning = timer.isRunning();
        double duration = composition.duration();
        // Keep the requested time inside the song (cannot go before 0 or past the end).
        double clamped = Math.max(0, Math.min(timeSeconds, duration));

        currentTime = clamped;
        // elapsed includes the lead-in offset used by start()/stop() timing math.
        elapsedBeforePause = clamped + leadTimeSeconds;

        // Rebuild "which notes already happened" and "which notes are held right now"
        // so tiles and sound match the new playhead position.
        for (Note note : composition.getNoteList())
        {
            if (note.startTime() <= clamped)
            {
                hitNotes.add(note);
                // Still within this note's hold window — it should be sounding.
                if (note.endTime() > clamped)
                {
                    soundingNotes.add(note);
                    if (wasRunning)
                    {
                        noteHitListener.accept(note);
                    }
                }
            }
        }

        if (wasRunning)
        {
            // Adjust the clock so the next update() continues from the seeked time.
            startedAt = System.nanoTime();
        }

        notifyTime();
        view.repaint();
    }

    /** Return to the pre-song lead-in (before the first note). */
    public void reset()
    {
        stop();
        elapsedBeforePause = 0;
        hitNotes.clear();
        soundingNotes.clear();
        currentTime = -leadTimeSeconds;
        notifyTime();
        view.repaint();
    }

    /** Tell the scrubber the current song position (never negative; capped at the end). */
    private void notifyTime()
    {
        timeListener.accept(Math.max(0, Math.min(currentTime, composition.duration())));
    }
}
