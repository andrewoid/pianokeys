package pianokeys;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PianoControllerTest
{
    CompositionView compositionView = mock();
    PianoSound sound = mock();
    PianoView pianoView = mock();
    Composition composition = new Composition();

    @Test
    void playNote()
    {
        // given
        PianoController controller = new PianoController(compositionView, sound, composition, pianoView);

        // when
        controller.playNote(60);

        // then
        verify(sound).playNote(60);
        verify(pianoView).showKeyPlayed(60, true);
    }

    @Test
    void stopNote()
    {
        // given
        PianoController controller = new PianoController(compositionView, sound, composition, pianoView);

        // when
        controller.stopNote(60);

        // then
        verify(sound).stopNote(60);
        verify(pianoView).showKeyPlayed(60, false);
        verify(compositionView).refreshLayout();
    }

    @Test
    void playComposition()
    {
        // given
        PianoController controller = new PianoController(compositionView, sound, composition, pianoView);

        // when
        boolean result = controller.playComposition();

        // then
        assertTrue(result);
    }

    @Test
    void playCompositionWhenAlreadyPlaying()
    {
        // given
        PianoController controller = new PianoController(compositionView, sound, composition, pianoView);
        controller.playComposition();

        // when
        boolean result = controller.playComposition();

        // then
        assertFalse(result);
    }

    @Test
    void stopComposition()
    {
        // given
        PianoController controller = new PianoController(compositionView, sound, composition, pianoView);
        controller.playComposition();

        // when
        controller.stopComposition();

        // then
        assertTrue(controller.playComposition());
    }

    @Test
    void eraseComposition()
    {
        // given
        PianoController controller = new PianoController(compositionView, sound, composition, pianoView);
        composition.addNote(new Note(60, 0, 0.125));

        // when
        controller.eraseComposition();

        // then
        assertEquals(0, composition.getNoteList().size());
        verify(compositionView).repaint();
    }

    @Test
    void restartComposition()
    {
        // given
        PianoController controller = new PianoController(compositionView, sound, composition, pianoView);

        // when
        controller.restartComposition();

        // then
        verify(compositionView).setCurrentTime(0);
    }

    @Test
    void changeInstrument()
    {
        // given
        PianoController controller = new PianoController(compositionView, sound, composition, pianoView);

        // when
        controller.changeInstrument("Piano");

        // then
        verify(sound).setInstrument("Piano");
    }

    @Test
    void changeInstrumentWithNull()
    {
        // given
        PianoController controller = new PianoController(compositionView, sound, composition, pianoView);

        // when
        controller.changeInstrument(null);

        // then
        verify(sound, never()).setInstrument(any());
    }

    @Test
    void setRecording()
    {
        // given
        PianoController controller = new PianoController(compositionView, sound, composition, pianoView);

        // when
        controller.setRecording(false);
        controller.playNote(60);
        controller.stopNote(60);

        // then
        assertEquals(0, composition.getNoteList().size());
    }
}