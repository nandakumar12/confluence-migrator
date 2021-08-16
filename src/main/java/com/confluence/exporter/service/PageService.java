package com.confluence.exporter.service;

import com.confluence.exporter.dao.PageDao;
import com.confluence.exporter.model.Page;
import com.confluence.exporter.model.PageId;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class PageService {

    @Autowired
    PageDao pageDao;

    public static final Logger log = LogManager.getLogger(PageService.class);


    public List<Page> getAllPages(){
        return (List<Page>) pageDao.findAll();
    }

    public long getTotalPageCount(){
        return pageDao.count();
    }


    @Transactional(readOnly = false)
    public Page createPage(String spaceKey, String pageId, String pageName, String htmlPageSlug){
      return  pageDao.save(new Page(spaceKey, pageId, pageName, htmlPageSlug, Page.status.NOT_STARTED.name()));
    }

    @Transactional(readOnly = false)
    public List<Page> createPages(List<Page> pages){
        Iterable<Page> iterable = pages;
        iterable =   pageDao.saveAll(iterable);
        return Lists.newArrayList(iterable);
    }

    public Optional<Page> getPage(String spaceKey, String pageId){
        return pageDao.findById(new PageId(spaceKey, pageId));
    }

    public void setStatus(String spaceKey, String pageId, String status){
        Optional<Page> foundPage = pageDao.findById(new PageId(spaceKey, pageId));
        if(foundPage.isPresent()){
            Page page = foundPage.get();
            page.setStatus(status);
            pageDao.save(page);
        }else{
            log.error("Can't find the given page "+spaceKey+" "+pageId);
        }

    }

    public Map<String, String> getMigrationStatusReport(String spaceKey) {
        int total_page_count = pageDao.findBySpaceKeyAndStatus(spaceKey, Arrays.asList("NOT_STARTED", "DONE", "CREATED_HIERARCHY"));
        int migrated_page_count = pageDao.findBySpaceKeyAndStatus(spaceKey, Arrays.asList("DONE"));
        int not_migrated_page_count = pageDao.findBySpaceKeyAndStatus(spaceKey, Arrays.asList("NOT_STARTED", "CREATED_HIERARCHY"));

        float ratio = ((migrated_page_count * 100.0f) / total_page_count);

        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("total pages", String.valueOf(total_page_count));
        statusMap.put("successfully migrated", String.valueOf(migrated_page_count));
        statusMap.put("maigration failures", String.valueOf(not_migrated_page_count));
        statusMap.put("success ratio", String.valueOf(ratio));

        return statusMap;
    }
}
