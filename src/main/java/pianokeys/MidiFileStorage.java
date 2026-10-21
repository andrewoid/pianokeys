package pianokeys;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Saves a Composition as a standard .mid file under the user's home directory,
 * at ~/pianokeys/my-recordings/, so recordings persist across app runs and are
 * easy for the user to find on their own machine.
 */
public class MidiFileStorage
{
    private static final int TICKS_PER_QUARTER_NOTE = 480;
    private static final int MICROSECONDS_PER_QUARTER_NOTE = 500_000; // 120 BPM
    private static final int NOTE_VELOCITY = 100;

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public Path getRecordingsDirectory() throws IOException
    {
        Path directory = Path.of(System.getProperty("user.home"), "pianokeys", "my-recordings");
        Files.createDirectories(directory);
        return directory;
    }

    public File save(Composition composition) throws IOException, InvalidMidiDataException
    {
        Sequence sequence = toSequence(composition);
        String fileName = "recording-" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + ".mid";
        File file = getRecordingsDirectory().resolve(fileName).toFile();
        MidiSystem.write(sequence, 0, file);
        return file;
    }

    private Sequence toSequence(Composition composition) throws InvalidMidiDataException
    {
        Sequence sequence = new Sequence(Sequence.PPQ, TICKS_PER_QUARTER_NOTE);
        Track track = sequence.createTrack();
        track.add(tempoEvent());

        for (Note note : composition.getNoteList())
        {
            track.add(noteEvent(ShortMessage.NOTE_ON, note.key(), secondsToTicks(note.startTime())));
            track.add(noteEvent(ShortMessage.NOTE_OFF, note.key(), secondsToTicks(note.endTime())));
        }

        return sequence;
    }

    private MidiEvent tempoEvent() throws InvalidMidiDataException
    {
        byte[] data = {
                (byte) ((MICROSECONDS_PER_QUARTER_NOTE >> 16) & 0xFF),
                (byte) ((MICROSECONDS_PER_QUARTER_NOTE >> 8) & 0xFF),
                (byte) (MICROSECONDS_PER_QUARTER_NOTE & 0xFF)
        };
        MetaMessage tempoMessage = new MetaMessage();
        tempoMessage.setMessage(0x51, data, data.length);
        return new MidiEvent(tempoMessage, 0);
    }

    private MidiEvent noteEvent(int command, int key, long tick) throws InvalidMidiDataException
    {
        int velocity = command == ShortMessage.NOTE_ON ? NOTE_VELOCITY : 0;
        return new MidiEvent(new ShortMessage(command, 0, key, velocity), tick);
    }

    private long secondsToTicks(double seconds)
    {
        double microsecondsPerTick = MICROSECONDS_PER_QUARTER_NOTE / (double) TICKS_PER_QUARTER_NOTE;
        return Math.round((seconds * 1_000_000) / microsecondsPerTick);
    }
}
