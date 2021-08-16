package com.confluence.exporter.model;


public class PageDetails {
    String id;
    String spaceKey;
    String fileName;

    public PageDetails() {
    }

    public PageDetails(String id, String spaceKey, String fileName) {
        this.id = id;
        this.spaceKey = spaceKey;
        this.fileName = fileName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSpaceKey() {
        return spaceKey;
    }

    public void setSpaceKey(String spaceKey) {
        this.spaceKey = spaceKey;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return "PageDetails{" +
                "id='" + id + '\'' +
                ", spaceKey='" + spaceKey + '\'' +
                ", fileName='" + fileName + '\'' +
                '}';
    }
}
