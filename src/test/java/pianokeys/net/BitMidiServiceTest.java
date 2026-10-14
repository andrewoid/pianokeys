package pianokeys.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.reactivex.rxjava3.core.Single;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import okhttp3.ResponseBody;

class BitMidiServiceTest
{
    private BitMidiService service;

    @BeforeEach
    void setUp()
    {
        service = new BitMidiServiceFactory("https://bitmidi.com/").create();
    }

    @Test
    void searchParsesBitMidiHtml() throws Exception
    {
        BitMidiApi api = new BitMidiApi() {
            @Override
            public Single<BitMidiSearchResponse> search(String query, int page) {
                return Single.just(new BitMidiSearchResponse(List.of(
                        new BitMidiSearchResult("1", "Mario.mid", "mario-mid", List.of(), 1L, 1L,
                                "", "", "/mario-mid", "/midi/mario.mid"))));
            }

            @Override
            public Single<ResponseBody> download(String url) {
                return Single.just(ResponseBody.create(null, new byte[] {0x4d, 0x54, 0x68, 0x64}));
            }
        };
        BitMidiService jsonService = new BitMidiService(api, "https://bitmidi.com/");
        List<BitMidiTrack> tracks = jsonService.search("mario").blockingGet();
        BitMidiTrack track = tracks.getFirst();

        assertEquals("Mario.mid", track.title());
        assertEquals("https://bitmidi.com/mario-mid", track.pageUrl());
        assertEquals("https://bitmidi.com/midi/mario.mid", track.downloadUrl());
    }

    @Test
    void downloadReturnsMidiBytes() throws Exception
    {
        BitMidiApi api = new BitMidiApi() {
            @Override
            public Single<BitMidiSearchResponse> search(String query, int page) {
                return Single.just(new BitMidiSearchResponse(List.of()));
            }

            @Override
            public Single<ResponseBody> download(String url) {
                return Single.just(ResponseBody.create(null, new byte[] {0x4d, 0x54, 0x68, 0x64}));
            }
        };
        BitMidiService downloadService = new BitMidiService(api, "https://bitmidi.com/");
        BitMidiTrack track = new BitMidiTrack("Mario.mid", "https://bitmidi.com/mario-mid", "https://bitmidi.com/midi/mario.mid");
        BitMidiFile file = downloadService.download(track).blockingGet();

        assertEquals("mario.mid", file.fileName());
        assertTrue(file.content().length >= 4);
        assertArrayEquals(new byte[] {0x4d, 0x54, 0x68, 0x64},
                java.util.Arrays.copyOf(file.content(), 4));
    }

    @Test
    void downloadRequiresDownloadUrl() {
        BitMidiTrack track = new BitMidiTrack("Mario.mid", "https://bitmidi.com/mario-mid", "");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.download(track).blockingGet());
        assertEquals("track must have a download URL", exception.getMessage());
    }
}
