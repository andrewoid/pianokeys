package pianokeys.net;

import io.reactivex.rxjava3.core.Single;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

interface BitMidiApi
{
    // BitMidi's JSON search endpoint is 0-based: page=0 is the first page.
    // BitMidiService converts from the app's 1-based page numbers before calling here.
    @GET("api/midi/search")
    Single<BitMidiSearchEnvelope> search(@Query("q") String query, @Query("page") int page);

    @Streaming
    @GET
    Single<ResponseBody> download(@Url String url);
}