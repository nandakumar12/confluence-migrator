package com.confluence.exporter.dao;
import com.confluence.exporter.model.Page;
import com.confluence.exporter.model.PageId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PageDao extends CrudRepository <Page,PageId>{
    @Query("select p from Page p where p.spaceKey=:sKey")
    List<Page> findPageBySpaceKey(@Param("sKey") String spaceKey);

    Optional<Page> findPageByPageId(String pageId);

    @Query("select p from Page p where p.spaceKey=?1 and p.status not in ?2")
    List<Page> findBySpaceKeyAndStatusNot(String spaceKey, List<String> status);

    @Query("select count(p) from Page p where p.spaceKey=?1 and p.status in ?2")
    int findBySpaceKeyAndStatus(String spaceKey, List<String> status);
}
