package com.confluence.exporter.service;

import com.confluence.exporter.dao.GDrivePageDao;
import com.confluence.exporter.model.Attachment;
import com.google.api.client.util.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * This is service class which can be injected into other beans.
 * This class holds the logic for downloading/retrieving data from confluence page and
 * processes pages in the given confluence space
 *
 * @author  Nandakumar12
 */
@Service
public class DownloadService {

    @Autowired
    @Qualifier("restTemplate")
    RestTemplate restTemplate;

    @Value("${confluence.domain}")
    String confluenceDomain;

    @Autowired
    GDriveService gDriveService;

    @Value("${confluence.token}")
    String confluenceToken;

    @Value("${confluence.email}")
    String confluenceEmail;

    @Autowired
    GDrivePageDao gDrivePageDao;

    private static final String MD_ATTACHMENT_DIRECTORY = "./files/attachments/";

    @Value("${confluence.slugs.confluence-content-slug}")
    private String CONFLUENCE_CONTENT_SLUG ;

    public static final Logger log = LogManager.getLogger(DownloadService.class);

    /**
     * This method holds the asynchronous logic for downloading an attachment
     * This is wrapped by {@link ConfluenceDownload#retrieveAttachments}
     * to download attachments in bulk
     *
     * @param attachment This is an json object which contains the details about a single attachment
     * @param htmlName This is the destination gitlab wiki name where the downloaded attachments needs
     *                 to be uploaded
     *
     * @return CompletableFuture<Attachment> This returns Attachment object{@link Attachment}
     * wrapped by CompletableFuture stating that it will be finished sometime in future
     *
     */
    public CompletableFuture<Attachment> downloadAttachment(Object attachment, String htmlName, String pageId) {
        log.info("downloading attachment...");
        JSONObject obj = (JSONObject) attachment;
        log.info("download attachment url {}",confluenceDomain+"/confluence"+obj.getJSONObject("_links").getString("download"));
        try {
            Files.createDirectories(Paths.get(MD_ATTACHMENT_DIRECTORY + htmlName +"/"));
            downloadFile(confluenceDomain+"/confluence"+obj.getJSONObject("_links").getString("download"), MD_ATTACHMENT_DIRECTORY + htmlName +"/"+obj.getString("title"));
        } catch (IOException e) {
            log.error("can't able to download file from link {}",e.getMessage());
        }
        String attachmentUrl = null;
        try {
            String parentId = gDrivePageDao.findGDrivePageByPageId(pageId).get().getParentFolderId();
            String attachmentsFolderId = gDriveService.findOrCreateAttachmentFolder(parentId, "Attachments");
            attachmentUrl = gDriveService.uploadFile(MD_ATTACHMENT_DIRECTORY + htmlName + "/" + obj.getString("title"), attachmentsFolderId ).get();
        } catch (IOException e) {
            log.error("Cant able to upload file to gdrive ");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(new Attachment(obj.getString("title"),attachmentUrl)) ;
    }



    public CompletableFuture<Attachment> downloadRawFile(String url, String htmlName, String fileName, String pageId) {
        try {
            Files.createDirectories(Paths.get(MD_ATTACHMENT_DIRECTORY + htmlName +"/"));
            downloadFile(url, MD_ATTACHMENT_DIRECTORY + htmlName +"/"+fileName);
        } catch (IOException e) {
            log.error(e);
        }
        String attachmentUrl = null;
        try {

            String parentId = gDrivePageDao.findGDrivePageByPageId(pageId).get().getParentFolderId();
            String attachmentsFolderId = gDriveService.findOrCreateAttachmentFolder(parentId, "Attachments");
            attachmentUrl = gDriveService.uploadFile(MD_ATTACHMENT_DIRECTORY + htmlName + "/" + fileName, attachmentsFolderId ).get();
        } catch (IOException e) {
            log.error("Cant able to upload file to gdrive ");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(new Attachment(fileName,attachmentUrl)) ;
    }



    /**
     * This method will process all the pages present in the given space
     *
     * @param spaceName This the name of the confluence space whose pages needs to be processed
     *
     * @return nothing
     *
     */
    public void processPages(String spaceName){
        String response = restTemplate.getForObject(confluenceDomain+String.format(CONFLUENCE_CONTENT_SLUG, spaceName), String.class);
        JSONObject resObj = new JSONObject(response);
        resObj.getJSONObject("page").getJSONArray("results").forEach((page)->{
            JSONObject pageObj = (JSONObject) page;
            log.info("Processing "+pageObj.getString("id"));
        });

    }


    public void downloadFile(String attachmentUrl, String filePath) throws IOException {
        FileOutputStream fileOutputStream=new FileOutputStream(filePath);
        String basicAuthenticationEncoded = Base64.getEncoder().encodeToString((confluenceEmail + ":" + confluenceToken).getBytes("UTF-8"));
        URL url = new URL(attachmentUrl);
        URLConnection urlConnection = url.openConnection();
        urlConnection.setRequestProperty("Authorization", "Basic " + basicAuthenticationEncoded);
        IOUtils.copy(urlConnection.getInputStream(), fileOutputStream);
    }

}

