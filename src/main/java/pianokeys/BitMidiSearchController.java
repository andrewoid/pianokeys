package pianokeys;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import pianokeys.net.BitMidiService;
import pianokeys.net.BitMidiTrack;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

public class BitMidiSearchController
{
    private final BitMidiSearchView view;
    private final BitMidiService service;

    // gets the downloaded song so it can be shown in the main window
    private final Consumer<Composition> songLoader;

    private String query = "";
    private int page = 1;
    private boolean hasNext;

    // the search or download that is currently running, if there is one
    private Disposable search = Disposable.disposed();

    public BitMidiSearchController(BitMidiSearchView view,
                                   BitMidiService service,
                                   Consumer<Composition> songLoader)
    {
        this.view = view;
        this.service = service;
        this.songLoader = songLoader;
    }

    /**
     * Starts a new search on the first page
     *
     * @param text the text the user typed
     */
    public void search(String text)
    {
        String trimmed = text.trim();
        if (trimmed.isEmpty())
        {
            view.showStatus("Please enter a search term");
            return;
        }
        searchPage(trimmed, 1);
    }

    //searches the page after the current one
    public void nextPage()
    {
        searchPage(query, page + 1);
    }

    //searches the page before the current one
    public void previousPage()
    {
        if (page > 1)
        {
            searchPage(query, page - 1);
        }
    }

    private void searchPage(String query, int page)
    {
        // stop a search that is still running so its results don't replace the new ones
        search.dispose();
        this.query = query;
        this.page = page;
        view.setSearching(true);
        view.showStatus("Searching for \"" + query + "\"...");

        // search on a background thread and update the view on the Swing thread
        search = service.search(query, page)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.from(SwingUtilities::invokeLater))
                .subscribe(this::showResults, this::showError);
    }

    private void showResults(List<BitMidiTrack> tracks)
    {
        hasNext = !tracks.isEmpty();
        view.showResults(tracks);
        view.setSearching(false);
        view.showPage(page, page > 1, hasNext);
        if (tracks.isEmpty())
        {
            view.showStatus("No results for \"" + query + "\"");
        } else
        {
            view.showStatus(tracks.size() + " results for \"" + query + "\"");
        }
    }

    /**
     * Downloads the song from BitMidi and shows it in the main window
     *
     * @param track the song picked in the results list
     */
    public void loadSong(BitMidiTrack track)
    {
        search.dispose();
        view.setSearching(true);
        view.showStatus("Downloading " + track.title() + "...");

        // download and read the MIDI file on a background thread, then show it on the Swing thread
        search = service.download(track)
                .map(file -> new Composition().fromMidiBytes(songName(track), file.content()))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.from(SwingUtilities::invokeLater))
                .subscribe(this::showSong, error -> showSongError(track, error));
    }

    private void showSong(Composition song)
    {
        songLoader.accept(song);
        view.setSearching(false);
        view.showPage(page, page > 1, hasNext);
        view.showStatus("Loaded " + song.getName());
    }

    private void showSongError(BitMidiTrack track, Throwable error)
    {
        view.setSearching(false);
        view.showPage(page, page > 1, hasNext);
        view.showStatus("Could not load " + track.title() + ": " + error.getMessage());
    }

    // "Super Mario 64 - Medley.mid" becomes "Super Mario 64 - Medley"
    private String songName(BitMidiTrack track)
    {
        String title = track.title();
        if (title.toLowerCase().endsWith(".mid"))
        {
            return title.substring(0, title.length() - ".mid".length());
        }
        return title;
    }

    private void showError(Throwable error)
    {
        hasNext = false;
        view.showResults(List.of());
        view.setSearching(false);
        view.showPage(page, page > 1, false);
        view.showStatus("Search failed: " + error.getMessage());
    }
}