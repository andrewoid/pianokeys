package pianokeys;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Supplier;

public class PianoGui extends JFrame
{
    private final String[] whiteKeyNames = {"C", "D", "E", "F", "G", "A", "B"};

    // White keys
    private final int whiteKeyWidth = 50;
    private final int whiteKeyHeight = 250;

    private final int octaves = 7;

    // index of the keyboard-shortcut home octave, matching KeyboardInputHandler's un-shifted mapping
    private static final int HOME_OCTAVE_INDEX = 3;

    //dropdown to change instrument
    // MIDI sound system
    private PianoSound sound;

    private PianoController controller;

    // window for searching BitMidi, created the first time it is opened
    private BitMidiSearchGui bitMidiSearchGui;

    public PianoGui()
    {
        initMidi();
        midiCleanup();
        setUpFrame();
        JPanel contentPanel = new JPanel(new BorderLayout());
        setContentPane(contentPanel);

        Composition composition = new Composition().fromMidiResource("twinkle-twinkle-little-star.mid");
        CompositionView compositionView = new CompositionView(composition);
        FallingNotesController fallingNotesController = new FallingNotesController(composition);

        // Because PianoController needs to access PianoView and PianoView needs PianoController
        // this creates a circular dependency. One way to fix this is to create a Supplier that is passed
        // to PianoView which will return the PianoController after it's created after PianoView is instantiated.
        Supplier<PianoController> controllerSupplier = () -> controller;

        PianoView pianoView = new PianoView(controllerSupplier, sound);
        JScrollPane pianoScrollPane = createScrollPane(pianoView);
        // Adding the controller object
        controller = new PianoController(compositionView, sound, composition, pianoView);
        controller.setFallingNotesController(fallingNotesController);

        new KeyboardInputHandler(controllerSupplier, pianoView, sound,
                octaveIndex -> centerOnOctave(pianoScrollPane, pianoView, octaveIndex))
                .registerOn(getRootPane());

        contentPanel.add(pianoScrollPane, BorderLayout.SOUTH);
        // vertical and  horizontal scroll panes for CompositionView
        JScrollPane compositionScrollPane = new JScrollPane(
                compositionView,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        );
        compositionScrollPane.setPreferredSize(new Dimension(800, 200));

        FallingNotesView fallingNotesView = fallingNotesController.getView();
        contentPanel.add(fallingNotesView, BorderLayout.CENTER);

        // buttons panel
        JButton play = new JButton(new ImageIcon(getClass().getResource("/images/play.png")));
        play.addActionListener(e ->
        {
            if (controller.playComposition())
            {
                play.setIcon(new ImageIcon(getClass().getResource("/images/pause.jpeg")));
            } else
            {
                controller.stopComposition();
                play.setIcon(new ImageIcon(getClass().getResource("/images/play.png")));
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel songTitle = new JLabel("Twinkle Twinkle Little Star", SwingConstants.CENTER);
        songTitle.setFont(new Font("Arial", Font.BOLD, 24));
        topPanel.add(songTitle, BorderLayout.NORTH);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JPanel playPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        playPanel.add(play);
        buttonPanel.add(playPanel);

        JComboBox<String> instrumentDropdown = new JComboBox<>(sound.instruments);
        instrumentDropdown.setFont(new Font("Arial", Font.BOLD, 16));
        instrumentDropdown.setBackground(Color.WHITE);
        instrumentDropdown.setForeground(Color.BLACK);
        instrumentDropdown.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        instrumentDropdown.addActionListener(e ->
                controller.changeInstrument((String) instrumentDropdown.getSelectedItem()));
        buttonPanel.add(instrumentDropdown);

        JButton bitMidiSearch = new JButton("Search BitMidi");
        bitMidiSearch.addActionListener(e -> openBitMidiSearch());
        buttonPanel.add(bitMidiSearch);

        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        contentPanel.add(topPanel, BorderLayout.NORTH);
        fallingNotesController.setFinishedListener(() ->
                play.setIcon(new ImageIcon(getClass().getResource("/images/play.png"))));
        contentPanel.revalidate();
        contentPanel.repaint();
        centerOnOctave(pianoScrollPane, pianoView, HOME_OCTAVE_INDEX);

    }

    private void setUpFrame()
    {
        setTitle("Piano Keys");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);
    }

    private void initMidi()
    {
        try
        {
            Synthesizer synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            MidiChannel[] channels = synthesizer.getChannels();
            sound = new PianoSound(synthesizer, channels[0], channels[1]);
        } catch (MidiUnavailableException e)
        {
            e.printStackTrace();
        }
    }

    private void midiCleanup()
    {
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                if (sound != null)
                {
                    sound.cleanup();
                }
            }
        });
    }


    private JScrollPane createScrollPane(JLayeredPane layeredPane)
    {
        // uses a scroll method so that you can easily see all the keys
        return new JScrollPane(
                layeredPane,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
    }

    private void centerOnOctave(JScrollPane scrollPane, PianoView pianoView, int octaveIndex)
    {
        // Centers the scroll pane's viewport on the given octave's first white key.
        SwingUtilities.invokeLater(() ->
        {
            int octaveStartX = pianoView.getOctaveStartX(octaveIndex);

            int targetX = octaveStartX - (scrollPane.getViewport().getWidth() / 2)
                    + (pianoView.getWhiteKeyWidth() / 2);
            targetX = Math.max(0, Math.min(targetX, scrollPane.getHorizontalScrollBar().getMaximum()
                    - scrollPane.getViewport().getWidth()));

            scrollPane.getHorizontalScrollBar().setValue(targetX);
        });
    }

    private void openBitMidiSearch()
    {
        if (bitMidiSearchGui == null)
        {
            bitMidiSearchGui = new BitMidiSearchGui();
            bitMidiSearchGui.setLocationRelativeTo(this);
        }
        bitMidiSearchGui.setVisible(true);
        bitMidiSearchGui.toFront();
    }
}