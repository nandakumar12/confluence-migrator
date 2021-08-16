package com.confluence.exporter.model;


public class Space {
    String spaceKey;

    public Space() {
    }

    public Space(String spaceKey) {
        this.spaceKey = spaceKey;
    }

    public String getSpaceKey() {
        return spaceKey;
    }

    public void setSpaceKey(String spaceKey) {
        this.spaceKey = spaceKey;
    }

    @Override
    public String toString() {
        return "Space{" +
                "spaceKey='" + spaceKey + '\'' +
                '}';
    }
}
