package pianokeys;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.border.Border;

import static java.awt.Color.*;

public class PianoView extends JLayeredPane
{

    // declared everything
    private final String[] whiteKeyNames = {"C", "D", "E", "F", "G", "A", "B"};
    private final String[] blackKeyNames = {"C#", "D#", "", "F#", "G#", "A#", ""};

    // White keys
    private final int whiteKeyWidth = 30;
    private final int whiteKeyHeight = 250;
    // Black keys
    private final int blackKeyWidth = 18;
    private final int blackKeyHeight = 170;

    private final int octaves = 7;

    private final JButton[] whiteButtons = new JButton[whiteKeyNames.length * octaves];
    private final JButton[] blackButtons = new JButton[5 * octaves];

    private static final Color C_BASE_COLOR = new Color(255, 245, 200);
    private static final Color C_HOVER_COLOR = new Color(255, 235, 180);
    private static final Font KEY_FONT = new Font("Arial", Font.BOLD, 16);
    private static final Border WHITE_KEY_BORDER = BorderFactory.createLineBorder(BLACK, 2);
    private static final Border MIDDLE_C_BORDER = BorderFactory.createLineBorder(RED, 3);
    private static final Border BLACK_KEY_BORDER = BorderFactory.createLineBorder(GRAY, 1);


    private final Supplier<PianoController> controller;
    private final PianoSound sound;

    private final HashMap<Integer, JButton> buttonsByNote = new HashMap<>();

    // which notes currently show a keyboard-shortcut badge, so they can be cleared when the octave shifts
    private final Map<Integer, JLabel> keyboardLabelsByNote = new HashMap<>();

    private static final int KEYBOARD_LABEL_WIDTH = 16;
    private static final int KEYBOARD_LABEL_HEIGHT = 12;
    private static final int KEYBOARD_LABEL_BOTTOM_MARGIN = 2;
    // on white keys, sit just below where the black keys end, rather than at the very
    // bottom of the (much taller) white key, so the badge stays visible on shorter screens
    private static final int WHITE_KEY_LABEL_TOP_MARGIN = 4;

    public PianoView(Supplier<PianoController> controller, PianoSound sound)

    {
        this.controller = controller;
        this.sound = sound;
        JPanel whiteKeysPanel = createWhiteKeysPanel();
        JPanel blackKeysPanel = createBlackKeysPanel();

        int totalWidth = whiteKeyNames.length * octaves * whiteKeyWidth;
        this.setPreferredSize(new Dimension(totalWidth, whiteKeyHeight));

        whiteKeysPanel.setBounds(0, 0, totalWidth, whiteKeyHeight);
        blackKeysPanel.setBounds(0, 0, totalWidth, whiteKeyHeight);

        // using different layers to add the panels
        this.add(whiteKeysPanel, Integer.valueOf(0));
        this.add(blackKeysPanel, Integer.valueOf(1));
    }


    private JPanel createWhiteKeysPanel()
    {
        JPanel whiteKeysPanel = new JPanel(null);
        whiteKeysPanel.setOpaque(true);
        whiteKeysPanel.setBackground(Color.LIGHT_GRAY);

        // nested for loop to run each octave to get 56 white keys
        // switched out i for keyIndex so that we can loop through without them all overlapping
        for (int octave = 0; octave < octaves; octave++)
        {
            for (int i = 0; i < whiteKeyNames.length; i++)
            {
                int note = getWhiteNoteForOctave(octave, i);
                JButton button = createWhitePianoKey(whiteKeyNames[i], note);

                // calculating position across all the indexes not just the first loop
                int keyIndex = octave * whiteKeyNames.length + i;
                whiteButtons[keyIndex] = button;
                buttonsByNote.put(note, button);

                button.setBounds(keyIndex * whiteKeyWidth, 0, whiteKeyWidth, whiteKeyHeight);
                whiteKeysPanel.add(button);
            }
        }
        return whiteKeysPanel;
    }

    private JPanel createBlackKeysPanel()
    {
        JPanel blackKeysPanel = new JPanel();
        blackKeysPanel.setLayout(null);
        blackKeysPanel.setOpaque(false);

        int blackKeyIndex = 0;
        for (int octave = 0; octave < octaves; octave++)
        {
            for (int i = 0; i < blackKeyNames.length; i++)
            {
                if (!blackKeyNames[i].isEmpty())
                {
                    int note = getBlackNoteForOctave(octave, i);
                    if (note == -1)
                    {
                        continue;
                    } // skip keys that don't exist (like between E-F or B-C)
                    JButton button = createBlackPianoKey(blackKeyNames[i], note);

                    blackButtons[blackKeyIndex] = button;

                    int whiteKeyPosition = octave * whiteKeyNames.length + i;

                    // make sure that the black keys are between the white keys
                    int blackKeyX = (whiteKeyPosition * whiteKeyWidth) + whiteKeyWidth - (blackKeyWidth / 2);
                    button.setBounds(blackKeyX, 0, blackKeyWidth, blackKeyHeight);
                    blackKeysPanel.add(button);

                    buttonsByNote.put(note, button);

                    blackKeyIndex++;
                }
            }
        }
        return blackKeysPanel;
    }

    /**
     * @return the pixel X position where the given octave's first (leftmost) white key starts
     */
    public int getOctaveStartX(int octaveIndex)
    {
        return octaveIndex * whiteKeyNames.length * whiteKeyWidth;
    }

    public int getWhiteKeyWidth()
    {
        return whiteKeyWidth;
    }

    private int getWhiteNoteForOctave(int octave, int whiteIndex)
    {
        return sound.whiteNotes[whiteIndex] + (octave - 3) * 12;
    }

    private int getBlackNoteForOctave(int octave, int blackIndex)
    {
        int base = sound.blackNotes[blackIndex];
        if (base == -1)
        {
            return -1;
        }
        return base + (octave - 3) * 12;
    }


    private JButton createWhitePianoKey(String whiteKeyName, int note)
    {
        JButton key = new JButton(whiteKeyName);

        // make it look like a piano key
        key.setForeground(BLACK);
        key.setFont(KEY_FONT);
        key.setFocusPainted(false);
        key.setBorder(WHITE_KEY_BORDER);
        key.setOpaque(true);
        key.setContentAreaFilled(true);

        final Color baseColor;
        final Color hoverColor;

        if (note == sound.c4)
        {
            key.setBorder(BorderFactory.createLineBorder(RED, 3));
            baseColor = C_BASE_COLOR;
            hoverColor = C_HOVER_COLOR;
            key.setBorder(MIDDLE_C_BORDER);
            key.setToolTipText("Middle C (C4)");
        } else
        {
            baseColor = Color.WHITE;
            hoverColor = Color.LIGHT_GRAY;
        }
        key.setBackground(baseColor);

        // hover to show the key
        key.addMouseListener(new MouseAdapter()
        {

            @Override
            public void mouseEntered(MouseEvent evt)
            {
                int modifiers = evt.getModifiersEx();
                if ((modifiers & InputEvent.BUTTON1_DOWN_MASK) != 0)
                {
                    // left mouse button
                    key.setBackground(Color.DARK_GRAY);
                    controller.get().playNote(note);
                } else
                {
                    key.setBackground(hoverColor);
                }
            }

            @Override
            public void mouseExited(MouseEvent evt)
            {
                key.setBackground(baseColor);
                int modifiers = evt.getModifiersEx();
                if ((modifiers & InputEvent.BUTTON1_DOWN_MASK) != 0)
                {
                    // left mouse button
                    controller.get().stopNote(note);
                }
            }

            @Override
            public void mousePressed(MouseEvent evt)
            {
                key.setBackground(Color.DARK_GRAY);
                controller.get().playNote(note);
            }

            @Override
            public void mouseReleased(MouseEvent evt)
            {
                controller.get().stopNote(note);
                if (key.contains(evt.getPoint()))
                {
                    key.setBackground(hoverColor);
                } else
                {
                    key.setBackground(baseColor);
                }
            }

        });

        return key;
    }

    private JButton createBlackPianoKey(String blackKeyName, int note)
    {
        JButton key = new JButton(blackKeyName);
        
        key.setBackground(BLACK);
        key.setForeground(WHITE);
        key.setFont(KEY_FONT);
        key.setFocusPainted(false);
        key.setBorder(BLACK_KEY_BORDER);
        key.setOpaque(true);
        key.setContentAreaFilled(true);

        key.addMouseListener(new MouseAdapter()
        {

            @Override
            public void mouseEntered(MouseEvent evt)
            {
                int modifiers = evt.getModifiersEx();
                if ((modifiers & InputEvent.BUTTON1_DOWN_MASK) != 0)
                {
                    key.setBackground(Color.DARK_GRAY);
                    // left mouse button
                    controller.get().playNote(note);
                } else
                {
                    key.setBackground(Color.LIGHT_GRAY);
                }
            }

            @Override
            public void mouseExited(MouseEvent evt)
            {
                key.setBackground(BLACK);
                int modifiers = evt.getModifiersEx();
                if ((modifiers & InputEvent.BUTTON1_DOWN_MASK) != 0)
                {
                    // left mouse button
                    controller.get().stopNote(note);
                }
            }

            @Override
            public void mousePressed(MouseEvent evt)
            {
                key.setBackground(Color.DARK_GRAY);
                controller.get().playNote(note);
            }

            @Override
            public void mouseReleased(MouseEvent evt)
            {
                controller.get().stopNote(note);
                // Check if mouse is still over the component
                if (key.contains(evt.getPoint()))
                {
                    key.setBackground(Color.LIGHT_GRAY);
                } else
                {
                    key.setBackground(BLACK);
                }
            }
        });

        return key;
    }

    public void showKeyPlayed(int note, boolean pressed)
    {
        JButton button = buttonsByNote.get(note);
        if (button == null)
        {
            return;
        }
        Color releasedColor;
        if (isBlackNote(note))
        {
            releasedColor = BLACK;
        } else
        {
            releasedColor = (note == sound.c4) ? C_BASE_COLOR : Color.WHITE;
        }

        if (pressed)
        {
            button.setBackground(Color.PINK);
        } else
        {
            button.setBackground(releasedColor);
        }
    }

    public void showKeyHit(int note, Color color)
    {
        JButton button = buttonsByNote.get(note);
        if (button == null)
        {
            return;
        }
        button.setBackground(color);
        Timer timer = new Timer(250, event ->
        {
            if (button.getBackground().equals(color))
            {
                showKeyPlayed(note, false);
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * MIDI notes repeat every 12 semitones. Remainder 1, 3, 6, 8, and 10 are the black keys
     * (C#, D#, F#, G#, A#); the rest are white keys.
     */
    private boolean isBlackNote(int note)
    {
        int pitchClass = Math.floorMod(note, 12);
        return pitchClass == 1 || pitchClass == 3 || pitchClass == 6 || pitchClass == 8 || pitchClass == 10;
    }


    /**
     * Shows a small keyboard-shortcut badge at the bottom of each note's key, replacing
     * whichever badges were shown before. Called once at startup and again every time the
     * keyboard octave shifts, so the badges always show what each key currently plays.
     *
     * Bottom placement (rather than a top corner) keeps the badge clear of the area where
     * black keys visually sit on top of white keys, since black keys don't extend all the
     * way down to the bottom of the white keys beneath them.
     */
    public void setKeyboardLabels(Map<Integer, String> noteToKeyLabel)
    {
        for (Map.Entry<Integer, JLabel> entry : keyboardLabelsByNote.entrySet())
        {
            JButton button = buttonsByNote.get(entry.getKey());
            if (button != null)
            {
                button.remove(entry.getValue());
                button.revalidate();
                button.repaint();
            }
        }
        keyboardLabelsByNote.clear();

        for (Map.Entry<Integer, String> entry : noteToKeyLabel.entrySet())
        {
            int note = entry.getKey();
            JButton button = buttonsByNote.get(note);
            if (button == null)
            {
                continue;
            }

            boolean isBlack = isBlackNote(note);
            int keyWidth = isBlack ? blackKeyWidth : whiteKeyWidth;
            int labelY = isBlack
                    ? blackKeyHeight - KEYBOARD_LABEL_HEIGHT - KEYBOARD_LABEL_BOTTOM_MARGIN
                    : blackKeyHeight + WHITE_KEY_LABEL_TOP_MARGIN;

            JLabel label = createKeyboardLabel(entry.getValue());
            button.setLayout(null);
            button.add(label);
            label.setBounds(
                    (keyWidth - KEYBOARD_LABEL_WIDTH) / 2,
                    labelY,
                    KEYBOARD_LABEL_WIDTH,
                    KEYBOARD_LABEL_HEIGHT);

            keyboardLabelsByNote.put(note, label);
        }
    }

    private JLabel createKeyboardLabel(String text)
    {
        // overridden contains() lets clicks pass through to the button underneath,
        // so the badge is purely visual and doesn't interfere with playing the key
        JLabel label = new JLabel(text)
        {
            @Override
            public boolean contains(int x, int y)
            {
                return false;
            }
        };
        label.setFont(new Font("Arial", Font.BOLD, 9));
        label.setForeground(BLACK);
        label.setOpaque(true);
        label.setBackground(new Color(255, 255, 255, 220));
        label.setBorder(BorderFactory.createLineBorder(GRAY, 1));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

}
