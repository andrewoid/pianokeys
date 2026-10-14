package pianokeys.net;

import io.reactivex.rxjava3.core.Single;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

interface BitMidiApi
{
    @GET("search")
    Single<BitMidiSearchResponse> search(@Query("q") String query, @Query("page") int page);

    @Streaming
    @GET
    Single<ResponseBody> download(@Url String url);
}
