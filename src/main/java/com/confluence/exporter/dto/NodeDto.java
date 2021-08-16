package com.confluence.exporter.dto;

import com.confluence.exporter.model.Node;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeDto {
    String title;
    String id;
    String spaceKey;
    Node parent;
    Set<Node> children;

    public NodeDto(String title, String id, String spaceKey, Node parent, Set<Node> children) {
        this.title = title;
        this.id = id;
        this.spaceKey = spaceKey;
        this.parent = parent;
        this.children = children;
    }

    public NodeDto() {
    }
}
