package com.confluence.exporter.dao;

import com.confluence.exporter.model.Node;
import com.confluence.exporter.model.NodeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NodeDao extends JpaRepository<Node, NodeId> {
}
