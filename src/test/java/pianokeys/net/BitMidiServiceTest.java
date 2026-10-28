package pianokeys.net;

import io.reactivex.rxjava3.core.Single;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    private BitMidiApi api;

    @BeforeEach
    void setUp()
    {
        api = mock(BitMidiApi.class);
        service = new BitMidiService(api, "https://bitmidi.com/");
    }

    @Test
    void searchRejectsBlankQuery()
    {
        assertThrows(IllegalArgumentException.class,
                () -> service.search("").blockingGet());
    }

    @Test
    void searchRejectsInvalidPage()
    {
        assertThrows(IllegalArgumentException.class,
                () -> service.search("mario", 0).blockingGet());
    }

    @Test
    void downloadReturnsMidiBytes()
    {
        BitMidiTrack track = new BitMidiTrack(
                "mario-theme.mid",
                "https://bitmidi.com/mario-theme-mid",
                "https://bitmidi.com/uploads/mario-theme.mid"
        );

        byte[] midiBytes = {
                0x4d, 0x54, 0x68, 0x64,
                0x00, 0x00, 0x00, 0x06
        };

        ResponseBody response = ResponseBody.create(
                MediaType.parse("audio/midi"),
                midiBytes
        );

        when(api.download(track.downloadUrl()))
                .thenReturn(Single.just(response));

        BitMidiFile file = service.download(track).blockingGet();

        assertEquals("mario-theme.mid", file.fileName());
        assertTrue(file.content().length >= 4);

        assertArrayEquals(
                new byte[] {0x4d, 0x54, 0x68, 0x64},
                Arrays.copyOf(file.content(), 4)
        );
    }
}