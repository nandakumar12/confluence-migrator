package com.confluence.exporter.service;

import com.confluence.exporter.dao.PageDao;
import com.confluence.exporter.model.Page;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;


@Service
public class GDriveStructureCreator {

    @Autowired
    PageDao pageDao;

    @Autowired
    PageService pageService;

    public static final Logger log = LogManager.getLogger(GDriveStructureCreator.class);

    public String createHierarchy(String spaceKey, boolean retry){
        List<Page> pages = null;
        if(!retry){
            if(spaceKey!=null){
                pages = pageDao.findPageBySpaceKey(spaceKey);
            }else{
                pages = pageService.getAllPages();
            }
        }else {
            pages = pageDao.findBySpaceKeyAndStatusNot(spaceKey,
                    Arrays.asList(Page.status.CREATED_HIERARCHY.name(), Page.status.DONE.name()));
        }

        long totalPageCount = pages.size();
        AtomicLong currentPageCount = new AtomicLong();
        pages.forEach(page -> {
            String pageSlug = page.getHtmlPageSlug();
            String folderId = null;
            try {
                log.info("creating page in gdrive {}/{}", currentPageCount.incrementAndGet(),totalPageCount);
                pageService.setStatus(page.getSpaceKey(),page.getPageId(), Page.status.CREATED_HIERARCHY.name());
            } catch (Exception e) {
                log.error(e.getMessage());
                log.error("Cant able to create folder in gdrive folder-id {}",pageSlug);
            }
        });
        return "Created confluence hierarchy in gdrive";
    }
}
