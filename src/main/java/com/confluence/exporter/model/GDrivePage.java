package com.confluence.exporter.model;


import javax.persistence.*;

@Entity
public class GDrivePage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name="pageSlug",columnDefinition="LONGTEXT")
    String pageSlug;
    String gDriveId;
    String pageId;
    @Column(name="parentFolderId")
    String parentFolderId;

    public GDrivePage() {
    }

    public GDrivePage(String pageSlug, String gDriveId, String pageId, String parentFolderId) {
        this.pageSlug = pageSlug;
        this.gDriveId = gDriveId;
        this.pageId = pageId;
        this.parentFolderId = parentFolderId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPageSlug() {
        return pageSlug;
    }

    public void setPageSlug(String pageSlug) {
        this.pageSlug = pageSlug;
    }

    public String getgDriveId() {
        return gDriveId;
    }

    public void setgDriveId(String gDriveId) {
        this.gDriveId = gDriveId;
    }

    public String getPageId() {
        return pageId;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public String getParentFolderId() {
        return parentFolderId;
    }

    public void setParentFolderId(String parentFolderId) {
        this.parentFolderId = parentFolderId;
    }
}
