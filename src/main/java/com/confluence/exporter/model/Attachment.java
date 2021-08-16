package com.confluence.exporter.model;


public class Attachment {
    String name;
    String url;

    public Attachment(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public Attachment() {
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "Attachment{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
