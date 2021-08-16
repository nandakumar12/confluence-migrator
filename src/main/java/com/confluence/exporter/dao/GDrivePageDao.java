package com.confluence.exporter.dao;

import com.confluence.exporter.model.GDrivePage;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;


public interface GDrivePageDao extends CrudRepository<GDrivePage, String> {
    GDrivePage findTopByPageSlugContaining(String htmlPageName);
    Optional<GDrivePage> findGDrivePageByPageSlug(String pageSlug);
    Optional<GDrivePage> findGDrivePageByPageId(String pageId);
}
