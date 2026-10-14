package pianokeys;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PianoSoundTest
{
    MidiChannel channel = mock();
    Synthesizer synthesizer = mock();

    @Test
    void playNote() throws MidiUnavailableException
    {
        // given
        PianoSound piano = new PianoSound(synthesizer, channel);

        // when
        piano.playNote(piano.c4);

        // then
        verify(channel).noteOn(piano.c4, 127);
    }

    @Test
    void stopNote() throws MidiUnavailableException
    {
        // given
        PianoSound piano = new PianoSound(synthesizer, channel);

        // when
        piano.stopNote(piano.c4);

        // then
        verify(channel).noteOff(piano.c4);
    }

    @Test
    void cleanup() throws MidiUnavailableException
    {
        // given
        PianoSound piano = new PianoSound(synthesizer, channel);

        // when
        piano.cleanup();

        // then
        verify(synthesizer).close();
    }


}