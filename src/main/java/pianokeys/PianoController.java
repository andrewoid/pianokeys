package pianokeys;

public class PianoController
{
    private final CompositionView compositionView;
    private final PianoSound sound;
    private final Composition composition;
    private final PianoView pianoView;
    private final Recorder recorder;
    private FallingNotesController fallingNotesController;
    private CompositionRunnable runnable;

    private boolean recording = true;

    public PianoController(CompositionView compositionView,
                           PianoSound sound,
                           Composition composition,
                           PianoView pianoView)
    {
        this.compositionView = compositionView;
        this.sound = sound;
        this.composition = composition;
        this.pianoView = pianoView;
        this.recorder = new Recorder(composition);
    }

    /**
     * Play the note in PianoSound
     *
     * @param note the MIDI note value to play
     */
    public void playNote(int note)
    {
        sound.playNote(note);
        if (fallingNotesController != null && fallingNotesController.wasRecentlyHit(note))
        {
            pianoView.showKeyHit(note, new java.awt.Color(144, 238, 144));
        } else
        {
            pianoView.showKeyPlayed(note, true);
        }
        if (recording)
        {
            recorder.startNote(note);
        }
    }

    /**
     * Stops the note from being played
     *
     * @param note the MIDI note value to stop
     */
    public void stopNote(int note)
    {
        sound.stopNote(note);
        pianoView.showKeyPlayed(note, false);
        if (recording)
        {
            recorder.stopNote();
            compositionView.setCurrentTime(recorder.getCompositionTimeSeconds());
            compositionView.refreshLayout();
        }
    }

    public boolean playComposition()
    {
        if (fallingNotesController != null)
        {
            if (fallingNotesController.isRunning())
            {
                return false;
            }
            fallingNotesController.start();
            return true;
        }
        //going to be using compositionRunnable
        if (runnable == null)
        {
            runnable = new CompositionRunnable(this, composition);
            new Thread(runnable).start();
            return true;
        }
        return false;
    }

    public void stopComposition()
    {
        if (fallingNotesController != null)
        {
            fallingNotesController.stop();
            sound.silenceSong();
            return;
        }
        if (runnable != null)
        {
            runnable.stop();
            runnable = null;
        }
    }

    //clears all notes from the composition
    public void eraseComposition()
    {
        composition.getNoteList().clear();
        compositionView.setCurrentTime(0);
        compositionView.repaint();
        recorder.reset();
        if (fallingNotesController != null)
        {
            fallingNotesController.reset();
        }
    }

    /**
     * Stops the current song and replaces it with a new one, like a song from BitMidi
     *
     * @param song the song to show
     */
    public void loadComposition(Composition song)
    {
        stopComposition();
        if (fallingNotesController != null)
        {
            fallingNotesController.loadComposition(song);
        } else
        {
            composition.getNoteList().clear();
            composition.addNotes(song.getNoteList());
        }
        recorder.reset();
        compositionView.setCurrentTime(0);
        compositionView.refreshLayout();
    }

    //resets the composition playback to the beginning
    public void restartComposition()
    {
        compositionView.setCurrentTime(0);
        if (fallingNotesController != null)
        {
            fallingNotesController.reset();
            sound.silenceSong();
            fallingNotesController.start();
        }
    }

    public void setFallingNotesController(FallingNotesController fallingNotesController)
    {
        this.fallingNotesController = fallingNotesController;
        recording = false;
        fallingNotesController.setNoteHitListener(note ->
        {
            pianoView.showKeyHit(note.key(), new java.awt.Color(255, 190, 80));
            sound.playSongNote(note.key());
        });
        fallingNotesController.setNoteEndListener(note -> sound.stopSongNote(note.key()));
        fallingNotesController.addFinishedListener(sound::silenceSong);
    }

    //changes the current instrument sound
    public void changeInstrument(String instrument)
    {
        if (instrument != null && sound != null)
        {
            sound.setInstrument(instrument);
        }
    }

    public void setRecording(boolean recording)
    {
        this.recording = recording;
    }

    public double getCurrentTime()
    {
        return compositionView.getCurrentTime();
    }

    public void setCurrentTime(double time)
    {
        compositionView.setCurrentTime(time);
    }

    public void setPlaybackSpeed(double speed)
    {
        if (fallingNotesController != null)
        {
            fallingNotesController.setPlaybackSpeed(speed);
        }
    }
}
