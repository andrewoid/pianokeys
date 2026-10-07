package pianokeys.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BitMidiServiceTest
{
    private MockWebServer server;
    private BitMidiService service;

    @BeforeEach
    void setUp() throws Exception
    {
        server = new MockWebServer();
        server.start();
        service = new BitMidiServiceFactory(server.url("/").toString()).create();
    }

    @AfterEach
    void tearDown() throws Exception
    {
        server.close();
    }

    @Test
    void searchParsesBitMidiHtml() throws Exception
    {
        server.enqueue(new MockResponse().setBody("""
                <main>
                  <a href="/mario-mid">Mario Theme.mid</a>
                  <a href="/about">About</a>
                </main>
                """));

        BitMidiTrack track = service.search("mario").blockingGet().get(0);

        assertEquals("Mario Theme.mid", track.title());
        assertEquals(server.url("/mario-mid").toString(), track.pageUrl());
        assertEquals("", track.downloadUrl());
        assertEquals("/search?q=mario&page=1", server.takeRequest().getPath());
    }

    @Test
    void downloadReturnsMidiBytes() throws Exception
    {
        byte[] midi = new byte[] {0x4d, 0x54, 0x68, 0x64};
        server.enqueue(new MockResponse().setBody("<a href=\"/uploads/123.mid\">Download MIDI</a>"));
        server.enqueue(new MockResponse().setBody(new String(midi)));

        BitMidiFile file = service.download(new BitMidiTrack(
                "Mario Theme.mid",
                server.url("/mario-mid").toString(),
                "")).blockingGet();

        assertEquals("123.mid", file.fileName());
        assertArrayEquals(midi, file.content());
        assertEquals("/mario-mid", server.takeRequest().getPath());
        assertEquals("/uploads/123.mid", server.takeRequest().getPath());
    }
}
