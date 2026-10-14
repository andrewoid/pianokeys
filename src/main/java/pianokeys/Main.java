package pianokeys;

import javax.swing.*;

public class Main
{
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() ->
        {
            PianoGui gui = new PianoGui();
            gui.setVisible(true);
        });
    }
}
