package alanisia.m3u8downloader;

import com.google.common.base.MoreObjects;
import java.io.File;

public class Input {
    private String hostUrl;
    private File m3u8File;
    private String savePath;

    public Input() {}

    public Input(String hostUrl, File m3u8File, String savePath) {
        this.hostUrl = hostUrl;
        this.m3u8File = m3u8File;
        this.savePath = savePath;
    }

    public String getHostUrl() {
        return hostUrl;
    }

    public void setHostUrl(String hostUrl) {
        this.hostUrl = hostUrl;
    }

    public File getM3u8File() {
        return m3u8File;
    }

    public void setM3u8File(File m3u8File) {
        this.m3u8File = m3u8File;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("hostUrl", hostUrl)
                .add("m3u8File", m3u8File.getPath())
                .add("savePath", savePath)
                .toString();
    }
}
