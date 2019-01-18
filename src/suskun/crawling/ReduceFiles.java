package suskun.crawling;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import suskun.extractor.ContentPatterns;
import zemberek.core.logging.Log;
import zemberek.core.text.Regexps;

public class ReduceFiles {

    private static void moveUnnecessaryFiles(Path root, Path outRoot, String domain, boolean dryRun)
            throws IOException {

        ContentPatterns patterns = Crawler.getPatternsIfContains(domain);
        if (patterns == null) {
            Log.info("No patterns found for %s.", domain);
            return;
        }

        Path domainRoot = root.resolve(domain + "/data");
        if (!domainRoot.toFile().exists()) {
            Log.info("Cannot find %s", domainRoot);
            return;
        }

        Path outDomain = outRoot.resolve(domain);

        Files.createDirectories(outDomain);

        Log.info("Loading from %s", domainRoot);

        List<Path> htmlDirectories = Files.walk(domainRoot, 1)
                .filter(s -> s.toFile().isDirectory() && !s.equals(domainRoot))
                .collect(Collectors.toList());

        Log.info("There are %d directories.", htmlDirectories.size());
        Path logFile = outDomain.resolve(domain + "-" + System.currentTimeMillis() + ".log");

        int totalFiles = 0, totalFilesToMove = 0, totalFileToRemove = 0;

        for (Path htmlDirectory : htmlDirectories) {

            List<Path> htmlFiles = Files.walk(htmlDirectory, 1)
                    .filter(s -> s.toFile().isFile())
                    .collect(Collectors.toList());
            Log.info("There are %d files to process in %s.", htmlFiles.size(), htmlDirectory.toFile().getName());
            totalFiles += htmlFiles.size();

            LinkedHashSet<Path> toMove = new LinkedHashSet<>();
            LinkedHashSet<Path> toRemove = new LinkedHashSet<>();
            for (Path htmlFile : htmlFiles) {
                String fileName = htmlFile.toFile().getName();
                String decoded = "";
                try {
                    decoded = URLDecoder.decode(fileName, "utf-8");
                } catch (Exception e) {
                    Log.warn("Exception while decoding name %s", fileName);
                    toRemove.add(htmlFile);
                }
                for (Pattern p : patterns.getUrlRemovePatterns()) {
                    if (Regexps.matchesAny(p, decoded)) {
                        toMove.add(htmlFile);
                    }
                }
            }
            Log.info("Amount of files to move = %d", toMove.size());
            totalFilesToMove += toMove.size();
            try (PrintWriter pw = new PrintWriter(logFile.toFile(), "utf-8")) {
                toMove.forEach(s -> pw.println(s.toString()));
            }
            Path outSubDir = outDomain.resolve(htmlDirectory.toFile().getName());
            Files.createDirectories(outSubDir);
            for (Path path : toMove) {
                if (!path.toFile().exists()) {
                    Log.warn("Path does not exist: %s", path);
                    continue;
                }
                if (!dryRun) {
                    Files.move(
                            path,
                            outSubDir.resolve(path.toFile().getName()),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
            Log.info("Amount of files to remove = %d", toRemove.size());
            totalFileToRemove += toRemove.size();
            for (Path path : toRemove) {
                if (!dryRun) {
                    Files.delete(path);
                }
            }

        }
        Log.info("Total files = %d", totalFiles);
        Log.info("Total files moved = %d", totalFilesToMove);
        Log.info("Total files removed = %d", totalFileToRemove);

    }

    public static void main(String[] args) throws IOException {
        Path root = Paths.get("/media/aaa/Data/crawl/forum-tr/");
        Path out = Paths.get("/media/aaa/Data/crawl/reduced-forum-tr/");

        List<Path> sourceDirs = Lists.newArrayList(Files.walk(root, 1)
                .filter(path -> path.toFile().isDirectory() && !path.equals(root)).iterator());

        for (Path sourceDir : sourceDirs) {
            moveUnnecessaryFiles(root, out, sourceDir.toFile().getName(), true);
        }

    }

}

