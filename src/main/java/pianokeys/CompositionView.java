package pianokeys;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * This displays the notes visually as rectangles.
 * The position on the x-axis is time.
 * The position on the y-axis is where the note is on the scale.
 */
public class CompositionView extends JComponent
{
    /**
     * Width of one second when displaying notes in the View.
     * Increased so that you can see the 8th subdivisions clearly
     */
    private final int secondWidth = 80;

    /**
     * Height of each note. This might need to be a percentage of the height of the View.
     */
    private final int noteHeight = 10;

    /**
     * Width of the current time vertical line
     */
    private final int currentTimeWidth = 2;

    /**
     * Number of subdivisions per second (8 creates 8th note grid lines).
     */
    private final int subdivisions = 8;

    /**
     * Number of notes to display vertically
     * 1 octave is 12 notes including the flats and sharps
     * 2 octaves = 24 notes
     */

    // This will change over time.
    private double currentTime = 0;
    private final Composition composition;

    private final int defaultHeight = 400;
    private final int minSeconds = 4;
    private final double timeStep = 1 / 8.0;

    public CompositionView(Composition composition)
    {
        this.composition = composition;
        setFocusable(true);
        requestFocusInWindow();

        // Use MouseListener for the clicking
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                requestFocusInWindow();

                if (SwingUtilities.isLeftMouseButton(e))
                {
                    if (e.getClickCount() == 1)
                    {
                        // Single click to set current time
                        setTimeAtPoint(e.getPoint());
                    } else if (e.getClickCount() == 2)
                    {
                        // Double click to delete note
                        deleteNoteAtPoint(e.getPoint());
                    }
                }
            }
        });

        //Added KeyListener for arrow key controls for moving time
        addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                switch (e.getKeyCode())
                {
                    case KeyEvent.VK_LEFT:
                        addTime(-timeStep); //move back 1/8th second
                        break;
                    case KeyEvent.VK_RIGHT:
                        addTime(timeStep); //move forward 1/8 second
                        break;
                    default:
                        break;
                }

            }

        });
    }

    public void fitToSeconds(double seconds)
    {
        int width = (Math.max(minSeconds, (int) Math.ceil(seconds)) + 1) * secondWidth;
        setPreferredSize(new Dimension(width, defaultHeight));
        refreshLayout(); // revalidate + repaint
    }

    public void resetToDefaultSize()
    {
        setPreferredSize(null);     // ← hand control back to getPreferredSize()
        refreshLayout();
    }

    // Helper method to get the unique keys sorted
    private List<Integer> getUniqueSortedKeys()
    {
        Set<Integer> uniqueKeys = new HashSet<>();
        for (Note note : composition.getNoteList())
        {
            uniqueKeys.add(note.key());
        }

        // Convert to a List and sort it once
        List<Integer> sortedKeys = new ArrayList<>(uniqueKeys);
        Collections.sort(sortedKeys);
        return sortedKeys;
    }

    // Use the composition from the composition class
    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        drawTimeGrid(g);

        /*
         * if the composition (array) is full / has notes in it,
         * for each note, it loops through every note in the composition
         * for each note, you call the existing display note method: which has startTime, endTime, key
         */
        List<Integer> uniqueKeys = getUniqueSortedKeys();

        for (Note note : composition.getNoteList())
        {
            displayNote(g, note, uniqueKeys);
        }

        displayCurrentTimeLine(g);
    }

    public Dimension getPreferredSize()
    {
        int secondsShown = Math.max(minSeconds, (int) Math.ceil(composition.duration()) + 1);
        int width = secondsShown * secondWidth;
        return new Dimension(width, defaultHeight);
    }

    // Making the grid so that you can see the time and the note at which point it is clicked
    private void drawTimeGrid(Graphics g)
    {
        Graphics2D g2d = (Graphics2D) g;
        int visibleSeconds = Math.max(1, getWidth() / secondWidth);

        int secondsShown = Math.max(minSeconds, (int) Math.ceil(composition.duration()) + 1);
        int viewHeight = getHeight();

        for (int second = 0; second <= secondsShown; second++)
        {
            int x = second * secondWidth;

            // Draw the main lines
            g2d.setColor(Color.GRAY);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(x, 0, x, viewHeight);

            // Draw the second label
            g2d.setColor(Color.BLACK);
            g2d.drawString(second + "s", x + 2, 12);

            // Draw the subdivisions lines (8ths)
            g2d.setStroke(new BasicStroke(1));

            // loops to draw 7 subdivision lines between each second
            for (int sub = 1; sub < subdivisions; sub++)
            {
                int subX = x + (sub * secondWidth / subdivisions);
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.drawLine(subX, 0, subX, viewHeight);
            }

            // Notes will automatically position themselves in the order so don't need actual lines
        }

    }

    private void displayNote(Graphics g, Note note, List<Integer> uniqueKeys)
    {
        int rowIndex = uniqueKeys.indexOf(note.key());
        int x1 = (int) (note.startTime() * secondWidth);
        int x2 = (int) (note.endTime() * secondWidth);

        // Only display the notes that are there
        int totalNotes = uniqueKeys.size();
        int noteHeight = getHeight() / totalNotes;

        // flips the Y axis so that the higher notes at the top and lower notes on the bottom
        int y1 = getHeight() - (rowIndex + 1) * noteHeight;

        g.setColor(Color.CYAN);
        g.fillRect(x1, y1, x2 - x1, noteHeight);
        g.setColor(Color.BLACK);
        g.drawRect(x1, y1, x2 - x1, noteHeight);

        // Add the note name to the block as it is played
        g.setColor(Color.BLACK);
        String noteLabel = note.getName();
        g.drawString(noteLabel, x1 + 5, y1 + noteHeight / 2 + 5);
    }

    private void displayCurrentTimeLine(Graphics g)
    {
        int currentTimeX = (int) (currentTime * secondWidth);
        g.setColor(Color.ORANGE);
        g.fillRect(currentTimeX, 0, currentTimeWidth, getHeight());
    }

    public double getCurrentTime()
    {
        return currentTime;
    }

    public void setCurrentTime(double currentTime)
    {
        this.currentTime = new Note(0, 0, 0).roundToNearestEighth(currentTime);
        refreshLayout();
    }

    public void addTime(double delta)
    {
        setCurrentTime(this.getCurrentTime() + delta);
    }

    public void refreshLayout()
    {
        revalidate();
        repaint();
    }

    private void deleteNoteAtPoint(Point p)
    {
        // Calculate the time and note from the mouse coordinates
        double clickedTime = (double) p.x / secondWidth;

        List<Integer> uniqueKeys = getUniqueSortedKeys();
        int totalNotes = uniqueKeys.size();
        if (totalNotes == 0)
        {
            return;
        }

        int noteHeight = getHeight() / totalNotes;

        // Flip Y to find which note row was clicked
        int rowIndex = (getHeight() - p.y) / noteHeight;
        if (rowIndex < 0 || rowIndex >= totalNotes)
        {
            return;
        }

        int clickedKey = uniqueKeys.get(rowIndex);

        // calling it from the method in Composition
        if (composition.removeNote(clickedTime, clickedKey))
        {
            repaint();
        }
    }

    private void setTimeAtPoint(Point p)
    {
        double newTime = (double) p.x / secondWidth;

        // Make sure that the time isn't negative
        if (newTime < 0)
        {
            newTime = 0;
        }

        setCurrentTime(newTime);
    }
}
