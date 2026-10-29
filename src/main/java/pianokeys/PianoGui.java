
package pianokeys;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PianoGui extends JFrame
{
    private final String[] whiteKeyNames = {"C", "D", "E", "F", "G", "A", "B"};

    private final int whiteKeyWidth = 50;
    private final int whiteKeyHeight = 250;
    private final int octaves = 7;

    private static final int HOME_OCTAVE_INDEX = 3;

    private static final Font TIME_LABEL_FONT = new Font("Arial", Font.PLAIN, 14);

    private PianoSound sound;
    private PianoController controller;
    private BitMidiSearchGui bitMidiSearchGui;
    private RecordingsGui recordingsGui;

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

        // its click listener is added below, once the scrubber it updates exists
        JButton bitMidiSearch = new JButton("Search BitMidi");
        buttonPanel.add(bitMidiSearch);

        JButton recordings = new JButton("My Recordings");
        recordings.addActionListener(e -> openRecordings());
        buttonPanel.add(recordings);

        JButton record = new JButton(new ImageIcon(getClass().getResource("/images/record.png")));
        record.setToolTipText("Click to start recording what you play");
        record.addActionListener(e ->
        {
            if (controller.isCapturingRecording())
            {
                File saved = controller.stopRecordingAndSave();
                record.setBackground(null);
                record.setToolTipText(saved != null
                        ? "Saved as " + saved.getName() + " - click to record again"
                        : "Click to start recording what you play");
            } else
            {
                controller.startRecording();
                record.setBackground(Color.RED);
                record.setToolTipText("Recording - click to stop and save");
            }
        });
        buttonPanel.add(record);

        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        contentPanel.add(topPanel, BorderLayout.NORTH);

        // Scrubber (progress bar): drag up/down to jump earlier or later in the song,
        // the same way you scrub a video or music player. Labels show "how far in" / "total length".
        double songDuration = Math.max(composition.duration(), 0.001);
        // JSlider uses integers; store thousandths of a second for smoother dragging.
        int sliderMax = Math.max(1, (int) Math.round(songDuration * 1000));
        JLabel currentTimeLabel = new JLabel(formatTime(0), SwingConstants.CENTER);
        JLabel durationLabel = new JLabel(formatTime(songDuration), SwingConstants.CENTER);
        currentTimeLabel.setFont(TIME_LABEL_FONT);
        durationLabel.setFont(TIME_LABEL_FONT);
        // Vertical slider: bottom = start of song, top = end (Swing default).
        JSlider seekSlider = new JSlider(JSlider.VERTICAL, 0, sliderMax, 0);
        seekSlider.setPaintTrack(true);
        seekSlider.setPaintTicks(false);
        // When true, the slider is being updated by playback — ignore that as a user drag.
        final boolean[] updatingSlider = {false};
        seekSlider.addChangeListener(e ->
        {
            if (updatingSlider[0])
            {
                return;
            }
            // Convert slider units back to seconds, then jump sound + tiles to that spot.
            double seekSeconds = seekSlider.getValue() / 1000.0;
            currentTimeLabel.setText(formatTime(seekSeconds));
            controller.seekTo(seekSeconds);
        });
        // While the song plays, keep the thumb and "current time" label moving automatically.
        fallingNotesController.setTimeListener(timeSeconds ->
                SwingUtilities.invokeLater(() ->
                {
                    updatingSlider[0] = true;
                    seekSlider.setValue((int) Math.round(timeSeconds * 1000));
                    currentTimeLabel.setText(formatTime(timeSeconds));
                    updatingSlider[0] = false;
                }));

        // a song picked in the BitMidi window replaces the song on screen
        bitMidiSearch.addActionListener(e -> openBitMidiSearch(song ->
        {
            controller.loadComposition(song);
            songTitle.setText(song.getName());
            play.setIcon(new ImageIcon(getClass().getResource("/images/play.png")));

            // the new song has a different length, so the scrubber has to be resized
            double newDuration = Math.max(composition.duration(), 0.001);
            updatingSlider[0] = true;
            seekSlider.setMaximum(Math.max(1, (int) Math.round(newDuration * 1000)));
            seekSlider.setValue(0);
            updatingSlider[0] = false;
            durationLabel.setText(formatTime(newDuration));
            currentTimeLabel.setText(formatTime(0));
        }));

        JPanel scrubberPanel = new JPanel(new BorderLayout(0, 8));
        scrubberPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 12));
        scrubberPanel.add(durationLabel, BorderLayout.NORTH);
        scrubberPanel.add(seekSlider, BorderLayout.CENTER);
        scrubberPanel.add(currentTimeLabel, BorderLayout.SOUTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(fallingNotesView, BorderLayout.CENTER);
        centerPanel.add(scrubberPanel, BorderLayout.EAST);
        contentPanel.add(centerPanel, BorderLayout.CENTER);
        fallingNotesController.addFinishedListener(() ->
                play.setIcon(new ImageIcon(getClass().getResource("/images/play.png"))));
        contentPanel.revalidate();
        contentPanel.repaint();

        centerOnOctave(
                pianoScrollPane, pianoView, HOME_OCTAVE_INDEX);
    }

    /** Formats seconds as m:ss (for example, 75 seconds -> "1:15"). */
    private static String formatTime(double seconds)
    {
        int totalSeconds = (int) Math.floor(Math.max(0, seconds));
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
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

    private void openBitMidiSearch(Consumer<Composition> songLoader)
    {
        if (bitMidiSearchGui == null)
        {
            bitMidiSearchGui = new BitMidiSearchGui(songLoader);
            bitMidiSearchGui.setLocationRelativeTo(this);
        }

        bitMidiSearchGui.setVisible(true);
        bitMidiSearchGui.toFront();
    }

    private void openRecordings()
    {
        if (recordingsGui == null)
        {
            recordingsGui = new RecordingsGui(() -> controller);
            recordingsGui.setLocationRelativeTo(this);
        }
        recordingsGui.setVisible(true);
        recordingsGui.toFront();
    }
}