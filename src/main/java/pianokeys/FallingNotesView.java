package pianokeys;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static java.awt.Color.*;

public class FallingNotesView extends JPanel
{
    private static final double LEAD_TIME_SECONDS = 5.0;
    private static final int PIXELS_PER_SECOND = 70;
    private static final int TIMER_DELAY_MS = 16;
    private static final Color NOTE_COLOR = new Color(65, 180, 240);

    private final Composition composition;
    private final Timer timer;
    private long startedAt;
    private double currentTime = -LEAD_TIME_SECONDS;
    private double elapsedBeforePause;
    private Consumer<Note> noteHitListener = note -> { };
    private Runnable finishedListener = () -> { };
    private Consumer<Note> noteEndListener = note -> { };
    private final Set<Note> hitNotes = new HashSet<>();
    private final Set<Note> soundingNotes = new HashSet<>();
    private final Map<Note, Double> visualHeights = new HashMap<>();

    public FallingNotesView(Composition composition)
    {
        this.composition = composition;
        setPreferredSize(new Dimension(1470, 400));
        setBackground(new Color(245, 245, 245));
        setOpaque(true);
        calculateVisualHeights();
        timer = new Timer(TIMER_DELAY_MS, event ->
        {
            currentTime = (System.nanoTime() - startedAt) / 1_000_000_000.0 - LEAD_TIME_SECONDS;
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
                    soundingNotes.add(note);
                    noteHitListener.accept(note);
                }
            }
            repaint();
        });
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
                .anyMatch(currentNote -> Math.abs(currentTime - currentNote.startTime()) <= 0.5);
    }

    public void start()
    {
        startedAt = System.nanoTime() - (long) (elapsedBeforePause * 1_000_000_000.0);
        timer.start();
        repaint();
    }

    public void stop()
    {
        if (!timer.isRunning())
        {
            return;
        }
        elapsedBeforePause = (System.nanoTime() - startedAt) / 1_000_000_000.0;
        timer.stop();
        repaint();
    }

    public boolean isRunning()
    {
        return timer.isRunning();
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
        currentTime = -LEAD_TIME_SECONDS;
        repaint();
    }

    private void calculateVisualHeights()
    {
        Map<Integer, Note> previousNotes = new HashMap<>();
        for (Note note : composition.getNoteList().stream()
                .sorted(java.util.Comparator.comparingDouble(Note::startTime)).toList())
        {
            double requestedHeight = Math.max(6,
                    (note.endTime() - note.startTime()) * PIXELS_PER_SECOND);
            Note previous = previousNotes.get(note.key());
            double height = requestedHeight;
            if (previous != null)
            {
                double availableHeight = (note.startTime() - previous.startTime())
                        * PIXELS_PER_SECOND - 4;
                visualHeights.put(previous, Math.max(6, Math.min(
                        visualHeights.get(previous), availableHeight)));
            }
            visualHeights.put(note, height);
            previousNotes.put(note.key(), note);
        }
    }

    @Override
    protected void paintComponent(Graphics graphics)
    {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        int hitY = getHeight() - 1;
        for (Note note : composition.getNoteList())
        {
            if (currentTime > note.endTime())
            {
                continue;
            }
            double noteHeight = visualHeights.getOrDefault(note, 6.0);
            double bottom = hitY - (note.startTime() - currentTime) * PIXELS_PER_SECOND;
            int x = noteX(note.key());
            int y = (int) Math.round(bottom - noteHeight);
            int height = (int) Math.round(noteHeight);
            if (y + height >= 0 && y <= hitY)
            {
                boolean blackKey = isBlackNote(note.key());
                int width = blackKey ? 18 : 26;
                int noteOffset = blackKey ? 0 : 2;
                g.setColor(NOTE_COLOR);
                g.fillRoundRect(x + noteOffset, y, width, height, 8, 8);
            }
        }
        g.setColor(ORANGE);
        g.fillRect(0, hitY - 2, getWidth(), 2);
        g.dispose();
    }

    private int noteX(int key)
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

    private boolean isBlackNote(int key)
    {
        int pitchClass = Math.floorMod(key, 12);
        return pitchClass == 1 || pitchClass == 3 || pitchClass == 6
                || pitchClass == 8 || pitchClass == 10;
    }
}
