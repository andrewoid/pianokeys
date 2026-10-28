package pianokeys;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Supplier;

/**
 * Lists the user's own saved recordings (~/pianokeys/my-recordings/) and plays a
 * selected one back through the piano, independent of the falling-notes song.
 */
public class RecordingsPanel extends JPanel
{
    private static final Font FONT = new Font("Arial", Font.BOLD, 16);

    private final Supplier<PianoController> controller;
    private final MidiFileStorage midiFileStorage = new MidiFileStorage();

    private final DefaultListModel<File> recordings = new DefaultListModel<>();
    private final JList<File> recordingsList = new JList<>(recordings);
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton playButton = new JButton("Play");
    private final JButton stopButton = new JButton("Stop");

    private CompositionRunnable activePlayback;

    public RecordingsPanel(Supplier<PianoController> controller)
    {
        this.controller = controller;
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        recordingsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recordingsList.setFont(FONT);
        recordingsList.setCellRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                            boolean isSelected, boolean cellHasFocus)
            {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(((File) value).getName());
                return this;
            }
        });

        add(new JScrollPane(recordingsList), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        refresh();
    }

    private JPanel createButtonPanel()
    {
        playButton.setFont(FONT);
        playButton.addActionListener(e -> playSelected());

        stopButton.setFont(FONT);
        stopButton.setVisible(false);
        stopButton.addActionListener(e -> stopPlayback());

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setFont(FONT);
        refreshButton.addActionListener(e -> refresh());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(refreshButton);
        actions.add(playButton);
        actions.add(stopButton);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(statusLabel, BorderLayout.CENTER);
        buttonPanel.add(actions, BorderLayout.EAST);
        return buttonPanel;
    }

    /**
     * Re-scans the recordings folder. Called at startup, on demand via the
     * Refresh button, and whenever this panel is switched back into view, so a
     * recording saved while this window was on the search tab shows up too.
     */
    public void refresh()
    {
        recordings.clear();
        try
        {
            File directory = midiFileStorage.getRecordingsDirectory().toFile();
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".mid"));
            if (files != null)
            {
                Arrays.sort(files, Comparator.comparing(File::getName).reversed());
                for (File file : files)
                {
                    recordings.addElement(file);
                }
            }
            statusLabel.setText(recordings.size() + " recording" + (recordings.size() == 1 ? "" : "s"));
        } catch (IOException e)
        {
            statusLabel.setText("Unable to read recordings folder: " + e.getMessage());
        }
    }

    private void playSelected()
    {
        File selected = recordingsList.getSelectedValue();
        if (selected == null)
        {
            statusLabel.setText("Select a recording to play");
            return;
        }

        Composition composition = new Composition().fromMidiFile(selected);
        CompositionRunnable playback = new CompositionRunnable(controller.get(), composition);
        activePlayback = playback;

        playButton.setEnabled(false);
        stopButton.setVisible(true);
        statusLabel.setText("Playing " + selected.getName() + "...");

        new Thread(() ->
        {
            playback.run();
            SwingUtilities.invokeLater(() ->
            {
                playButton.setEnabled(true);
                stopButton.setVisible(false);
                activePlayback = null;
                statusLabel.setText(recordings.size() + " recording" + (recordings.size() == 1 ? "" : "s"));
            });
        }).start();
    }

    private void stopPlayback()
    {
        if (activePlayback != null)
        {
            activePlayback.stop();
        }
    }
}
