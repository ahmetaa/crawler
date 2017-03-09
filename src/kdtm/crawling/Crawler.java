package kdtm.crawling;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import suskun.extractor.ContentPatterns;
import zemberek.core.logging.Log;
import zemberek.core.text.Regexps;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Crawler extends WebCrawler {

    private static final Pattern FILTERS = Pattern.compile(".*(\\.(css|js|gif|jpg|png|mp3|mp3|zip|gz))$");
    private Path root;

    static Map<String, ContentPatterns> patternsMap = new HashMap<>();

    static {
        try {
            patternsMap = ContentPatterns.fromFile(Paths.get("domains/content-rules.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init(int id, CrawlController crawlController) {
        super.init(id, crawlController);
        this.root = (Path) getMyController().getCustomData();
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        if (!url.getDomain().equals(referringPage.getWebURL().getDomain())) {
            return false;
        }

        String href = url.getURL().toLowerCase();

        if (FILTERS.matcher(href).matches()) {
            return false;
        }

        ContentPatterns patterns = patternsMap.get(url.getDomain());
        if (patterns != null) {
            for (Pattern p : patterns.getUrlRemovePatterns()) {
                if (Regexps.matchesAny(p, href)) {
                    return false;
                }
            }
            for (Pattern p : patterns.getUrlAcceptPatterns()) {
                if (Regexps.matchesAny(p, href)) {
                    Log.info("%s will be loaded.", href);
                    return true;
                }
            }
            if (patterns.getUrlAcceptPatterns().size() > 0) {
                return false;
            }
        }

        Log.info("%s will be loaded.", href);
        return true;
    }

    @Override
    public void visit(Page page) {

        WebURL webURL = page.getWebURL();
        String url = webURL.getURL();
        try {
            String date = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE);
            Path dir = root.resolve(date);
            if (Files.notExists(dir)) {
                Files.createDirectories(dir);
            }
            String fileName = URLEncoder.encode(url, "UTF-8");
            if (fileName.length() > 255) {
                fileName = fileName.substring(0, 255);
            }

            if (page.getParseData() instanceof HtmlParseData) {
                HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
                String html = htmlParseData.getHtml();
                try (BufferedWriter bw = Files.newBufferedWriter(dir.resolve(fileName), Charsets.UTF_8)) {
                    bw.write(html);
                }
                Log.info("%s saved.", fileName);
            }
        } catch (Exception e) {
            System.err.println("Exception while visiting " + url);
            System.err.println(e.toString());
            System.err.println("");
        }
    }

    static Pattern REMOVE_PATTERN = Pattern.compile(
            "<script.+?</script>|<style.+?</style>|<option.+?</option>|<header.+?</header>|<link.+?/>",
            //"<script.+?</script>|<style.+?</style>|<li.+?</li>|<!--.+?-->|<option.+?</option>|<link.+?/>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public static String reduceHtml(String input) {
        return REMOVE_PATTERN.matcher(input).replaceAll("").replaceAll("[\n\r]+", "\n").replaceAll("[ \t]+", " ");
    }

    public static void main(String[] args) throws IOException {
        String content = String.join("\n", Files.readAllLines(Paths.get("test/html-full.html"), StandardCharsets.UTF_8));
        Files.write(Paths.get("test/reduced.html"), Lists.newArrayList(reduceHtml(content)), StandardCharsets.UTF_8);
    }
}
