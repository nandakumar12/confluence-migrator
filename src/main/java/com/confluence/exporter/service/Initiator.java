package com.confluence.exporter.service;

import com.confluence.exporter.model.Node;
import com.confluence.exporter.service.asyncInitialtor.AsyncReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.atomic.AtomicReference;


/**
 * This is an service class which initiates the main parsing and conversion logic
 * due to the fact that async methods needs to be proxied by spring, those classes with
 * asynchronous methods will be injected and invoked from this class
 *
 * @author  Nandakumar12
 */
@Service
public class Initiator {

    @Autowired
    @Qualifier("restTemplate")
    RestTemplate restTemplate;


    @Value("${confluence.domain}")
    String confluenceDomain;

    @Lazy
    @Autowired
    StructureBuilder structureBuilder;

    @Value("${confluence.slugs.confluence-space-slug}")
    String CONFLUENCE_SPACE_SLUG;

    @Autowired
    GDriveStructureCreator gDriveStructureCreator;

    @Autowired
    AsyncReader reader;

    public static final Logger log = LogManager.getLogger(Initiator.class);


    /**
     * This methods kick starts the processing of confluence spaces
     * it retrieves list of all the spaces in the give confluence domain
     * and starts processing
     *
     * @param inputSpaceKey This is the unique space identifier for an confluence space
     * if the both the params are null then it will start processing all the spaces in the
     *                        specified domain, else this methods will just process the provided
     *                        space
     * @return nothing
     *
     */
    public void processSpace(String inputSpaceKey, boolean updateDb) throws Exception {
        log.info("started processing space {}",inputSpaceKey);
        AtomicReference<Node> root = new AtomicReference<>();
        String response = restTemplate.getForObject(confluenceDomain + CONFLUENCE_SPACE_SLUG, String.class);
        JSONObject resObj = new JSONObject(response);        
        if (inputSpaceKey != null ) {
            structureBuilder.buildStructure(inputSpaceKey, updateDb);
        } else {
            resObj.getJSONArray("results").forEach((space) -> {
                JSONObject pageObj = (JSONObject) space;
                String spaceKey = pageObj.getString("key");
                String spaceName = pageObj.getString("name");
                try {
                    structureBuilder.buildStructure(spaceKey, updateDb);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void startWholeProcessing(String spaceKey, boolean uploadToGdrive) throws Exception {
        log.info("building hierarchy for space {}",spaceKey);
        processSpace(spaceKey, true);
        log.info("finished building hierarchy");
        log.info("downloading and parsing html files of space {}",spaceKey);
        reader.initializeData(spaceKey, uploadToGdrive, false);

    }

    public void retryProcessing(String spaceKey){
        gDriveStructureCreator.createHierarchy(spaceKey, true);
        reader.initializeData(spaceKey, true, true);
    }


}
