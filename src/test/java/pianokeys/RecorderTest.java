package pianokeys;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecorderTest
{

    @Test
    void startNote()
    {
        // given
        Composition composition = new Composition();
        Recorder recorder = new Recorder(composition);

        // when
        recorder.startNote(60);

        // then
        assertEquals(0, composition.getNoteList().size());
    }

    @Test
    void stopNote()
    {
        // given
        Composition composition = new Composition();
        Recorder recorder = new Recorder(composition);

        // when
        recorder.startNote(60);
        recorder.stopNote();

        // then
        assertEquals(1, composition.getNoteList().size());
        assertEquals(60, composition.getNoteList().get(0).key());
    }

    @Test
    void reset()
    {
        // given
        Composition composition = new Composition();
        Recorder recorder = new Recorder(composition);
        recorder.startNote(60);
        recorder.stopNote();

        // when
        recorder.reset();
        recorder.startNote(62);
        recorder.stopNote();

        // then
        assertEquals(0, composition.getNoteList().get(1).startTime());
        assertTrue(composition.getNoteList().get(0).endTime() > 0);
    }
}