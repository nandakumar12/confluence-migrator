package com.confluence.exporter.service;

import com.confluence.exporter.dao.NodeDao;
import com.confluence.exporter.dto.NodeDto;
import com.confluence.exporter.model.Node;
import com.confluence.exporter.model.NodeId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class NodeService {

    @Autowired
    NodeDao nodeDao;
    public static final Logger log = LogManager.getLogger(NodeService.class);


    /**
     * This method will search the and return the node with the given key
     * starting from the given node it will search all its child nodes
     * if multiple nodes are present with the same key it will return the first match
     *
     * @param name This is the key of the node to be searched
     * @return Node This returns the node with the matched key
     */

    private final Function<Node, NodeDto> mapToNodeDto = n ->
            new NodeDto(n.getTitle(), n.getId(), n.getSpaceKey(), n.getParent(), n.getChildren());


    /**
     * This method will set a node as an child for the given
     * parent node
     *
     * @param nodeName This is the parent node to which the new child must be set
     * @param newChildNode  This is the new node which is to be set as an child
     * @return nothing
     */
    public void setChild(Node nodeName, Node newChildNode) {
            newChildNode.setParent(nodeName);
            nodeName.setChild(newChildNode);
            nodeDao.save(nodeName);
    }

    public Node search(String title, String id){
        Optional<Node> node = nodeDao.findById(new NodeId(title, id));
        return node.orElse(null);
    }

    public List<NodeDto> getAllNodes() {
        return nodeDao.findAll().stream().map(mapToNodeDto).collect(Collectors.toList());

    }

    public Set<Node> getChildNodes(String title, String id) {
       return nodeDao.findById(new NodeId(title, id)).get().getChildren();
    }

    public Node createNode(Node node) {
        return nodeDao.save(node);
    }

    public NodeDto convert(Node node){
        return new NodeDto(node.getTitle(), node.getId(), node.getSpaceKey(), node.getParent(), node.getChildren());
    }


}
