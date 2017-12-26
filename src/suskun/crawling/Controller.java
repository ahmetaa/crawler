package suskun.crawling;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

import java.net.URI;
import java.nio.file.Path;

public class Controller {

    private final CrawlController controller;

    public Controller(Path root, URI uri, int politeness) throws Exception {
        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(root.toString());
        config.setPolitenessDelay(politeness);
        config.setResumableCrawling(true);
        config.setIncludeBinaryContentInCrawling(false);
        config.setUserAgentString("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36");

        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);

        this.controller = new CrawlController(config, pageFetcher, robotstxtServer);

        controller.addSeed(uri.toString());
        controller.setCustomData(root.resolve("data"));
    }


    public void start() throws Exception {
        controller.startNonBlocking(Crawler.class, 2);
    }

    public void stop() {
        controller.shutdown();
        controller.waitUntilFinish();
    }
}
