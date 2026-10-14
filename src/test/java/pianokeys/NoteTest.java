package pianokeys;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoteTest
{

    @Test
    public void roundToNearestEighth()
    {
        // then
        Note helper = new Note(0, 0, 0);
        assertEquals(0, helper.roundToNearestEighth(0));
        assertEquals(.125, helper.roundToNearestEighth(.12));
        assertEquals(.125, helper.roundToNearestEighth(.13));
        assertEquals(.125, helper.roundToNearestEighth(.18));
        assertEquals(.25, helper.roundToNearestEighth(.20));
        assertEquals(1.0, helper.roundToNearestEighth(.99));
    }

    @Test
    void getName()
    {
        Note noteC5 = new Note(72, 0.0, 0.125);
        assertEquals("C5", noteC5.getName());

        Note noteE5 = new Note(76, 0.0, 0.125);
        assertEquals("E5", noteE5.getName());

        Note noteB4 = new Note(71, 0.0, 0.125);
        assertEquals("B4", noteB4.getName());

        Note noteDsharp5 = new Note(75, 0.0, 0.125);
        assertEquals("D#5", noteDsharp5.getName());
    }

}