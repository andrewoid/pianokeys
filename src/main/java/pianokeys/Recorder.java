package pianokeys;

public class Recorder
{

    private final Composition composition;
    private double compositionTimeSeconds;
    private double startTimeMs;
    private int note;
    private final Note timeHelper = new Note(0, 0, 0);

    public Recorder(Composition composition)
    {
        this.composition = composition;
    }

    public void startNote(int note)
    {
        startTimeMs = System.currentTimeMillis();
        this.note = note;
    }

    public void stopNote()
    {
        double endTimeMs = System.currentTimeMillis();
        double deltaTimeSeconds = timeHelper.roundToNearestEighth(
                (endTimeMs - startTimeMs) / 1000.0);
        if (deltaTimeSeconds == 0)
        {
            deltaTimeSeconds = 1 / 8.0;
        }
        composition.addNote(new Note(note,
                compositionTimeSeconds,
                compositionTimeSeconds + deltaTimeSeconds));

        compositionTimeSeconds += deltaTimeSeconds;
    }

    public void reset()
    {
        compositionTimeSeconds = 0;
    }

    public double getCompositionTimeSeconds()
    {
        return compositionTimeSeconds;
    }
}
