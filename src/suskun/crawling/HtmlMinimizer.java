package suskun.crawling;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import zemberek.core.logging.Log;

public class HtmlMinimizer {

    public static void minimizeFolder(Path input, Path output, int count) throws IOException {

        Files.createDirectories(output);

        Log.info("Loading from %s", input);

        List<Path> htmlFiles = Files.walk(input, 1)
                .filter(s -> s.toFile().isFile())
                .collect(Collectors.toList());
        Log.info("There are %d files to process.", htmlFiles.size());

        int counter = 0;
        for (Path htmlFile : htmlFiles) {
            if (counter > count) {
                return;
            }
            Path out = output.resolve(htmlFile.toFile().getName());
            if (out.equals(htmlFile)) {
                continue;
            }
            out = Paths.get(out.toString() + ".html");
            String content = String.join("\n", Files.readAllLines(htmlFile, StandardCharsets.UTF_8));
            String clean = Jsoup.clean(content, Whitelist.relaxed());
            Files.write(out, Collections.singletonList(clean), StandardCharsets.UTF_8);
            if (counter > 0 && counter % 100 == 0) {
                Log.info("%d of %d processed.", counter, htmlFiles.size());
            }
            counter++;
        }
    }

}
