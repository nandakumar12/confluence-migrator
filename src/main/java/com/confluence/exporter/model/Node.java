package com.confluence.exporter.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.Objects;
import java.util.Set;

/**
 * This is the class which holds the logic for creating custom tree(General Tree)
 * with n nodes.
 * This General tree will be using by the application to generate the hierarchy
 * structure of the confluence space
 *
 * @author  Nandakumar12
 */

@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
@IdClass(NodeId.class)
public class Node {

    @Id
    String title;

    @Id
    String id;

    String spaceKey;

    @ManyToOne(fetch = FetchType.EAGER)
    private Node parent;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "parent")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Set<Node> children;

    public Node(String title, String id, String spaceKey, Node parent, Set<Node> children) {
        this.title = title;
        this.id = id;
        this.spaceKey = spaceKey;
        this.parent = parent;
        this.children = children;
    }

    public Node() {
    }

    @JsonIgnore
    public Set<Node> getChildren(){
        return children;
    }

    @Transient
    public void setChild(Node child){
        children.add(child);
    }

    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }

    public String getSpaceKey() {
        return spaceKey;
    }

    public Node getParent() {
        return parent;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setSpaceKey(String spaceKey) {
        this.spaceKey = spaceKey;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return title.equals(node.title) && id.equals(node.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, id);
    }

    @Override
    public String toString() {
        return "Node{" +
                "title='" + title + '\'' +
                ", id='" + id + '\'' +
                ", spaceKey='" + spaceKey + '\'' +
                ", parent=" + parent +
                '}';
    }
}
