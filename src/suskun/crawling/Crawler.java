package suskun.crawling;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import java.net.URLDecoder;
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
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class Crawler extends WebCrawler {

    private static final Pattern FILTERS = Pattern.compile(".*(\\.(css|js|gif|jpg|png|mp3|zip|gz|xls))$");

    private Path root;

    static Map<String, ContentPatterns> patternsMap = new HashMap<>();

    static {
        try {
            patternsMap = ContentPatterns.fromFile(Paths.get("domains/crawl-rules.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ContentPatterns getPatternsIfContains(String domain) {
        domain = domain.replaceAll("www\\.|http://|https://", "").toLowerCase(Locale.ENGLISH);
        for (String s : patternsMap.keySet()) {
            s = s.toLowerCase();
            if (s.contains(domain)) {
                return patternsMap.get(s);
            }
        }
        Log.warn("No patterns found for %s", domain);
        return null;
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

        ContentPatterns patterns = getPatternsIfContains(url.getDomain());
        if (patterns != null) {
            for (Pattern p : patterns.getUrlRemovePatterns()) {
                if (Regexps.matchesAny(p, href)) {
                    return false;
                }
            }
        }

        Log.debug("%s will be loaded.", href);
        return true;
    }

    @Override
    public void visit(Page page) {

        WebURL webURL = page.getWebURL();
        String url = webURL.getURL();

        String href = webURL.getURL().toLowerCase();

        ContentPatterns patterns = getPatternsIfContains(webURL.getDomain());

        boolean ignore = true;

        if (patterns != null) {

            for (Pattern p : patterns.getUrlRemovePatterns()) {
                if (Regexps.matchesAny(p, href)) {
                    String clean = getString(href);
                    Log.info("%s will not be saved. (URL rule [%s])", clean, p.pattern());
                    return;
                }
            }

            if (patterns.getUrlAcceptPatterns().size() > 0) {
                for (Pattern p : patterns.getUrlAcceptPatterns()) {
                    if (Regexps.matchesAny(p, href)) {
                        String clean = getString(href);
                        Log.info("%s will be saved.", clean);
                        ignore = false;
                        break;
                    }
                }
            } else {
                ignore = false;
            }
        } else {
            ignore = false;
        }

        if (ignore) {
            Log.info("%s is not saved.", getString(href));
            return;
        }

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
                Log.info("%s saved.", getString(fileName));
            }
        } catch (Exception e) {
            System.err.println("Exception while visiting " + url);
            System.err.println(e.toString());
            System.err.println("");
        }
    }

    private String getString(String href) {
        String clean;
        try {
            clean = URLDecoder.decode(href, "UTF-8");
        } catch (Exception e) {
            clean = href;
        }
        return clean;
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
