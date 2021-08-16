package com.confluence.exporter.service;

import com.confluence.exporter.model.Upload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * This service class holds several methods for further processing the
 * converted markdown file
 *
 * @author  Nandakumar12
 */
@Service
public class ParseHtml {

    @Autowired
    PageService pageService;

    @Value("${confluence.domain}")
    String confluenceDomain;

    @Autowired
    @Qualifier("restTemplate")
    RestTemplate restTemplate;

    @Autowired
    LinkParser linkParser;

    @Value("${confluence.slugs.confluence-attachment-slug}")
    private String CONFLUENCE_ATTACHMENT_SLUG;


    public static final Logger log = LogManager.getLogger(ParseHtml.class);

    /**
     * This method holds the main logic for parsing the markdown file
     * it fixes broken links, parses the attachments and userprofile urls
     *
     * @param filePath This the path of the markdown file which needs to be processed
     * @param pageId This is the confluence page id for the provided markdown file
     * @param gdriveHtmlFileName This is the name of the wiki page in gitlab, where the markdown contents
     *                         will be uploaded into
     * @return nothing
     *
     */
     Upload parse(Path filePath, String pageId, String pageSpaceKey, String gdriveHtmlFileName){
          log.info("parsing the html content of page {}",pageId);
        StringBuilder data = new StringBuilder();
        try(BufferedReader br= new BufferedReader(new FileReader(String.valueOf(filePath.toAbsolutePath())))){
            String line;
            while ((line = br.readLine())!=null){
                data.append(line).append("\n");
            }
        } catch (IOException e) {
            log.error(e);
        }
        String currentPageSlug = pageService.getPage(pageSpaceKey,pageId).get().getHtmlPageSlug();
        Document document = Jsoup.parse(data.toString(), confluenceDomain);
        log.info("extracting and replacing url of page {} in html",pageId);
        Elements links = document.select("a[href]");
        Elements medias = document.select("img[src]");
        String htmlName = filePath.getFileName().toString().replaceFirst("(.md)$","");
        JSONArray attachmentDetails = new JSONObject(restTemplate.getForObject(confluenceDomain+String.format(CONFLUENCE_ATTACHMENT_SLUG, pageId), String.class))
                .getJSONObject("children")
                .getJSONObject("attachment")
                .getJSONArray("results");


         Elements iconImgTags = document.select("img.icon");
         for(Element imgTag: iconImgTags){
             imgTag.remove();
         }

         for(Element link : links){
             try{
                 String newUrl = linkParser.replaceLink(link, filePath, currentPageSlug, attachmentDetails, htmlName, pageId).getFirst();
                 link.attr("href",newUrl);

             }catch (Exception e){
                 log.error("{} page-id {} {}", e.getMessage(),pageId, link);
                 log.error(e.toString());
             }
        }
         for(Element media : medias){
             try{
                 if (media.toString().contains("class=\"expand-control-image\"")) {
                     media.remove();
                 } else {
                     Pair<String, String> newUrlData = linkParser.replaceLink(media, filePath, currentPageSlug, attachmentDetails, htmlName, pageId);
                     media.removeAttr("src");
                     media.attr("href",newUrlData.getFirst());
                     media.text(newUrlData.getSecond());
                 }
             }catch (Exception e){
                 log.error("{} page-id {} {}",e.getMessage(),pageId, media);
                 log.error(e.toString());
             }
         }
         medias.tagName("a");
         try(BufferedWriter br= new BufferedWriter(new FileWriter(filePath.toAbsolutePath().toString()))){
             br.write(document.toString());
         } catch (IOException e) {
             log.error(e);
         }
         Upload upload = new Upload(pageSpaceKey, pageId, filePath.toAbsolutePath().toString(),gdriveHtmlFileName);
         log.info("data to be uploaded in gdrive {}",upload );

         return upload;


     }




}
