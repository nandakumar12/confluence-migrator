package com.confluence.exporter.controller;


import com.confluence.exporter.model.Page;
import com.confluence.exporter.service.GDriveStructureCreator;
import com.confluence.exporter.service.Initiator;
import com.confluence.exporter.service.PageService;
import com.confluence.exporter.service.asyncInitialtor.AsyncReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
public class Controller {

    @Autowired
    Initiator initiator;

    @Autowired
    PageService pageService;

    @Autowired
    AsyncReader reader;

    @Autowired
    GDriveStructureCreator gDriveStructureCreator;

    public static final Logger log = LogManager.getLogger(Controller.class);


    @GetMapping(value="/start/processing/")
    String beginConfluenceProcessing() throws Exception {
         initiator.processSpace(null, false);
         return "Successfully parsed files";
    }

    @GetMapping(value = "/process/pages")
    String buildPages() throws Exception {
        initiator.processSpace(null, true);
        return "db populated with slugs";
    }

    @GetMapping(value = "/process/page/{spaceKey}")
    String buildSingleSpacePages(@PathVariable("spaceKey")String spaceKey) throws Exception {
        initiator.processSpace(spaceKey, true);
        return "db populated with slugs";
    }

    @GetMapping(value = "/get/pages")
    List<Page> getPages(){
        return pageService.getAllPages();
    }


    @RequestMapping("async/start/job")
    public String handleAsync() throws Exception {
        reader.initializeData(null, true, false);

        return "Batch job has been invoked";
    }

    @RequestMapping("async/start/job/{spaceKey}")
    public String handleAsyncSingleSpace(@PathVariable("spaceKey")String spaceKey) throws Exception {
        reader.initializeData(spaceKey, true, false);

        return "Batch job has been invoked";
    }

    @GetMapping(value="/create/gdrive/hierarchy")
    public String createGdriveHierarchy(){
        return gDriveStructureCreator.createHierarchy(null, false);
    }

    @GetMapping(value="/create/gdrive/hierarchy/{spaceKey}")
    public String createGdriveHierarchySingleSpace(@PathVariable("spaceKey")String spaceKey){
        return gDriveStructureCreator.createHierarchy(spaceKey, false);
    }

    @GetMapping(value = "/process")
    public String startWholeProcessing(@RequestParam(value = "spaceKey",required = false)String spaceKey, @RequestParam("uploadGdrive") boolean uploadToGdrive ) throws Exception {
        initiator.startWholeProcessing(spaceKey, uploadToGdrive);
        return "processed the given space";
    }

    @GetMapping(value = "/retry/{spaceKey}")
    public String retryProcessing(@PathVariable("spaceKey")String spaceKey){
        initiator.retryProcessing(spaceKey);
        return "retrying "+spaceKey;
    }

    @GetMapping(value = "/space/{spaceKey}/report")
    public Map<String, String> getMigrationReport(@PathVariable("spaceKey")String spaceKey){
        return pageService.getMigrationStatusReport(spaceKey);
    }

}
