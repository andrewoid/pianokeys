package pianokeys.net;

import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

final class BitMidiHtmlParser
{
    private BitMidiHtmlParser()
    {
    }

    static List<BitMidiTrack> searchResults(String html, String baseUrl)
    {
        Document document = Jsoup.parse(html, baseUrl);
        List<BitMidiTrack> tracks = new ArrayList<>();
        for (Element link : document.select("a[href]"))
        {
            String title = link.text().trim();
            String pageUrl = link.absUrl("href");
            if (!title.toLowerCase().endsWith(".mid") || !pageUrl.matches(".+/.+-mid(?:[?#].*)?$"))
            {
                continue;
            }

            tracks.add(new BitMidiTrack(title, pageUrl, ""));
        }
        return List.copyOf(tracks);
    }

    static String fileName(BitMidiTrack track)
    {
        String path = java.net.URI.create(track.downloadUrl()).getPath();
        int slash = path.lastIndexOf('/');
        String fileName = slash >= 0 ? path.substring(slash + 1) : path;
        return fileName.isBlank() ? track.title() : fileName;
    }

    static String downloadUrl(String html, String pageUrl)
    {
        Element link = Jsoup.parse(html, pageUrl).selectFirst("a[href$=.mid]");
        return link == null ? "" : link.absUrl("href");
    }
}
