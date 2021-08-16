package com.confluence.exporter.model;


public class Upload {
    String spaceKey;
    String pageId;
    String filePath;
    String gdriveHtmlFileName;

    public Upload(String spaceKey, String pageId, String filePath, String gdriveHtmlFileName) {
        this.spaceKey = spaceKey;
        this.pageId = pageId;
        this.filePath = filePath;
        this.gdriveHtmlFileName = gdriveHtmlFileName;
    }

    public Upload() {
    }

    public String getSpaceKey() {
        return spaceKey;
    }

    public void setSpaceKey(String spaceKey) {
        this.spaceKey = spaceKey;
    }

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getGdriveHtmlFileName() {
        return gdriveHtmlFileName;
    }

    public void setGdriveHtmlFileName(String gdriveHtmlFileName) {
        this.gdriveHtmlFileName = gdriveHtmlFileName;
    }

    @Override
    public String toString() {
        return "Upload{" +
                "spaceKey='" + spaceKey + '\'' +
                ", pageId='" + pageId + '\'' +
                ", filePath='" + filePath + '\'' +
                ", gitlabMdFileName='" + gdriveHtmlFileName + '\'' +
                '}';
    }
}
