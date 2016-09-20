package kdtm.crawling;

import de.l3s.boilerpipe.extractors.ArticleExtractor;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Extractor {

    public static void main(String[] args) throws IOException, InterruptedException {

        if (args.length < 2) {
            throw new IllegalArgumentException("At least two parameter [crawl-root] [extraction-root] is necessary");
        }
        //TODO: fill below
        Path inRoot = Paths.get(args[0]);
        Path outRoot = Paths.get(args[1]);

        Rule rules = new Rule();
        ArrayList ruleList = rules.getRules("domains/rules");
        rules = (Rule) ruleList.get(1);


        ThreadPoolExecutor es = new ThreadPoolExecutor(3, 3, 0L, TimeUnit.MILLISECONDS, new LimitedQueue<>(3));

        Files.walkFileTree(inRoot, new FileVisitor<Path>() {

            private final Pattern DATE = Pattern.compile("[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]");

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (DATE.matcher(dir.getFileName().toString()).matches()) {
                    Path relative = inRoot.relativize(dir);
                    Path outFile = outRoot.resolve(relative);
                    if (Files.notExists(outFile)) {
                        es.submit(new ExtractorTask(dir, outFile));
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
        es.shutdown();
        es.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
    }

    private static final class ExtractorTask implements Runnable {

        private static final Pattern pattern = Pattern.compile("<head>.*</head>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        final Path inDir;
        final Path outFile;

        ExtractorTask(Path inDir, Path outFile) {
            this.inDir = inDir;
            this.outFile = outFile;
        }

        @Override
        public void run() {
            try {
                Files.createDirectories(outFile.getParent());
            } catch (IOException ex) {
                System.err.println(ex.toString());
                return;
            }
            Path tmp = Paths.get(outFile.toString() + ".tmp");
            if (Files.exists(tmp)) {
                try {
                    Files.delete(tmp);
                } catch (IOException ex) {
                    System.err.println(ex.toString());
                    return;
                }
            }
            try (OutputStream os = Files.newOutputStream(tmp);
                 DirectoryStream<Path> ds = Files.newDirectoryStream(inDir)) {
                int count = 0;
                for (Path inFile : ds) {
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        BufferedWriter mbw = new BufferedWriter(new OutputStreamWriter(baos, Charset.forName("UTF-8")));
                        String text = new String(Files.readAllBytes(inFile), Charset.forName("UTF-8"));
                        text = pattern.matcher(text).replaceAll("<head><meta charset=\"UTF-8\"></head>");
                        text = ArticleExtractor.INSTANCE.getText(text);

//                        text = DefaultExtractor.INSTANCE.getText(text);
//                        text = KeepEverythingExtractor.INSTANCE.getText(text);
//
                        mbw.write("<doc id=\"" + inFile.getFileName().toString().replace("%3A", ":").replace("%2F", "/") + "\" source=\"" + inDir.toString().substring(inDir.toString().indexOf("www"), inDir.toString().indexOf("/data")) + "\" crawl-date=\"" + outFile.toString().substring(outFile.toString().length() - 10) + "\">");
//                        mbw.write("#####");
//                        mbw.write(inFile.getFileName().toString());
                        mbw.newLine();
                        mbw.write(text);
                        mbw.write("</doc>");
                        mbw.newLine();
                        mbw.close();
                        os.write(baos.toByteArray());
                        count++;
                    } catch (Exception aex) {
                        System.err.println("Exception in file " + inFile);
                    }
                }
                Files.move(tmp, outFile);
                System.out.println("completed : " + inDir + " " + count + " files");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static final class LimitedQueue<E> extends LinkedBlockingQueue<E> {

        public LimitedQueue(int maxSize) {
            super(maxSize);
        }

        @Override
        public boolean offer(E e) {
            // turn offer() and add() into a blocking calls (unless interrupted)
            try {
                put(e);
                return true;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            return false;
        }

    }
}
