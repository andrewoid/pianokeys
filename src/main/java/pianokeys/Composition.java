package pianokeys;

import javax.sound.midi.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static pianokeys.Note.TIME_STEP;
import static pianokeys.PianoSound.C4;
import static pianokeys.PianoSound.D4;
import static pianokeys.PianoSound.E4;
import static pianokeys.PianoSound.F4;
import static pianokeys.PianoSound.G4;

public class Composition
{
    private String name;
    private int id;

    public static final Composition ODE_TO_JOY = new SimpleCompositionFactory()
            .toComposition(new int[]{E4, E4, F4, G4, G4, F4, E4, D4,
                            C4, C4, D4, E4, E4, D4, D4,
                            E4, E4, F4, G4, G4, F4, E4, D4,
                            C4, C4, D4, E4, D4, C4, C4,
                            D4, D4, E4, C4, D4, E4, F4, E4, C4,
                            D4, E4, F4, E4, D4, C4, D4, G4,
                            E4, E4, F4, G4, G4, F4, E4, D4,
                            C4, C4, D4, E4, D4, C4, C4},
                    TIME_STEP * 4);

    private final ArrayList<Note> noteList = new ArrayList<>();

    public Composition() {
        id = new Random().nextInt();
    }

    public Composition(String name)
    {
        this.name = name;
        id = new Random().nextInt();
    }

    public static Composition fromMidiResource(String resourceName)
    {
        Composition composition = new Composition(resourceName);
        try (InputStream input = Composition.class.getResourceAsStream("/" + resourceName))
        {
            if (input == null)
            {
                throw new IOException("MIDI resource not found: " + resourceName);
            }
            composition.addNotes(readNotes(MidiSystem.getSequence(input)));
            return composition;
        } catch (IOException | InvalidMidiDataException e)
        {
            throw new IllegalStateException("Unable to load MIDI resource " + resourceName, e);
        }
    }

    private static List<Note> readNotes(Sequence sequence)
    {
        if (sequence.getDivisionType() != Sequence.PPQ)
        {
            throw new IllegalArgumentException("Only PPQ MIDI sequences are supported");
        }
        Map<Long, Integer> tempos = new TreeMap<>();
        Map<Long, List<MidiEvent>> eventsByTick = new TreeMap<>();
        for (Track track : sequence.getTracks())
        {
            for (int i = 0; i < track.size(); i++)
            {
                MidiEvent event = track.get(i);
                eventsByTick.computeIfAbsent(event.getTick(), ignored -> new ArrayList<>()).add(event);
                MidiMessage message = event.getMessage();
                if (message instanceof MetaMessage meta && meta.getType() == 0x51
                        && meta.getData().length == 3)
                {
                    byte[] data = meta.getData();
                    tempos.put(event.getTick(), ((data[0] & 0xff) << 16)
                            | ((data[1] & 0xff) << 8) | (data[2] & 0xff));
                }
            }
        }
        List<Note> notes = new ArrayList<>();
        Map<Integer, Deque<Double>> activeNotes = new HashMap<>();
        for (Map.Entry<Long, List<MidiEvent>> entry : eventsByTick.entrySet())
        {
            double time = midiTimeSeconds(entry.getKey(), sequence.getResolution(), tempos);
            for (MidiEvent event : entry.getValue())
            {
                if (!(event.getMessage() instanceof ShortMessage message))
                {
                    continue;
                }
                int key = message.getData1();
                if (message.getCommand() == ShortMessage.NOTE_ON && message.getData2() > 0)
                {
                    activeNotes.computeIfAbsent(key, ignored -> new ArrayDeque<>()).addLast(time);
                } else if (message.getCommand() == ShortMessage.NOTE_OFF
                        || (message.getCommand() == ShortMessage.NOTE_ON && message.getData2() == 0))
                {
                    Deque<Double> starts = activeNotes.get(key);
                    if (starts != null && !starts.isEmpty())
                    {
                        notes.add(new Note(key, starts.removeFirst(), time));
                    }
                }
            }
        }
        notes.sort(Comparator.comparingDouble(Note::startTime));
        return notes;
    }

    private static double midiTimeSeconds(long tick, int resolution, Map<Long, Integer> tempos)
    {
        long previousTick = 0;
        int microsecondsPerQuarter = 500000;
        double microseconds = 0;
        for (Map.Entry<Long, Integer> tempo : tempos.entrySet())
        {
            if (tempo.getKey() > tick)
            {
                break;
            }
            microseconds += (tempo.getKey() - previousTick) * (double) microsecondsPerQuarter / resolution;
            previousTick = tempo.getKey();
            microsecondsPerQuarter = tempo.getValue();
        }
        microseconds += (tick - previousTick) * (double) microsecondsPerQuarter / resolution;
        return microseconds / 1_000_000.0;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public ArrayList<Note> getNoteList()
    {
        return noteList;
    }

    public static final List<Note> furElise = Arrays.asList(
            new Note(PianoSound.E5, 0.0, 0.25),
            new Note(PianoSound.D5_SHARP, 0.25, 0.5),
            new Note(PianoSound.E5, 0.5, 0.75),
            new Note(PianoSound.D5_SHARP, 0.75, 1.0),
            new Note(PianoSound.E5, 1.0, 1.25),
            new Note(PianoSound.B4, 1.25, 1.5),
            new Note(PianoSound.D5, 1.5, 1.75),
            new Note(PianoSound.C5, 1.75, 2.0),
            new Note(PianoSound.A4, 2.0, 2.25),
            new Note(PianoSound.C4, 2.25, 2.5),
            new Note(PianoSound.E4, 2.5, 2.75),
            new Note(PianoSound.A4, 2.75, 3.0),
            new Note(PianoSound.B4, 3.0, 3.25),
            new Note(PianoSound.E4, 3.25, 3.5),
            new Note(PianoSound.G4_SHARP, 3.5, 3.75),
            new Note(PianoSound.B4, 3.75, 4.0),
            new Note(PianoSound.C5, 4.0, 4.25),
            new Note(PianoSound.E5, 4.25, 4.5),
            new Note(PianoSound.D5_SHARP, 4.5, 4.75),
            new Note(PianoSound.E5, 4.75, 5.0),
            new Note(PianoSound.D5_SHARP, 5.0, 5.25),
            new Note(PianoSound.E5, 5.25, 5.5),
            new Note(PianoSound.B4, 5.5, 5.75),
            new Note(PianoSound.D5, 5.75, 6.0),
            new Note(PianoSound.C5, 6.0, 6.25),
            new Note(PianoSound.A4, 6.25, 6.5),
            new Note(PianoSound.C4, 6.5, 6.75),
            new Note(PianoSound.E4, 6.75, 7.0),
            new Note(PianoSound.A4, 7.0, 7.25),
            new Note(PianoSound.B4, 7.25, 7.5),
            new Note(PianoSound.E4, 7.5, 7.75),
            new Note(PianoSound.C5, 7.75, 8.0),
            new Note(PianoSound.B4, 8.0, 8.25),
            new Note(PianoSound.A4, 8.25, 8.5)
    );

    // single note
    public void addNote(Note note)
    {
        noteList.add(note);
    }

    // multiple notes
    public void addNotes(Note... notes)
    {
        noteList.addAll(Arrays.stream(notes).toList());
    }

    public void addNotes(List<Note> list)
    {
        noteList.addAll(list);
    }

    public boolean removeNote(double clickedTime, int clickedKey)
    {
        // Use iterator to make sure the note is safely removed
        Iterator<Note> iterator = getNoteList().iterator();

        // Find the note that was clicked
        while (iterator.hasNext())
        {
            Note note = iterator.next();
            if (note.key() == clickedKey
                    && clickedTime >= note.startTime()
                    && clickedTime <= note.endTime())
            {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    // returns duration
    public double duration()
    {
        double duration = 0;
        for (Note note : getNoteList())
        {
            if (note.endTime() > duration)
            {
                duration = note.endTime();
            }
        }
        return duration;
    }
}

