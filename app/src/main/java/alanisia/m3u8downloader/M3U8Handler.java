package alanisia.m3u8downloader;

import com.google.common.base.MoreObjects;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class M3U8Handler extends Service<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(M3U8Handler.class);
    private final Executor executor = Executors.newFixedThreadPool(16);
    private final HttpClient httpClient;
    private List<InputStream> inputStreams;
    private boolean downloadStatus = false;
    private Input input;

    public M3U8Handler() {
        this.httpClient = HttpClient.newBuilder().build();
    }

    private List<String> resolveM3u8() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(input.getM3u8File()));
        List<String> m3u8UriSnippets = new ArrayList<>();
        Pattern pattern = Pattern.compile("^(?!#).+(?<=ts)$");
        String snippet;
        while ((snippet = bufferedReader.readLine()) != null) {
            if (pattern.matcher(snippet).matches()) {
                m3u8UriSnippets.add(snippet);
            }
        }
        return m3u8UriSnippets;
    }

    private URI createURI(String snippet) {
        int len = input.getHostUrl().length();
        if (input.getHostUrl().charAt(len - 1) == '/') {
            input.setHostUrl(input.getHostUrl().substring(0, len - 1));
        }
        if (snippet.charAt(0) == '/') {
            snippet = snippet.substring(1);
        }
        return URI.create(String.format("%s/%s", input.getHostUrl(), snippet));
    }

    public M3U8Handler setInput(Input input) {
        this.input = input;
        return this;
    }

    public boolean getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(boolean downloadStatus) {
        this.downloadStatus = downloadStatus;
    }

    public void writeToOutputFile() throws Exception {
        Enumeration<InputStream> enumeration = Collections.enumeration(inputStreams);
        try (SequenceInputStream sequenceInputStream = new SequenceInputStream(enumeration);
             FileOutputStream fileOutputStream = new FileOutputStream(String.format("%s/out_%d.mp4",
                     input.getSavePath(), System.currentTimeMillis()));
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)) {
            bufferedOutputStream.write(sequenceInputStream.readAllBytes());
        } catch (IOException exception) {
            LOGGER.error(exception.getMessage(), exception);
            throw exception;
        }
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                try {
                    List<String> snippets = resolveM3u8();
                    inputStreams = Arrays.asList(new InputStream[snippets.size()]);
                    AtomicInteger snippetDownloaded = new AtomicInteger(0);
                    CountDownLatch countDownLatch = new CountDownLatch(snippets.size());
                    for (int i = 0; i < snippets.size(); i++) {
                        SnippetWithIndex e = new SnippetWithIndex(snippets.get(i), i);
                        HttpRequest request = HttpRequest.newBuilder().uri(createURI(e.snippet)).build();
                        executor.execute(() -> {
                            try {
                                InputStream inputStream = httpClient
                                        .send(request, HttpResponse.BodyHandlers.ofInputStream()).body();
                                inputStreams.set(e.index, inputStream);
                                snippetDownloaded.incrementAndGet();
                                double progress = (double) snippetDownloaded.get() / snippets.size();
                                updateProgress(progress, 1);
                                updateMessage(String.format("%.2f%%", progress * 100));
                                countDownLatch.countDown();
                            } catch (IOException | InterruptedException ex) {
                                LOGGER.error("{}", ex.getMessage(), ex);
                            }
                        });
                    }
                    countDownLatch.await();
                } catch (InterruptedException | IOException e) {
                    LOGGER.error(e.getMessage());
                }
                return null;
            }
        };
    }

    private record SnippetWithIndex(String snippet, int index) {
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("snippet", snippet).add("index", index).toString();
        }
    }
}
