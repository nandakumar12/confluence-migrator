package com.confluence.exporter.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class JiraIssue {
    @Id
    String issueId;
    String url;

    public JiraIssue() {
    }

    public JiraIssue(String issueId, String url) {
        this.issueId = issueId;
        this.url = url;
    }

    public String getIssueId() {
        return issueId;
    }

    public void setIssueId(String issueId) {
        this.issueId = issueId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
