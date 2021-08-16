package com.confluence.exporter.model;


import java.io.Serializable;

public class NodeId implements Serializable {
    String title;
    String id;

    public NodeId() {
    }

    public NodeId(String title, String id) {
        this.title = title;
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "NodeId{" +
                "title='" + title + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
