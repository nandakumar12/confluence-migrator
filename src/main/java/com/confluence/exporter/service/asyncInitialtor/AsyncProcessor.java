package com.confluence.exporter.service.asyncInitialtor;

import com.confluence.exporter.dao.PageDao;
import com.confluence.exporter.model.Page;
import com.confluence.exporter.model.Space;
import com.confluence.exporter.service.ConfluenceDownload;
import com.confluence.exporter.service.PageService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Async("asyncExecutor")
@Service
public class AsyncProcessor  {

    @Autowired
    ConfluenceDownload confluenceDownload;

    @Autowired
    PageService pageService;

    @Autowired
    PageDao pageDao;

    @Autowired
    AsyncWriter writer;

    public static final Logger log = LogManager.getLogger(AsyncProcessor.class);

    public void process(Space space, boolean uploadToGdrive, boolean retry) throws Exception {
        log.info("the space going to be processed {}",space);
        long totalCount = pageService.getTotalPageCount();
        AtomicLong currentPageCount = new AtomicLong();
        List<Page> pages;
        if(!retry){
            pages = pageDao.findPageBySpaceKey(space.getSpaceKey());
        }else{
            pages = pageDao.findBySpaceKeyAndStatusNot(space.getSpaceKey(),
                    Collections.singletonList(Page.status.DONE.name()));
        }
        pages.forEach(pageDetail->{
            log.info("processing page {}/{}",currentPageCount.incrementAndGet(),totalCount);
            log.info("retrieving html for page {}, slug {}",pageDetail.getPageId(), pageDetail.getHtmlPageSlug());
             confluenceDownload.retriveHTML(pageDetail.getPageId(),pageDetail.getSpaceKey(), pageDetail.getHtmlPageSlug())
                     .thenAccept(upload -> {
                         if(uploadToGdrive){
                             try {
                                 log.info("uploading content of page {} to gdrive",pageDetail.getPageId());
                                 writer.write(upload);
                             } catch (Exception e) {
                                 e.printStackTrace();
                             }
                         }

                     });
        });


    }

}