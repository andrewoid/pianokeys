package pianokeys.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;

class BitMidiServiceTest
{
    private final BitMidiService service = new BitMidiServiceFactory("https://bitmidi.com/").create();

    @Test
    void searchReturnsRealResultsFromServer()
    {
        List<BitMidiTrack> tracks = service.search("mario").blockingGet();
        assertNotNull(tracks);
        assertFalse(tracks.isEmpty());
        BitMidiTrack track = tracks.getFirst();

        assertNotNull(track.title());
        assertTrue(track.pageUrl().startsWith("https://bitmidi.com/"));
        assertTrue(track.downloadUrl().startsWith("https://bitmidi.com/"));
    }

    @Test
    void downloadFetchesRealMidiFromServer()
    {
        BitMidiTrack track = service.search("mario").blockingGet().getFirst();
        BitMidiFile file = service.download(track).blockingGet();

        assertNotNull(file);
        assertTrue(file.fileName().endsWith(".mid"));
        assertTrue(file.content().length > 4);
        assertTrue(
                file.content()[0] == 0x4d &&
                file.content()[1] == 0x54 &&
                file.content()[2] == 0x68 &&
                file.content()[3] == 0x64);
    }
}
