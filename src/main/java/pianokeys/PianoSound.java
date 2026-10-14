package pianokeys;

import javax.sound.midi.*;
public class PianoSound
{
    // MIDI note constants
    public final int c4 = 60;
    public final int d4 = 62;
    public final int e4 = 64;
    public final int f4 = 65;
    public final int g4 = 67;
    public final int a4 = 69;
    public final int b4 = 71;
    public final int c5 = 72;
    public final int d5 = 74;
    public final int d5Sharp = 75;
    public final int e5 = 76;

    // Black-key (sharp) note values.
    public final int c4Sharp = 61;
    public final int d4Sharp = 63;
    public final int f4Sharp = 66;
    public final int g4Sharp = 68;
    public final int a4Sharp = 70;

    private final Synthesizer synthesizer;
    private final MidiChannel channel;
    private final MidiChannel songChannel;

    public final int[] whiteNotes = {c4, d4, e4, f4, g4, a4, b4, c5};
    public final int[] blackNotes = {c4Sharp, d4Sharp, -1, f4Sharp, g4Sharp, a4Sharp, -1};
    public final String[] instruments = {"piano", "guitar", "violin", "trumpet"};

    public PianoSound(Synthesizer synthesizer, MidiChannel channel) throws MidiUnavailableException
    {
        this(synthesizer, channel, channel);
    }

    public PianoSound(Synthesizer synthesizer, MidiChannel channel, MidiChannel songChannel)
            throws MidiUnavailableException
    {
        this.synthesizer = synthesizer;
        this.channel = channel;
        this.songChannel = songChannel;
    }

    public MidiChannel getChannel()
    {
        return channel;
    }

    public Synthesizer getSynthesizer()
    {
        return synthesizer;
    }

    public void playNote(int note)
    {
        channel.noteOn(note, 127); // velocity 127 (max volume)
    }

    public void stopNote(int note)
    {
        channel.noteOff(note);
    }

    public void playSongNote(int note)
    {
        songChannel.noteOn(note, 90);
    }

    public void stopSongNote(int note)
    {
        songChannel.noteOff(note);
    }

    public void silenceSong()
    {
        songChannel.allNotesOff();
    }

    public void cleanup()
    {
        synthesizer.close();
    }

    //https://midiprog.com/program-numbers/
    public void setInstrument(String instrumentName)
    {
        switch (instrumentName)
        {
            case "piano" -> channel.programChange(0);
            case "guitar" -> channel.programChange(24);
            case "violin" -> channel.programChange(40);
            case "trumpet" -> channel.programChange(56);
            default -> channel.programChange(0); //default: piano
        }
    }
}