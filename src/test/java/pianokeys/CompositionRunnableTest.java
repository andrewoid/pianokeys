package pianokeys;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class CompositionRunnableTest
{

    @Test
    public void run()
    {
        //given
        PianoController controller = mock();
        Composition comp = new Composition();
        comp.addNote(new Note(60, 0.0, 0.25));
        comp.addNote(new Note(62, 0.25, 0.5));
        CompositionRunnable runnable = new CompositionRunnable(controller, comp, 0.0);

        // when
        runnable.run();

        //then
        verify(controller).playPlaybackNote(60);
        verify(controller, atLeastOnce()).stopPlaybackNote(60);
        verify(controller).playPlaybackNote(62);
        verify(controller, atLeastOnce()).stopPlaybackNote(62);
    }

}