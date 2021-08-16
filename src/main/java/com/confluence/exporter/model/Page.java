package com.confluence.exporter.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

@Entity
@IdClass(PageId.class)
public class Page {
    @Id
    String spaceKey;

    @Id
    String pageId;

    String pageName;

    public Page(String spaceKey, String pageId, String pageName, String htmlPageSlug, String status) {
        this.spaceKey = spaceKey;
        this.pageId = pageId;
        this.pageName = pageName;
        this.htmlPageSlug = htmlPageSlug;
        this.status = status;
    }

    public Page() {
    }

    public String getSpaceKey() {
        return spaceKey;
    }

    public String getPageId() {
        return pageId;
    }

    public String getPageName() {
        return pageName;
    }

    public String getStatus() {
        return status;
    }

    public void setSpaceKey(String spaceKey) {
        this.spaceKey = spaceKey;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHtmlPageSlug() {
        return htmlPageSlug;
    }

    public void setHtmlPageSlug(String htmlPageSlug) {
        this.htmlPageSlug = htmlPageSlug;
    }

    @Column(name="htmlPageSlug",columnDefinition="LONGTEXT")
    String htmlPageSlug;

    String status;

    public enum status{
        NOT_STARTED,
        CREATED_HIERARCHY,
        DONE
    }

    @Override
    public String toString() {
        return "Page{" +
                "spaceKey='" + spaceKey + '\'' +
                ", pageId='" + pageId + '\'' +
                ", pageName='" + pageName + '\'' +
                ", htmlPageSlug='" + htmlPageSlug + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
