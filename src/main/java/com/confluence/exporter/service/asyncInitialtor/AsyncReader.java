package com.confluence.exporter.service.asyncInitialtor;

import com.confluence.exporter.model.Space;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutionException;

@Service
@Async("asyncExecutor")
public class AsyncReader{

    @Autowired
    @Qualifier("restTemplate")
    RestTemplate restTemplate;

    @Value("${confluence.domain}")
    String confluenceDomain;

    @Autowired
    AsyncProcessor processor;

    @Value("${confluence.slugs.confluence-space-slug}")
    String CONFLUENCE_SPACE_SLUG;


    public static final Logger log = LogManager.getLogger(AsyncReader.class);


    public void initializeData(String spaceKey, boolean uploadToGdrive, boolean retry){
        log.info("Initializing post construct data");
        if(spaceKey == null){
            String response = restTemplate.getForObject(confluenceDomain + CONFLUENCE_SPACE_SLUG, String.class);
            JSONObject resObj = new JSONObject(response);
            resObj.getJSONArray("results").forEach((space) -> {
                JSONObject pageObj = (JSONObject) space;
                String sKey = pageObj.getString("key");
                String spaceName = pageObj.getString("name");
                Space tempSpace = new Space(spaceKey);
                log.info("stated processing {}",tempSpace);
                try {
                    processor.process(tempSpace, uploadToGdrive, retry);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            });
        }else{
            Space tempSpace = new Space(spaceKey);
            try {
                log.info("stated processing {}",tempSpace);
                processor.process(tempSpace, uploadToGdrive, retry);
            } catch (ExecutionException | InterruptedException e) {
                log.error(e.getMessage());
                log.error("can't able to process "+tempSpace);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

}