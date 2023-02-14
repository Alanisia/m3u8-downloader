package alanisia.m3u8downloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;

public class M3U8Handler {
    private static final Logger LOGGER = LoggerFactory.getLogger(M3U8Handler.class);
    private Input input;

    public M3U8Handler(Input input) {
        this.input = input;
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

    public void download() {
        try {
            List<String> snippets = resolveM3u8();

            int snippetDownloaded = 0;
            FutureTask<Integer> futureTask = new FutureTask<>(() -> {
                return (snippetDownloaded / snippets.size()) * 100;
            });
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    public M3U8Handler setInput(Input input) {
        this.input = input;
        return this;
    }

    public Input getInput() {
        return input;
    }
}
