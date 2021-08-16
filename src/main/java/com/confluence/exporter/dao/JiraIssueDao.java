package com.confluence.exporter.dao;

import com.confluence.exporter.model.JiraIssue;
import org.springframework.data.repository.CrudRepository;

public interface JiraIssueDao extends CrudRepository<JiraIssue, String> {
}
