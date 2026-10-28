package pianokeys.net;

// BitMidi wraps the actual search payload in an outer object:
// { "result": { "results": [...] } }
record BitMidiSearchEnvelope(BitMidiSearchResponse result) {
}
