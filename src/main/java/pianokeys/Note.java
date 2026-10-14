package pianokeys;

/**
 * @param key       from PianoSound
 * @param startTime in seconds
 * @param endTime   in seconds
 */

public record Note(int key, double startTime, double endTime)
{
    /**
     * @param time in seconds
     * @return time rounded to the nearest 8th of a second.
     */
    public double roundToNearestEighth(double time)
    {
        return Math.round(time * 8.0) / 8.0;
    }

    public String getName()
    {
        String[] noteNames = {
                "C", "C#", "D", "D#", "E", "F",
                "F#", "G", "G#", "A", "A#", "B"};
        int octave = (key / 12) - 1;  // MIDI note number to octave
        int noteIndex = key % 12;
        return noteNames[noteIndex] + octave;
    }

}
