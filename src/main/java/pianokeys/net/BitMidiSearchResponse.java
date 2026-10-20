package pianokeys.net;

import java.util.List;

// Inner payload returned by BitMidi under the JSON field "result".
record BitMidiSearchResponse(List<BitMidiSearchResult> results) {
}