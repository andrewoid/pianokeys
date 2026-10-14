package pianokeys;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.function.Supplier;

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

    private final Color cBaseColor = new Color(255, 245, 200);
    private final Color cHoverColor = new Color(255, 235, 180);

    private final Supplier<PianoController> controller;
    private final PianoSound sound;

    private final HashMap<Integer, JButton> buttonsByNote = new HashMap<>();

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
        key.setForeground(Color.BLACK);
        key.setFont(new Font("Arial", Font.BOLD, 16));
        key.setFocusPainted(false);
        key.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        key.setOpaque(true);
        key.setContentAreaFilled(true);

        final Color baseColor;
        final Color hoverColor;

        if (note == sound.c4)
        {
            baseColor = cBaseColor;
            hoverColor = cHoverColor;
            key.setBorder(BorderFactory.createLineBorder(Color.RED, 3));
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
        key.setBackground(Color.BLACK);
        key.setForeground(Color.WHITE);
        key.setFont(new Font("Arial", Font.BOLD, 16));
        key.setFocusPainted(false);
        key.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
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
                key.setBackground(Color.BLACK);
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
                    key.setBackground(Color.BLACK);
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
            releasedColor = Color.BLACK;
        } else
        {
            releasedColor = (note == sound.c4) ? cBaseColor : Color.WHITE;
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

}
