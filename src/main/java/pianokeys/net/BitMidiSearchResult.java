package pianokeys.net;

import java.util.List;

record BitMidiSearchResult(
        String id,
        String name,
        String slug,
        List<String> alternateNames,
        Long views,
        Long plays,
        String createdAt,
        String updatedAt,
        String url,
        String downloadUrl) {
}