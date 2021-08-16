package com.confluence.exporter.service.asyncInitialtor;

import com.confluence.exporter.dao.GDrivePageDao;
import com.confluence.exporter.model.Page;
import com.confluence.exporter.model.Upload;
import com.confluence.exporter.service.GDriveService;
import com.confluence.exporter.service.PageService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Async("asyncExecutor")
public class AsyncWriter{

    @Autowired
    PageService pageService;

    @Autowired
    GDriveService gDriveService;

    @Autowired
    GDrivePageDao gDrivePageDao;

    public static final Logger log = LogManager.getLogger(AsyncWriter.class);

    public void write(Upload upload) throws Exception
    {
        Path path = Paths.get(upload.getFilePath());
        String fileId = gDrivePageDao.findGDrivePageByPageId(upload.getPageId()).get().getgDriveId();
        log.info("the file is {} with id {}",path.getFileName(), fileId);
        gDriveService.updateFileWrapper(fileId, upload.getFilePath()).thenRun(()->{
                pageService.setStatus(upload.getSpaceKey(), upload.getPageId(), Page.status.DONE.name());
                log.info("finished uploading the content of page {}",upload.getPageId());
            });
        }
    }
