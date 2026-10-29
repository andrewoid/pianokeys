package pianokeys;

import javax.sound.midi.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.io.ByteArrayInputStream;

public class Composition
{
    private String name;
    private int id;

    private final ArrayList<Note> noteList = new ArrayList<>();
    private final double timeStep = 1 / 8.0;

    public Composition() {
        id = new Random().nextInt();
    }

    public Composition(String name)
    {
        this.name = name;
        id = new Random().nextInt();
    }

    public Composition fromMidiResource(String resourceName)
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

    /**
     * Reads a MIDI file that was downloaded, like one from BitMidi
     *
     * @param name the name of the song
     * @param midi the bytes of the .mid file
     * @return a Composition with the notes of the song
     */
    public Composition fromMidiBytes(String name, byte[] midi)
    {
        Composition composition = new Composition(name);
        try
        {
            composition.addNotes(readNotes(MidiSystem.getSequence(new ByteArrayInputStream(midi))));
            return composition;
        } catch (IOException | InvalidMidiDataException e)
        {
            throw new IllegalStateException(name + " is not a MIDI file", e);
        }
    }

    /**
     * Loads a saved recording (or any standard MIDI file) from disk, as opposed to
     * fromMidiResource, which only loads files bundled on the classpath.
     */
    public Composition fromMidiFile(File file)
    {
        Composition composition = new Composition(file.getName());
        try
        {
            composition.addNotes(readNotes(MidiSystem.getSequence(file)));
            return composition;
        } catch (IOException | InvalidMidiDataException e)
        {
            throw new IllegalStateException("Unable to load MIDI file " + file, e);
        }
    }

    private List<Note> readNotes(Sequence sequence)
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

                    // MIDI stores the tempo as 3 bytes: high, middle, low.
                    // & 0xff is 225 in hex, changing each Java byte to its 0-255 MIDI value.
                    // << positions the bytes; | joins them into one number.
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
                    // Store when the note began; its matching end event gives
                    // us the note's duration.
                    activeNotes.computeIfAbsent(key, ignored -> new ArrayDeque<>()).addLast(time);
                } else if (message.getCommand() == ShortMessage.NOTE_OFF
                        || (message.getCommand() == ShortMessage.NOTE_ON && message.getData2() == 0))
                {
                    // Both NOTE_OFF and NOTE_ON with velocity 0 mean "stop."
                    // Match this event with the note's saved start time.
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

    private double midiTimeSeconds(long tick, int resolution, Map<Long, Integer> tempos)
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

        // Add the time since the most recent tempo change.
        microseconds += (tick - previousTick) * (double) microsecondsPerQuarter / resolution;
        return microseconds / 1_000_000.0;
    }

    public String getName()
    {
        return name;
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

    public Composition odeToJoy()
    {
        return new SimpleCompositionFactory().toComposition(
                new int[]{64, 64, 65, 67, 67, 65, 64, 62,
                        60, 60, 62, 64, 64, 62, 62,
                        64, 64, 65, 67, 67, 65, 64, 62,
                        60, 60, 62, 64, 62, 60, 60,
                        62, 62, 64, 60, 62, 64, 65, 64, 60,
                        62, 64, 65, 64, 62, 60, 62, 67,
                        64, 64, 65, 67, 67, 65, 64, 62,
                        60, 60, 62, 64, 62, 60, 60},
                timeStep * 4);
    }

    public List<Note> furElise()
    {
        return Arrays.asList(
                new Note(76, 0.0, 0.25), new Note(75, 0.25, 0.5),
                new Note(76, 0.5, 0.75), new Note(75, 0.75, 1.0),
                new Note(76, 1.0, 1.25), new Note(71, 1.25, 1.5),
                new Note(74, 1.5, 1.75), new Note(72, 1.75, 2.0),
                new Note(69, 2.0, 2.25), new Note(60, 2.25, 2.5),
                new Note(64, 2.5, 2.75), new Note(69, 2.75, 3.0),
                new Note(71, 3.0, 3.25), new Note(64, 3.25, 3.5),
                new Note(68, 3.5, 3.75), new Note(71, 3.75, 4.0),
                new Note(72, 4.0, 4.25), new Note(76, 4.25, 4.5),
                new Note(75, 4.5, 4.75), new Note(76, 4.75, 5.0),
                new Note(75, 5.0, 5.25), new Note(76, 5.25, 5.5),
                new Note(71, 5.5, 5.75), new Note(74, 5.75, 6.0),
                new Note(72, 6.0, 6.25), new Note(69, 6.25, 6.5),
                new Note(60, 6.5, 6.75), new Note(64, 6.75, 7.0),
                new Note(69, 7.0, 7.25), new Note(71, 7.25, 7.5),
                new Note(64, 7.5, 7.75), new Note(72, 7.75, 8.0),
                new Note(71, 8.0, 8.25), new Note(69, 8.25, 8.5));
    }

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
