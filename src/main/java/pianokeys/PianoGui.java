
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

    private final int whiteKeyWidth = 50;
    private final int whiteKeyHeight = 250;
    private final int octaves = 7;

    private static final int HOME_OCTAVE_INDEX = 3;

    private PianoSound sound;
    private PianoController controller;
    private BitMidiSearchGui bitMidiSearchGui;

    public PianoGui()
    {
        initMidi();
        midiCleanup();
        setUpFrame();

        JPanel contentPanel = new JPanel(new BorderLayout());
        setContentPane(contentPanel);

        Composition composition = new Composition()
                .fromMidiResource("twinkle-twinkle-little-star.mid");

        CompositionView compositionView = new CompositionView(composition);
        FallingNotesController fallingNotesController =
                new FallingNotesController(composition);

        Supplier<PianoController> controllerSupplier = () -> controller;

        PianoView pianoView = new PianoView(controllerSupplier, sound);
        JScrollPane pianoScrollPane = createScrollPane(pianoView);

        controller = new PianoController(
                compositionView, sound, composition, pianoView);

        controller.setFallingNotesController(fallingNotesController);

        new KeyboardInputHandler(
                controllerSupplier,
                pianoView,
                sound,
                octaveIndex -> centerOnOctave(
                        pianoScrollPane, pianoView, octaveIndex))
                .registerOn(getRootPane());

        contentPanel.add(pianoScrollPane, BorderLayout.SOUTH);

        JScrollPane compositionScrollPane = new JScrollPane(
                compositionView,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        );

        compositionScrollPane.setPreferredSize(new Dimension(800, 200));

        FallingNotesView fallingNotesView = fallingNotesController.getView();
        contentPanel.add(fallingNotesView, BorderLayout.CENTER);

        JButton play = new JButton(
                new ImageIcon(getClass().getResource("/images/play.png")));

        play.addActionListener(e ->
        {
            if (controller.playComposition())
            {
                play.setIcon(new ImageIcon(
                        getClass().getResource("/images/pause.jpeg")));
            } else {
                controller.stopComposition();
                play.setIcon(new ImageIcon(
                        getClass().getResource("/images/play.png")));
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout());

        JLabel songTitle = new JLabel(
                "Twinkle Twinkle Little Star", SwingConstants.CENTER);

        songTitle.setFont(new Font("Arial", Font.BOLD, 24));
        topPanel.add(songTitle, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER));

        JPanel playPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER));

        playPanel.add(play);
        buttonPanel.add(playPanel);

        JComboBox<String> instrumentDropdown =
                new JComboBox<>(sound.instruments);

        instrumentDropdown.setFont(new Font("Arial", Font.BOLD, 16));
        instrumentDropdown.setBackground(Color.WHITE);
        instrumentDropdown.setForeground(Color.BLACK);
        instrumentDropdown.setBorder(
                BorderFactory.createLineBorder(Color.GRAY, 2));

        instrumentDropdown.addActionListener(e ->
                controller.changeInstrument(
                        (String) instrumentDropdown.getSelectedItem()));

        buttonPanel.add(instrumentDropdown);

        // SPEED CONTROL
        final JPanel speedPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel speedLabel = new JLabel("Speed: 1.0x");

        JSlider speedSlider = new JSlider(50, 200, 100);
        speedSlider.setPreferredSize(new Dimension(160, 40));
        speedSlider.setMajorTickSpacing(50);
        speedSlider.setPaintTicks(true);

        speedSlider.addChangeListener(e ->
        {
            double speed = speedSlider.getValue() / 100.0;

            controller.setPlaybackSpeed(speed);

            speedLabel.setText(
                    String.format("Speed: %.2fx", speed));
        });

        speedPanel.add(speedLabel);
        speedPanel.add(speedSlider);
        buttonPanel.add(speedPanel);
        // Keep the label and slider synchronized so the current percentage is visible.
        JLabel volumeLabel = new JLabel("Volume: 100%");
        JSlider volumeSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, sound.getVolume());
        volumeSlider.setPreferredSize(new Dimension(180, volumeSlider.getPreferredSize().height));
        volumeSlider.addChangeListener(e ->
        {
            int volume = volumeSlider.getValue();
            // Apply changes immediately to both live notes and song playback.
            sound.setVolume(volume);
            volumeLabel.setText("Volume: " + volume + "%");
        });
        buttonPanel.add(volumeLabel);
        buttonPanel.add(volumeSlider);

        JButton bitMidiSearch = new JButton("Search BitMidi");

        bitMidiSearch.addActionListener(e -> openBitMidiSearch());
        buttonPanel.add(bitMidiSearch);

        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        contentPanel.add(topPanel, BorderLayout.NORTH);

        fallingNotesController.setFinishedListener(() ->
                play.setIcon(new ImageIcon(
                        getClass().getResource("/images/play.png"))));

        contentPanel.revalidate();
        contentPanel.repaint();

        centerOnOctave(
                pianoScrollPane, pianoView, HOME_OCTAVE_INDEX);
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

            sound = new PianoSound(
                    synthesizer, channels[0], channels[1]);
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
        return new JScrollPane(
                layeredPane,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
    }

    private void centerOnOctave(
            JScrollPane scrollPane,
            PianoView pianoView,
            int octaveIndex)
    {
        SwingUtilities.invokeLater(() ->
        {
            int octaveStartX =
                    pianoView.getOctaveStartX(octaveIndex);

            int targetX = octaveStartX
                    - (scrollPane.getViewport().getWidth() / 2)
                    + (pianoView.getWhiteKeyWidth() / 2);

            targetX = Math.max(0, Math.min(
                    targetX,
                    scrollPane.getHorizontalScrollBar().getMaximum()
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
