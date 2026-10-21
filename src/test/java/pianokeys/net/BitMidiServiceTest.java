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
        assertTrue(track.title().toLowerCase().contains("mario"));
        assertTrue(track.pageUrl().startsWith("https://bitmidi.com/"));
        assertTrue(!"https://bitmidi.com/".equals(track.pageUrl()));
        assertTrue(track.downloadUrl().startsWith("https://bitmidi.com/"));
        assertTrue(!"https://bitmidi.com/".equals(track.downloadUrl()));
    }

    @Test
    void downloadFetchesRealMidiFromServer()
    {
        BitMidiTrack track = service.search("mario").blockingGet().getFirst();
        BitMidiFile file = service.download(track).blockingGet();
        byte[] content = file.content();

        assertNotNull(file);
        assertTrue(file.fileName().endsWith(".mid"));
        assertTrue(content.length > 4);
        // 0x4d 0x54 0x68 0x64 are ASCII bytes for the MIDI header chunk magic "MThd".
        assertTrue(
                content[0] == 0x4d
                        && content[1] == 0x54
                        && content[2] == 0x68
                        && content[3] == 0x64);
    }
}
