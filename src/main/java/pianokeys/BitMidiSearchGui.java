package pianokeys;

import pianokeys.net.BitMidiServiceFactory;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public class BitMidiSearchGui extends JFrame
{
    private static final String BIT_MIDI_URL = "https://bitmidi.com/";
    private static final String RECORDINGS_CARD = "recordings";
    private static final String SEARCH_CARD = "search";

    private BitMidiSearchController controller;

    public BitMidiSearchGui(Supplier<PianoController> pianoController)
    {
        setUpFrame();

        CardLayout cardLayout = new CardLayout();
        JPanel cards = new JPanel(cardLayout);

        RecordingsPanel recordingsPanel = new RecordingsPanel(pianoController);
        cards.add(recordingsPanel, RECORDINGS_CARD);

        // Same approach as PianoGui: the view gets a Supplier because the controller
        // can only be created after the view exists.
        Supplier<BitMidiSearchController> controllerSupplier = () -> controller;
        BitMidiSearchView searchView = new BitMidiSearchView(controllerSupplier);
        controller = new BitMidiSearchController(searchView,
                new BitMidiServiceFactory(BIT_MIDI_URL).create());
        cards.add(searchView, SEARCH_CARD);

        add(createToggle(cardLayout, cards, recordingsPanel), BorderLayout.NORTH);
        add(cards, BorderLayout.CENTER);
    }

    private JPanel createToggle(CardLayout cardLayout, JPanel cards, RecordingsPanel recordingsPanel)
    {
        JToggleButton recordingsButton = new JToggleButton("My Recordings", true);
        JToggleButton searchButton = new JToggleButton("Search Online");

        ButtonGroup group = new ButtonGroup();
        group.add(recordingsButton);
        group.add(searchButton);

        recordingsButton.addActionListener(e ->
        {
            recordingsPanel.refresh();
            cardLayout.show(cards, RECORDINGS_CARD);
        });
        searchButton.addActionListener(e -> cardLayout.show(cards, SEARCH_CARD));

        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        togglePanel.add(recordingsButton);
        togglePanel.add(searchButton);
        return togglePanel;
    }

    private void setUpFrame()
    {
        setTitle("MIDI Library");
        // hide instead of exit so closing this window doesn't close the piano
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setSize(600, 500);
    }
}