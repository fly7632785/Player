package com.jafir.player;

/**
 * created by jafir on 2019/3/15
 */
public class RecordingModel {
    private long rid;
    private String name;
    private String coverUrl;
    private String createTime;
    private String duration;
    private String filePath;
    private long size;

    public RecordingModel(String name, String filePath, String coverUrl, String createTime, String duration, long size) {
        this.name = name;
        this.filePath = filePath;
        this.coverUrl = coverUrl;
        this.createTime = createTime;
        this.duration = duration;
        this.size = size;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getRid() {
        return rid;
    }

    public void setRid(long rid) {
        this.rid = rid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
