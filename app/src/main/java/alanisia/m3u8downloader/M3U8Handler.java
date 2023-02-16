package alanisia.m3u8downloader;

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
import java.util.concurrent.atomic.AtomicInteger;

public class M3U8Handler extends Service<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(M3U8Handler.class);
    private final HttpClient httpClient;
    private final List<InputStream> inputStreams = new ArrayList<>();
    private AtomicInteger downloadedSnippetCount = new AtomicInteger(0);
    private boolean downloadStatus = false;
    private Input input;

    public M3U8Handler(Input input) {
        this.input = input;
        this.httpClient = HttpClient.newBuilder().build();
    }

    private List<String> resolveM3u8() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(input.getM3u8File()));
        List<String> m3u8UriSnippets = new ArrayList<>();
        String snippet;
        int i = 1;
        while ((snippet = bufferedReader.readLine()) != null) {
            if (i >= 7 && (i & 1) != 0) {
                m3u8UriSnippets.add(snippet);
            }
            i++;
        }
        return m3u8UriSnippets;
    }

    private URI createURI(String snippet) {
        int len = input.getHostUrl().length();
        if (input.getHostUrl().charAt(len - 1) == '/') {
            input.setHostUrl(input.getHostUrl().substring(0, len - 1));
        }
        return URI.create(String.format("%s/%s", input.getHostUrl(), snippet));
    }

    public M3U8Handler setInput(Input input) {
        this.input = input;
        return this;
    }

    public Input getInput() {
        return input;
    }

    public List<InputStream> getInputStreams() {
        return inputStreams;
    }

    public boolean getDownloadStatus() {
        return downloadStatus;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                downloadedSnippetCount.set(0);
                downloadStatus = true;
                try {
                    List<String> snippets = resolveM3u8();
                    AtomicInteger snippetDownloaded = new AtomicInteger(0);
                    for (int i = 0; i < snippets.size(); i++) {
                        String e = snippets.get(i);
                        HttpRequest request = HttpRequest.newBuilder().uri(createURI(e)).build();
                        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                                .thenApply(HttpResponse::body)
                                .thenAccept(c -> {
                                    LOGGER.debug("Download over: {}", e);
                                    inputStreams.add(Integer.parseInt(e.substring(8, e.length() - 3)), c);
                                    snippetDownloaded.getAndIncrement();
                                    double progress = (double) snippetDownloaded.get() / snippets.size();
                                    LOGGER.debug("P: {}", progress);

                                });

                    }
                } catch (IOException e) {
                    LOGGER.error(e.getMessage());
                }
                return null;
            }
        };
    }
}
