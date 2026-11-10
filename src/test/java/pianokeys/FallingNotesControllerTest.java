package pianokeys;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FallingNotesControllerTest
{
    @Test
    void loadComposition()
    {
        // given
        Composition composition = new Composition();
        composition.addNote(new Note(60, 0, 1));
        FallingNotesController controller = new FallingNotesController(composition);
        controller.start();
        Note newNote = new Note(62, 0, 2);
        Composition song = new Composition("song");
        song.addNote(newNote);

        // when
        controller.loadComposition(song);

        // then
        assertEquals(List.of(newNote), composition.getNoteList());
        assertFalse(controller.isRunning());
        // the new note should be drawn the same height as if the app had started with this song
        Composition startedWithSong = new Composition();
        startedWithSong.addNote(newNote);
        double expectedHeight = new FallingNotesController(startedWithSong).getVisualHeight(newNote);
        assertEquals(expectedHeight, controller.getVisualHeight(newNote));
    }
}