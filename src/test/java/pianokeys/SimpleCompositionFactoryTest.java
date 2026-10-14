package pianokeys;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
class SimpleCompositionFactoryTest
{
    private final double timeStep = 1 / 8.0;

    @Test
    public void toComposition()
    {
        // given
        var factory = new SimpleCompositionFactory();

        // when
        var composition = factory.toComposition(
                new int[]{
                        60,
                        60,
                        64,
                        62
                },
                timeStep
        );

        // then
        List<Note> notes = composition.getNoteList();
        assertEquals(new Note(60, 0, timeStep), notes.get(0));
        assertEquals(new Note(60, timeStep, .25), notes.get(1));
        assertEquals(new Note(64, .25, .375), notes.get(2));
        assertEquals(new Note(62, .375, .50), notes.get(3));
    }

}