package suskun.crawling;

import com.google.common.base.Charsets;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class BatchController {

    public static void main(String[] args) throws Exception {
        Path domainList = Paths.get(args[0]);
        Path root = Paths.get(args[1]);
        crawl(domainList, root);
    }

    private static void crawl(Path domainList, Path root) throws IOException, InterruptedException {
        List<Controller> controllers = Files.readAllLines(domainList, Charsets.UTF_8).stream()
                .filter((line) -> !line.startsWith("#") && !line.trim().isEmpty())
                .map((line) -> {
                    String[] split = line.trim().split("\\s+");
                    String domain = split[0];
                    int politeness = 3000;
                    if (split.length == 2) {
                        politeness = Integer.parseInt(split[1]);
                    }
                    return new DomainInfo(domain, politeness);
                })
                .map((domainInfo) -> {
                    try {
                        URI uri = new URI(domainInfo.domain);
                        Path controllerRoot = root.resolve(uri.getHost());
                        Controller controller = new Controller(controllerRoot, uri, domainInfo.politeness);
                        controller.start();
                        return controller;
                    } catch (Exception exception) {
                        exception.printStackTrace();
                        System.exit(-1);
                        return null;
                    }
                }).collect(Collectors.toList());


        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (!br.readLine().equalsIgnoreCase("done")) {
            Thread.sleep(1000);
        }
        controllers.forEach(Controller::stop);
    }

    private static final class DomainInfo {

        final String domain;
        final int politeness;

        DomainInfo(String domain, int politeness) {
            this.domain = domain;
            this.politeness = politeness;
        }
    }
}
