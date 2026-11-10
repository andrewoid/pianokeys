package pianokeys;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class CompositionTest
{
    @Test
    void addNote()
    {
        // given
        Composition composition = new Composition();
        Note note = new Note(69, 1, 1.25);

        // when
        composition.addNote(note);

        // then
        assertTrue(composition.getNoteList().contains(note));
    }

    @Test
    void addNotes()
    {
        // given
        Composition composition = new Composition();
        Note note1 = new Note(71, 1, 1.25);
        Note note2 = new Note(60, 3.45, 4);
        List<Note> notes = Arrays.asList(note1, note2);

        // when
        composition.addNotes(note1, note2);

        // then
        assertEquals(composition.getNoteList(), notes);
    }

    @Test
    void removeNote()
    {
        // given
        Composition composition = new Composition();
        Note note1 = new Note(71, 1, 1.25);
        Note note2 = new Note(60, 3.45, 4);
        composition.addNotes(note1, note2);

        // when
        boolean removed = composition.removeNote(1.1, 71);

        // then
        assertTrue(removed);
        assertEquals(1, composition.getNoteList().size());
        assertTrue(composition.getNoteList().contains(note2));
    }

    @Test
    void fromMidiBytes() throws Exception
    {
        // given
        byte[] midi;
        try (InputStream input = getClass().getResourceAsStream("/twinkle-twinkle-little-star.mid"))
        {
            midi = input.readAllBytes();
        }

        // when
        Composition composition = new Composition().fromMidiBytes("Twinkle", midi);

        // then
        assertEquals("Twinkle", composition.getName());
        assertFalse(composition.getNoteList().isEmpty());
    }

    @Test
    void fromMidiBytesNotMidi()
    {
        // given
        byte[] notMidi = "<html></html>".getBytes();

        // when
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new Composition().fromMidiBytes("page", notMidi));

        // then
        assertEquals("page is not a MIDI file", error.getMessage());
    }
}