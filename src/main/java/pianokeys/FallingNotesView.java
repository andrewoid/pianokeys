package pianokeys;

import javax.swing.*;
import java.awt.*;

public class FallingNotesView extends JPanel
{
    private final FallingNotesController controller;

    public FallingNotesView(FallingNotesController controller)
    {
        this.controller = controller;
    }

    @Override
    protected void paintComponent(Graphics graphics)
    {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        int hitY = getHeight() - 1;
        for (Note note : controller.getComposition().getNoteList())
        {
            if (controller.getCurrentTime() > note.endTime())
            {
                continue;
            }
            double noteHeight = controller.getVisualHeight(note);
            double bottom = hitY
                    - (note.startTime() - controller.getCurrentTime())
                    * controller.getPixelsPerSecond();
            int x = controller.noteXcoordinate(note.key());
            int y = (int) Math.round(bottom - noteHeight);
            int height = (int) Math.round(noteHeight);
            if (y + height >= 0 && y <= hitY)
            {
                boolean blackKey = controller.isBlackNote(note.key());
                int width = blackKey ? 18 : 26;
                int noteOffset = blackKey ? 0 : 2;
                g.setColor(controller.getNoteColor());
                g.fillRoundRect(x + noteOffset, y, width, height, 8, 8);
            }
        }
        g.setColor(Color.ORANGE);
        g.fillRect(0, hitY - 2, getWidth(), 2);
        g.dispose();
    }
}
