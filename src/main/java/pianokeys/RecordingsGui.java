package pianokeys;

import javax.swing.*;
import java.util.function.Supplier;

public class RecordingsGui extends JFrame
{
    public RecordingsGui(Supplier<PianoController> pianoController)
    {
        setUpFrame();
        add(new RecordingsPanel(pianoController));
    }

    private void setUpFrame()
    {
        setTitle("My Recordings");
        // hide instead of exit so closing this window doesn't close the piano
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setSize(600, 500);
    }
}
