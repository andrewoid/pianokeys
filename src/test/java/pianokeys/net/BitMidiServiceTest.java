
package pianokeys.net;

import io.reactivex.rxjava3.core.Single;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BitMidiServiceTest
{
    private BitMidiService service;
    private BitMidiApi api;

    @BeforeEach
    void setUp()
    {
        api = mock(BitMidiApi.class);
        service = new BitMidiService(api, "https://bitmidi.com/");
    }

    @Test
    void searchParsesBitMidiHtml()
    {
        String html = """
                <html>
                <body>
                <a href="/mario-theme-mid">mario-theme.mid</a>
                </body>
                </html>
                """;

        ResponseBody response = ResponseBody.create(
                MediaType.parse("text/html"),
                html
        );

        when(api.search("mario", 1))
                .thenReturn(Single.just(response));

        List<BitMidiTrack> tracks =
                service.search("mario").blockingGet();

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

        assertFalse(file.fileName().isBlank());
        assertTrue(file.content().length >= 4);

        assertArrayEquals(
                new byte[] {0x4d, 0x54, 0x68, 0x64},
                Arrays.copyOf(file.content(), 4)
        );
    }
}
