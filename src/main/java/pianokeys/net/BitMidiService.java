package pianokeys.net;

import io.reactivex.rxjava3.core.Single;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import okhttp3.ResponseBody;

public class BitMidiService
{
    private final BitMidiApi api;
    private final String baseUrl;

    BitMidiService(BitMidiApi api, String baseUrl) {
        this.api = api;
        this.baseUrl = baseUrl;
    }

    public Single<List<BitMidiTrack>> search(String query)
    {
        return search(query, 1);
    }

    public Single<List<BitMidiTrack>> search(String query, int page) {
        if (query == null || query.isBlank()) {
            return Single.error(new IllegalArgumentException("query must not be blank"));
        }
        if (page < 1) {
            return Single.error(new IllegalArgumentException("page must be positive"));
        }
        // Public app/UI pages are 1-based, but BitMidi's JSON endpoint is 0-based.
        int apiPage = page - 1;
        return api.search(query, apiPage)
                .map(response -> {
                    // BitMidi wraps the actual payload in a top-level "result" object.
                    if (response == null || response.result() == null) {
                        throw new IllegalStateException("BitMidi search response is missing results");
                    }
                    return response.result();
                })
                .map(response -> parseSearchResponse(response, baseUrl));
    }

    public Single<BitMidiFile> download(BitMidiTrack track)
    {
        if (track == null || track.downloadUrl().isBlank()) {
            return Single.error(new IllegalArgumentException("track must have a download URL"));
        }
        String downloadUrl = track.downloadUrl();
        return api.download(downloadUrl).map(response ->
                new BitMidiFile(fileName(downloadUrl, track), readBytes(response).clone()));
    }

    private List<BitMidiTrack> parseSearchResponse(BitMidiSearchResponse response, String baseUrl) {
        if (response == null || response.results() == null) {
            throw new IllegalStateException("BitMidi search response is missing results");
        }
        URI baseUri = URI.create(baseUrl);
        return response.results().stream()
                .filter(Objects::nonNull)
                .map(result -> new BitMidiTrack(
                        result.name() == null ? "" : result.name(),
                        resolveUrl(baseUri, result.url()),
                        resolveUrl(baseUri, result.downloadUrl())))
                .filter(track -> !track.title().isBlank() && !track.pageUrl().isBlank() && !track.downloadUrl().isBlank())
                .toList();
    }

    private byte[] readBytes(ResponseBody response) {
        try (response) {
            return response.bytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read BitMidi download", exception);
        }
    }

    private String fileName(String downloadUrl, BitMidiTrack track) {
        String path = URI.create(downloadUrl).getPath();
        int slash = path.lastIndexOf('/');
        String fileName = slash >= 0 ? path.substring(slash + 1) : path;
        return fileName.isBlank() ? track.title() : fileName;
    }

    private String resolveUrl(URI baseUri, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return baseUri.resolve(value).toString();
    }
}
