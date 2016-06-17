package kdtm.crawling;

import com.google.common.base.Charsets;

import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

import java.io.BufferedWriter;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class Crawler extends WebCrawler {

    private static final Pattern FILTERS = Pattern.compile(".*(\\.(css|js|gif|jpg|png|mp3|mp3|zip|gz))$");
    private Path root;

    Rule rules= new Rule();
    ArrayList ruleList =  rules.getRules("/home/sila/projects/crawler/domains/rules");

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
        int count =0;
        for (int i = 0; i < ruleList.size(); i++) {
            rules = (kdtm.crawling.Rule) ruleList.get(i);
            if(href.contains(rules.source) || href.contains(rules.source.replace(":", "%3a").replace("/", "%2f"))) {  //href yukarıda lowercase e çevirdiğimizden a ve f küçük
                for(String ig : rules.ignores) {
                    if(href.contains(ig))
                        count++;
                }
            }
            if(count != 0)
                return false;
        }

        return !FILTERS.matcher(href).matches();
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

            }
        } catch (Exception e) {
            System.err.println("Exception while visiting " + url);
            System.err.println(e.toString());
            System.err.println("");
        }
    }
}
