package pianokeys;

import javax.sound.midi.InvalidMidiDataException;
import java.io.File;
import java.io.IOException;

public class PianoController
{
    private final CompositionView compositionView;
    private final PianoSound sound;
    private final Composition composition;
    private final PianoView pianoView;
    private final Recorder recorder;
    private final MidiFileStorage midiFileStorage = new MidiFileStorage();
    private FallingNotesController fallingNotesController;
    private CompositionRunnable runnable;

    private boolean recording = true;

    // A dedicated recording session started/stopped by the record button, independent of
    // the "recording" flag above (which belongs to the falling-notes feature's own
    // composition). Writes into its own fresh Composition rather than whatever song is
    // currently loaded, so a user's own playing never gets mixed into that song's data.
    private boolean capturingRecording = false;
    private Composition activeRecordingComposition;
    private Recorder activeRecorder;

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
        if (capturingRecording)
        {
            activeRecorder.startNote(note);
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
        if (capturingRecording)
        {
            activeRecorder.stopNote();
        }
    }

    /**
     * Plays a note as part of automated composition playback (CompositionRunnable),
     * as opposed to a note the user actually pressed. Does the same sound/visual work as
     * playNote, but never feeds into an active recording session - a note the app itself
     * is auto-playing was never something the user played.
     */
    public void playPlaybackNote(int note)
    {
        sound.playNote(note);
        pianoView.showKeyPlayed(note, true);
    }

    public void stopPlaybackNote(int note)
    {
        sound.stopNote(note);
        pianoView.showKeyPlayed(note, false);
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

    public boolean isCapturingRecording()
    {
        return capturingRecording;
    }

    /**
     * Starts a new recording session: notes played from now on (regardless of whether a
     * song is currently playing via the falling-notes feature) get captured into a fresh
     * Composition, separate from whatever song is currently loaded.
     */
    public void startRecording()
    {
        activeRecordingComposition = new Composition();
        activeRecorder = new Recorder(activeRecordingComposition);
        capturingRecording = true;
    }

    /**
     * Stops the current recording session and saves everything captured since
     * startRecording() as a .mid file under the user's home directory.
     *
     * @return the saved file, or null if nothing was captured or saving failed
     */
    public File stopRecordingAndSave()
    {
        capturingRecording = false;
        if (activeRecordingComposition == null)
        {
            return null;
        }
        try
        {
            return midiFileStorage.save(activeRecordingComposition);
        } catch (IOException | InvalidMidiDataException e)
        {
            e.printStackTrace();
            return null;
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
