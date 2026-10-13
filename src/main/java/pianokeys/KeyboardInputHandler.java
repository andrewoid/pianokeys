package pianokeys;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static pianokeys.PianoSound.C4;

/**
 * Lets the computer keyboard play notes, using the standard "musical typing"
 * layout (as used by GarageBand, Ableton, etc.):
 *
 * Black keys:  W E   T Y U   O P
 * White keys: A S D F G H J K L ;
 *
 * Z / X shift the whole mapping down/up an octave, so every note on the
 * piano is reachable from this one compact cluster of keys.
 *
 * Registers key bindings on the given root pane with WHEN_IN_FOCUSED_WINDOW,
 * so playing works no matter which component currently has focus, and tracks
 * which keys are currently held down so that OS key-repeat doesn't retrigger
 * notes, corrupt recorded durations, or re-shift the octave on every repeat.
 */
public class KeyboardInputHandler
{
    private static final Map<Integer, Integer> KEY_TO_NOTE = new HashMap<>();

    static
    {
        // white keys
        KEY_TO_NOTE.put(KeyEvent.VK_A, C4);
        KEY_TO_NOTE.put(KeyEvent.VK_S, C4 + 2);
        KEY_TO_NOTE.put(KeyEvent.VK_D, C4 + 4);
        KEY_TO_NOTE.put(KeyEvent.VK_F, C4 + 5);
        KEY_TO_NOTE.put(KeyEvent.VK_G, C4 + 7);
        KEY_TO_NOTE.put(KeyEvent.VK_H, C4 + 9);
        KEY_TO_NOTE.put(KeyEvent.VK_J, C4 + 11);
        KEY_TO_NOTE.put(KeyEvent.VK_K, C4 + 12);
        KEY_TO_NOTE.put(KeyEvent.VK_L, C4 + 14);
        KEY_TO_NOTE.put(KeyEvent.VK_SEMICOLON, C4 + 16);

        // black keys
        KEY_TO_NOTE.put(KeyEvent.VK_W, C4 + 1);
        KEY_TO_NOTE.put(KeyEvent.VK_E, C4 + 3);
        KEY_TO_NOTE.put(KeyEvent.VK_T, C4 + 6);
        KEY_TO_NOTE.put(KeyEvent.VK_Y, C4 + 8);
        KEY_TO_NOTE.put(KeyEvent.VK_U, C4 + 10);
        KEY_TO_NOTE.put(KeyEvent.VK_O, C4 + 13);
        KEY_TO_NOTE.put(KeyEvent.VK_P, C4 + 15);
    }

    private static final Map<Integer, String> KEY_TO_LABEL = new HashMap<>();

    static
    {
        KEY_TO_LABEL.put(KeyEvent.VK_A, "A");
        KEY_TO_LABEL.put(KeyEvent.VK_S, "S");
        KEY_TO_LABEL.put(KeyEvent.VK_D, "D");
        KEY_TO_LABEL.put(KeyEvent.VK_F, "F");
        KEY_TO_LABEL.put(KeyEvent.VK_G, "G");
        KEY_TO_LABEL.put(KeyEvent.VK_H, "H");
        KEY_TO_LABEL.put(KeyEvent.VK_J, "J");
        KEY_TO_LABEL.put(KeyEvent.VK_K, "K");
        KEY_TO_LABEL.put(KeyEvent.VK_L, "L");
        KEY_TO_LABEL.put(KeyEvent.VK_SEMICOLON, ";");
        KEY_TO_LABEL.put(KeyEvent.VK_W, "W");
        KEY_TO_LABEL.put(KeyEvent.VK_E, "E");
        KEY_TO_LABEL.put(KeyEvent.VK_T, "T");
        KEY_TO_LABEL.put(KeyEvent.VK_Y, "Y");
        KEY_TO_LABEL.put(KeyEvent.VK_U, "U");
        KEY_TO_LABEL.put(KeyEvent.VK_O, "O");
        KEY_TO_LABEL.put(KeyEvent.VK_P, "P");
    }

    // PianoView renders 7 octaves, with the home octave (this class's un-shifted
    // mapping) at index 3 - so the shift is only valid from -3 to +3.
    private static final int MIN_OCTAVE_SHIFT = -3;
    private static final int MAX_OCTAVE_SHIFT = 3;

    private final Map<Integer, Integer> heldNotes = new HashMap<>();
    private final Set<Integer> heldControlKeys = new HashSet<>();
    private final Supplier<PianoController> controller;
    private final PianoView pianoView;

    private int octaveShift = 0;

    public KeyboardInputHandler(Supplier<PianoController> controller, PianoView pianoView)
    {
        this.controller = controller;
        this.pianoView = pianoView;
    }

    /**
     * Registers one press/release key binding per mapped note key, plus the
     * octave-shift keys, on the given root pane, scoped to WHEN_IN_FOCUSED_WINDOW.
     */
    public void registerOn(JRootPane rootPane)
    {
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        for (Map.Entry<Integer, Integer> entry : KEY_TO_NOTE.entrySet())
        {
            int keyCode = entry.getKey();
            int baseNote = entry.getValue();

            String pressKey = "pianokeys.press." + keyCode;
            String releaseKey = "pianokeys.release." + keyCode;

            inputMap.put(KeyStroke.getKeyStroke(keyCode, 0, false), pressKey);
            inputMap.put(KeyStroke.getKeyStroke(keyCode, 0, true), releaseKey);

            actionMap.put(pressKey, new AbstractAction()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    if (!heldNotes.containsKey(keyCode))
                    {
                        int note = baseNote + octaveShift * 12;
                        heldNotes.put(keyCode, note);
                        controller.get().playNote(note);
                    }
                }
            });

            actionMap.put(releaseKey, new AbstractAction()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Integer note = heldNotes.remove(keyCode);
                    if (note != null)
                    {
                        controller.get().stopNote(note);
                    }
                }
            });
        }

        registerOctaveShiftKey(inputMap, actionMap, KeyEvent.VK_Z, -1);
        registerOctaveShiftKey(inputMap, actionMap, KeyEvent.VK_X, 1);

        updateKeyboardLabels();
    }

    /**
     * Recomputes which note each mapped key currently plays (base note shifted by the
     * current octave) and pushes the corner badges to PianoView to match.
     */
    private void updateKeyboardLabels()
    {
        Map<Integer, String> noteToLabel = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : KEY_TO_NOTE.entrySet())
        {
            int keyCode = entry.getKey();
            int note = entry.getValue() + octaveShift * 12;
            noteToLabel.put(note, KEY_TO_LABEL.get(keyCode));
        }
        pianoView.setKeyboardLabels(noteToLabel);
    }

    private void registerOctaveShiftKey(InputMap inputMap, ActionMap actionMap, int keyCode, int direction)
    {
        String pressKey = "pianokeys.octave.press." + keyCode;
        String releaseKey = "pianokeys.octave.release." + keyCode;

        inputMap.put(KeyStroke.getKeyStroke(keyCode, 0, false), pressKey);
        inputMap.put(KeyStroke.getKeyStroke(keyCode, 0, true), releaseKey);

        actionMap.put(pressKey, new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (heldControlKeys.add(keyCode))
                {
                    int shifted = octaveShift + direction;
                    if (shifted >= MIN_OCTAVE_SHIFT && shifted <= MAX_OCTAVE_SHIFT)
                    {
                        octaveShift = shifted;
                        updateKeyboardLabels();
                    }
                }
            }
        });

        actionMap.put(releaseKey, new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                heldControlKeys.remove(keyCode);
            }
        });
    }
}
