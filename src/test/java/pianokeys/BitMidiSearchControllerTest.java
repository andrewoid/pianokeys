package pianokeys;

import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pianokeys.net.BitMidiFile;
import pianokeys.net.BitMidiService;
import pianokeys.net.BitMidiTrack;

import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * The searches and downloads run on a background thread, so these tests use
 * verify(..., timeout(1000)) to wait up to a second for the view to be updated.
 */
class BitMidiSearchControllerTest
{
    BitMidiSearchView view = mock();
    BitMidiService service = mock();
    Consumer<Composition> songLoader = mock();
    BitMidiTrack track = new BitMidiTrack("mario.mid", "https://bitmidi.com/mario-mid", "");

    @Test
    void search()
    {
        // given
        BitMidiSearchController controller = new BitMidiSearchController(view, service, songLoader);
        doReturn(Single.just(List.of(track))).when(service).search("mario", 1);

        // when
        controller.search("  mario ");

        // then
        verify(view, timeout(1000)).showResults(List.of(track));
        verify(view, timeout(1000)).showPage(1, false, true);
        verify(view, timeout(1000)).showStatus("1 results for \"mario\"");
    }

    @Test
    void searchBlank()
    {
        // given
        BitMidiSearchController controller = new BitMidiSearchController(view, service, songLoader);

        // when
        controller.search("   ");

        // then
        verify(view).showStatus("Please enter a search term");
        verifyNoInteractions(service);
    }

    @Test
    void searchNoResults()
    {
        // given
        BitMidiSearchController controller = new BitMidiSearchController(view, service, songLoader);
        doReturn(Single.just(List.of())).when(service).search("zzzz", 1);

        // when
        controller.search("zzzz");

        // then
        verify(view, timeout(1000)).showPage(1, false, false);
        verify(view, timeout(1000)).showStatus("No results for \"zzzz\"");
    }

    @Test
    void searchError()
    {
        // given
        BitMidiSearchController controller = new BitMidiSearchController(view, service, songLoader);
        doReturn(Single.error(new IllegalStateException("offline"))).when(service).search("mario", 1);

        // when
        controller.search("mario");

        // then
        verify(view, timeout(1000)).showPage(1, false, false);
        verify(view, timeout(1000)).showStatus("Search failed: offline");
    }

    @Test
    void nextPage()
    {
        // given
        BitMidiSearchController controller = new BitMidiSearchController(view, service, songLoader);
        doReturn(Single.just(List.of(track))).when(service).search(anyString(), anyInt());
        controller.search("mario");

        // when
        controller.nextPage();

        // then
        verify(service, timeout(1000)).search("mario", 2);
        verify(view, timeout(1000)).showPage(2, true, true);
    }

    @Test
    void previousPageOnFirstPage()
    {
        // given
        BitMidiSearchController controller = new BitMidiSearchController(view, service, songLoader);
        doReturn(Single.just(List.of(track))).when(service).search("mario", 1);
        controller.search("mario");

        // when
        controller.previousPage();

        // then
        verify(service, times(1)).search(anyString(), anyInt());
    }

    @Test
    void loadSong() throws Exception
    {
        // given
        BitMidiSearchController controller = new BitMidiSearchController(view, service, songLoader);
        BitMidiTrack twinkle = new BitMidiTrack("Twinkle Twinkle.mid", "https://bitmidi.com/twinkle-mid", "");
        doReturn(Single.just(new BitMidiFile("twinkle.mid", twinkleMidi()))).when(service).download(twinkle);

        // when
        controller.loadSong(twinkle);

        // then
        ArgumentCaptor<Composition> song = ArgumentCaptor.forClass(Composition.class);
        verify(songLoader, timeout(1000)).accept(song.capture());
        assertEquals("Twinkle Twinkle", song.getValue().getName());
        verify(view, timeout(1000)).showStatus("Loaded Twinkle Twinkle");
    }

    @Test
    void loadSongUpperCaseExtension() throws Exception
    {
        // given
        BitMidiSearchController controller = new BitMidiSearchController(view, service, songLoader);
        BitMidiTrack twinkle = new BitMidiTrack("TWINKLE.MID", "https://bitmidi.com/twinkle-mid", "");
        doReturn(Single.just(new BitMidiFile("twinkle.mid", twinkleMidi()))).when(service).download(twinkle);

        // when
        controller.loadSong(twinkle);

        // then
        ArgumentCaptor<Composition> song = ArgumentCaptor.forClass(Composition.class);
        verify(songLoader, timeout(1000)).accept(song.capture());
        assertEquals("TWINKLE", song.getValue().getName());
    }

    @Test
    void loadSongNotMidi()
    {
        // given
        BitMidiSearchController controller = new BitMidiSearchController(view, service, songLoader);
        doReturn(Single.just(new BitMidiFile("mario.mid", "<html></html>".getBytes()))).when(service).download(track);

        // when
        controller.loadSong(track);

        // then
        verify(view, timeout(1000)).showStatus(startsWith("Could not load mario.mid"));
        verifyNoInteractions(songLoader);
    }

    @Test
    void loadSongDownloadError()
    {
        // given
        BitMidiSearchController controller = new BitMidiSearchController(view, service, songLoader);
        doReturn(Single.error(new IllegalStateException("offline"))).when(service).download(track);

        // when
        controller.loadSong(track);

        // then
        verify(view, timeout(1000)).showStatus("Could not load mario.mid: offline");
        verifyNoInteractions(songLoader);
    }

    // a real MIDI file that is already in the project
    private byte[] twinkleMidi() throws Exception
    {
        try (InputStream input = getClass().getResourceAsStream("/twinkle-twinkle-little-star.mid"))
        {
            return input.readAllBytes();
        }
    }
}