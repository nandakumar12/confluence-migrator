package com.confluence.exporter.model;

import java.io.Serializable;

public class PageId implements Serializable {
    String spaceKey;
    String pageId;

    public PageId(String spaceKey, String pageId) {
        this.spaceKey = spaceKey;
        this.pageId = pageId;
    }

    public PageId() {
    }

    public String getSpaceKey() {
        return spaceKey;
    }

    public String getPageId() {
        return pageId;
    }

    public void setSpaceKey(String spaceKey) {
        this.spaceKey = spaceKey;
    }



    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    @Override
    public String toString() {
        return "PageId{" +
                "spaceKey='" + spaceKey + '\'' +
                ", pageId='" + pageId + '\'' +
                '}';
    }
}
