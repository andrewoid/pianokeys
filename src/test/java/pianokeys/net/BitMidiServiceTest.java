package pianokeys.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.reactivex.rxjava3.core.Single;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Mocks BitMidiApi directly instead of calling the real bitmidi.com, so these
 * tests are deterministic and don't depend on that site being up/reachable
 * from wherever they run. Still exercises the real parsing logic
 * (BitMidiHtmlParser) against canned HTML shaped like a real BitMidi page.
 */
class BitMidiServiceTest
{
    private static final String BASE_URL = "https://bitmidi.com/";
    private static final String TRACK_TITLE = "Mario Bros Theme.mid";
    private static final String TRACK_PAGE_URL = "https://bitmidi.com/mario-bros-theme-mid";
    private static final String DOWNLOAD_URL = "https://bitmidi.com/uploads/mario-bros-theme.mid";
    private static final byte[] MIDI_BYTES = {0x4d, 0x54, 0x68, 0x64, 0x00, 0x00, 0x00, 0x06};

    private BitMidiService service;

    @BeforeEach
    void setUp()
    {
        BitMidiApi api = mock(BitMidiApi.class);
        service = new BitMidiService(api, BASE_URL);

        // canned search results page, shaped like a real BitMidi search page
        String searchHtml = "<html><body><a href=\"" + TRACK_PAGE_URL + "\">" + TRACK_TITLE + "</a></body></html>";
        when(api.search(eq("mario"), eq(1))).thenReturn(Single.just(htmlResponse(searchHtml)));

        // canned track page, with its own MIDI download link
        String trackPageHtml = "<html><body><a href=\"" + DOWNLOAD_URL + "\">Download</a></body></html>";
        when(api.page(eq(TRACK_PAGE_URL))).thenReturn(Single.just(htmlResponse(trackPageHtml)));

        // canned MIDI file bytes, standing in for the actual download
        when(api.download(eq(DOWNLOAD_URL)))
                .thenReturn(Single.just(ResponseBody.create(MediaType.get("audio/midi"), MIDI_BYTES)));
    }

    private ResponseBody htmlResponse(String html)
    {
        return ResponseBody.create(MediaType.get("text/html"), html);
    }

    @Test
    void searchParsesBitMidiHtml()
    {
        List<BitMidiTrack> tracks = service.search("mario").blockingGet();
        assertFalse(tracks.isEmpty());
        BitMidiTrack track = tracks.get(0);

        assertTrue(track.title().toLowerCase(Locale.ROOT).endsWith(".mid"));
        URI pageUri = URI.create(track.pageUrl());
        assertEquals("https", pageUri.getScheme());
        assertEquals("bitmidi.com", pageUri.getHost());
        assertTrue(track.downloadUrl().isBlank());
    }

    @Test
    void downloadReturnsMidiBytes()
    {
        List<BitMidiTrack> tracks = service.search("mario").blockingGet();
        assertFalse(tracks.isEmpty());
        BitMidiTrack track = tracks.get(0);

        BitMidiFile file = service.download(track).blockingGet();

        assertFalse(file.fileName().isBlank());
        assertTrue(file.content().length >= 4);
        assertArrayEquals(new byte[] {0x4d, 0x54, 0x68, 0x64},
                Arrays.copyOf(file.content(), 4));
    }
}
