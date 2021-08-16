package com.confluence.exporter.service;

import com.confluence.exporter.config.AppConfig;
import com.confluence.exporter.model.Attachment;
import com.confluence.exporter.model.Upload;
import com.google.api.client.util.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;



/**
 * This is service class which can be injected into other beans.
 * This class holds the logic for downloading/retrieving data from confluence
 *
 * @author  Nandakumar12
 */
@Service
public class ConfluenceDownload {

    @Autowired
    @Qualifier("restTemplate")
    RestTemplate restTemplate;

    @Autowired
    ParseHtml parseHtml;

    @Autowired
    DownloadService downloadService;

    @Value("${confluence.domain}")
    String confluenceDomain;

    @Value("${confluence.token}")
    String confluenceToken;

    @Value("${confluence.email}")
    String confluenceEmail;

    private static final String MD_DIRECTORY_PATH = "./htmls/";

    private static final String HTML_DIRECTORY_PATH = "./files/html/";

    @Value("${confluence.slugs.confluence-content-export-slug}")
    private String CONFLUENCE_CONTENT_EXPORT_SLUG;

    @Value("${confluence.slugs.confluence-attachment-slug}")
    private String CONFLUENCE_ATTACHMENT_SLUG;

    @Value("${confluence.slugs.confluence-metadeta-slug}")
    private String CONFLUENCE_METADATA_SLUG;
    public static final Logger log = LogManager.getLogger(ConfluenceDownload.class);

    Base64.Encoder enc = Base64.getEncoder();


    @PostConstruct
    void createFolders(){
        Path path1 = Paths.get("./files/attachments");
        Path path2 = Paths.get("./files/html");
        Path path3 = Paths.get("./htmls");
        try{
            Files.createDirectories(path1);
            Files.createDirectories(path2);
            Files.createDirectory(path3);
            log.info("The folder has been created");

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * This method will download an image and stores it in
     * the local filesystem using the confluence API
     *
     * @param imageUrl This is url which points to an image
     * @param fileName This is the file name with which the download image
     *                 will be saved in the local filesystem
     * @param htmlName This is the wiki name with which a folder will be
     *                 created in the filesystem to keep the attachments organized
     * @return String This returns the path in the local filesystem
     *                 where the image got downloaded
     *
     */
    public String image(String imageUrl, String fileName, String htmlName) throws IOException {
        String newFilename= fileName.replace(':','-');
        Files.createDirectories(Paths.get(MD_DIRECTORY_PATH + htmlName));
        FileOutputStream fileOutputStream=new FileOutputStream(MD_DIRECTORY_PATH + htmlName +"/"+newFilename+".png");
        String basicAuthenticationEncoded = Base64.getEncoder().encodeToString((confluenceEmail + ":" + confluenceToken).getBytes(StandardCharsets.UTF_8));
        URL url = new URL(imageUrl);
        URLConnection urlConnection = url.openConnection();
        urlConnection.setRequestProperty("Authorization", "Basic " + basicAuthenticationEncoded);
        IOUtils.copy(urlConnection.getInputStream(), fileOutputStream);
        return MD_DIRECTORY_PATH + htmlName +"/"+newFilename+".png";
    }


    /**
     * This method will retrieve the html content of an Confluence page
     * This is an asynchronous method, which takes the configuration from
     * the bean {@link AppConfig #asyncExecutor}
     * each call for this method will be executed in a separate thread
     *
     * @param pageId This is the confluence page id whose content should be retrived
     *
     * @return nothing
     *
     */
    @Async
    public CompletableFuture<Upload> retriveHTML(String pageId, String spaceKey, String gdriveHtmlFileName){
        String exportUrl = confluenceDomain+String.format(CONFLUENCE_CONTENT_EXPORT_SLUG,pageId);
        String token = enc.encodeToString((confluenceEmail + ":" + confluenceToken).getBytes());
        String response = restTemplate.getForObject(exportUrl, String.class);
        String metaDataUrl = confluenceDomain+CONFLUENCE_METADATA_SLUG+"/"+pageId;
        String metaDataResponse = restTemplate.getForObject(metaDataUrl, String.class);
        JSONObject metaDataJson = new JSONObject(metaDataResponse);
        JSONObject responseObj = new JSONObject(response);
        String baseUri = metaDataJson.getJSONObject("_links").getString("base");
        String webui = metaDataJson.getJSONObject("_links").getString("webui");
        //pageService.setStatus(spaceKey, pageId, Page.status.DOWNLOADING_DATA.name());
        String fileName = responseObj.getString("title").replaceAll("[^a-zA-Z0-9.\\-]", "_");
        String data = responseObj.getJSONObject("body").getJSONObject("export_view").getString("value");
        writeToFile(HTML_DIRECTORY_PATH+fileName+".html", data);
        Path filePath= Paths.get(HTML_DIRECTORY_PATH+fileName+".html");
       return CompletableFuture.completedFuture(parseHtml.parse(filePath, pageId, spaceKey, gdriveHtmlFileName));
    }



    /**
     * This is an helper method which creates a file in the local filesystem with
     * the provided content
     *
     * @param fileName This is the name of new file that needs to be created
     * @param data This is the content of the new file
     *
     * @return nothing
     *
     */
    void writeToFile(String fileName, String data){
        try(BufferedWriter br= new BufferedWriter(new FileWriter(new File(fileName)))){
            br.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This is an wrapper method of{@link DownloadService#downloadAttachment}
     * which helps in retrieving all the attachments for the given confluence page
     * and store it in the local filesystem
     *
     * @param id This the confluence page id whose attachment needs to be downloaded
     * @param htmlName This is the gitlab wiki name where the downloaded attachments needs
     *                 to be uploaded
     *
     * @return List<Attachment> This returns list of Attachment objects {@link Attachment}
     *
     */
    List<Attachment> retrieveAttachments(String id, String htmlName, String currentPageSlug) {
        List<Attachment> attachments = new ArrayList<>();
        String response  = restTemplate.getForObject(confluenceDomain+String.format(CONFLUENCE_ATTACHMENT_SLUG, id), String.class);
        JSONObject responeObj = new JSONObject(response);
        JSONArray arr = responeObj.getJSONObject("children").getJSONObject("attachment").getJSONArray("results");
        List<CompletableFuture<Attachment>> futureAttachments =  StreamSupport.stream(arr.spliterator(), false).map((item) ->{
            return downloadService.downloadAttachment(item, htmlName, currentPageSlug);
        }).collect(Collectors.toList());

        CompletableFuture<Void> allFutureAttachments = CompletableFuture.allOf(futureAttachments.toArray(new CompletableFuture[futureAttachments.size()]));
        CompletableFuture<List<Attachment>> allAttachments = allFutureAttachments.thenApply(v-> futureAttachments.stream().map(attachmentFuture -> attachmentFuture.join()).collect(Collectors.toList()));
        try {
            return allAttachments.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return attachments;

    }


}
