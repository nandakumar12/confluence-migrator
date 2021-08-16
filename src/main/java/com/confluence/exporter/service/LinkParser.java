package com.confluence.exporter.service;

import com.confluence.exporter.dao.GDrivePageDao;
import com.confluence.exporter.dao.JiraIssueDao;
import com.confluence.exporter.dao.PageDao;
import com.confluence.exporter.model.Attachment;
import com.confluence.exporter.model.GDrivePage;
import com.confluence.exporter.model.JiraIssue;
import com.confluence.exporter.model.Page;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@Service
public class LinkParser {

    public static final String GDRIVE_DOCUMENT_URL = "https://docs.google.com/document/d/";

    @Value("${confluence.domain}")
    String confluenceDomain;

    static String CONFLUENCE_DOMAIN;

    @Autowired
    PageService pageService;

    @Lazy
    @Autowired
    ConfluenceDownload confluenceDownloadService;

    @Autowired
    GDriveService gDriveService;

    @Autowired
    DownloadService downloadService;

    @Autowired
    GDrivePageDao gDrivePageDao;

    @Autowired
    JiraIssueDao jiraIssueDao;

    @Autowired
    PageDao pageDao;

    public static final Logger log = LogManager.getLogger(LinkParser.class);

    @Value("${confluence.domain}")
    public static void setConfluenceDomain(String confluenceDomain) {
        CONFLUENCE_DOMAIN = confluenceDomain;
    }


    static String slugRegex = "%s\\/confluence\\/spaces\\/([a-zA-Z]*)\\/pages\\/([0-9]*)\\/[a-zA-Z+0-9-~]*$";
    static Pattern slugPattern = Pattern.compile(String.format(slugRegex, CONFLUENCE_DOMAIN));

    // \/confluence\/display\/([~A-Za-z]*)\/([A-Za-z-0-9+%._]*)$
    static String slugRegex2 = "\\/confluence\\/display\\/([~A-Za-z]*)\\/([A-Za-z-0-9+%._]*)(\\?([A-Za-z+=&])*)?$";
    static Pattern slugPattern2 = Pattern.compile(slugRegex2);

    static String slugRegex3 = "\\/confluence\\/pages\\/viewpage.action\\?pageId=([0-9]*)$";
    static Pattern slugPattern3 = Pattern.compile(slugRegex3);

    static String slugRegex4 = "\\/confluence\\/pages\\/viewpage.action\\?spaceKey=([~_A-Za-z]*)&title=([~_A-Za-z]*)";
    static Pattern slugPattern4 = Pattern.compile(slugRegex4);

    static String imageRegex = "([a-z0-9.\\-_]*)(.png)?(?:\\?[a-z=0-9]*)?$";
    static Pattern imagePattern = Pattern.compile(imageRegex);

    static String usernameRegex = "\\/confluence\\/people\\/([0-9:a-z-]*)";
    static Pattern usernamePattern = Pattern.compile(usernameRegex);

    static String attachmentRegex = "\\/confluence\\/download\\/attachments\\/([0-9a-z-\\/A-Z%.]*)\\/([_A-Za-z0-9\\.%-:]*)";
    static Pattern attachmentPattern = Pattern.compile(attachmentRegex);

    static String attachmentRegex2 = "\\/confluence\\/display\\/([A-Za-z]*)\\/([A-Za-z+0-9_]*)\\?preview=([A-Za-z%0-9]*)%2F([A-Za-z+._0-9-]*)";
    static Pattern attachmentPattern2 = Pattern.compile(attachmentRegex2);

    static String attachmentRegex3 = "\\/confluence\\/pages\\/viewpage.action\\?pageId=([0-9]*)&preview=([A-Za-z%0-9]*)%2F([A-Za-z+._0-9-]*)";
    static Pattern attachmentPattern3 = Pattern.compile(attachmentRegex3);

    static String jiraRegex = "\\/jira\\/browse\\/([A-Z-0-9a-z]*)";
    static Pattern jiraPattern = Pattern.compile(jiraRegex);

    Pair<String, String> replaceLink(Element link, Path filePath, String currentPageSlug, JSONArray attachmentDetails, String htmlName, String pageId){
        String extractedUrl = null;
        if(link.tagName().equals("img")){
            extractedUrl = link.attr("abs:src");
        }else if(link.tagName().equals("a")){
            extractedUrl = link.attr("abs:href");
        }
        Pair<String, String> replacedUrl = null;

        Matcher slugMatcher = slugPattern.matcher(extractedUrl);

        Matcher slugMatcher2 = slugPattern2.matcher(extractedUrl);

        Matcher slugMatcher3 = slugPattern3.matcher(extractedUrl);

        Matcher slugMatcher4 = slugPattern4.matcher(extractedUrl);

        Matcher imageMatcher = imagePattern.matcher(extractedUrl);
        List<String> possibleImageSlugs = Arrays.asList("image", "aa-avatar", "png");
        String finalExtractedUrl = extractedUrl;
        boolean isImage = possibleImageSlugs.stream().anyMatch(finalExtractedUrl::contains);

        Matcher usernameMatcher = usernamePattern.matcher(extractedUrl);

        Matcher attachmentMatcher = attachmentPattern.matcher(extractedUrl);

        Matcher attachmentMatcher2 = attachmentPattern2.matcher(extractedUrl);

        Matcher attachmentMatcher3 = attachmentPattern3.matcher(extractedUrl);

        Matcher jiraMatcher = jiraPattern.matcher(extractedUrl);

        if(slugMatcher.find()){
            replacedUrl = slugLinkReplacer(slugMatcher);
        }else if(slugMatcher2.find()){
            replacedUrl = pageLinkReplacer(slugMatcher2);
        }else if(slugMatcher3.find()){
            replacedUrl = slugLinkReplacer3(slugMatcher3);
        }else if(slugMatcher4.find()){
            replacedUrl = pageLinkReplacer(slugMatcher4);
        }else if(isImage && imageMatcher.find()){
            try {
                replacedUrl = imageLinkReplacer(imageMatcher, extractedUrl, filePath, pageId);
            } catch (IOException e) {
                log.error("Cant able to download image");
                log.error(e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else if(usernameMatcher.find()){
            replacedUrl = usernameLinkReplacer(usernameMatcher);

        }else if(attachmentMatcher.find()){
            try {
                replacedUrl = attachmentLinkReplacer(attachmentMatcher, attachmentDetails, htmlName, pageId);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                log.error(e.getMessage());
                log.error("Cant able to download attachment");
            }
        }else if(attachmentMatcher2.find()){
            String fileName = UriUtils.decode(attachmentMatcher2.group(4), StandardCharsets.UTF_8).replace('+', ' ');
            try {
                replacedUrl = attachmentLinkReplacer2(fileName, attachmentDetails, htmlName, pageId);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                log.error(e.getMessage());
                log.error("Cant able to download attachment");
            }
        }else if(attachmentMatcher3.find()){
            String fileName = UriUtils.decode(attachmentMatcher3.group(3), StandardCharsets.UTF_8).replace('+', ' ');
            try {
                replacedUrl = attachmentLinkReplacer2(fileName, attachmentDetails, htmlName, pageId);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

        }
        else if(jiraMatcher.find()){
            replacedUrl = jiraLinkReplacer(jiraMatcher);
        }else{
            replacedUrl = Pair.of(extractedUrl, "");
        }
        return replacedUrl;
    }


    Pair<String, String> slugLinkReplacer(Matcher url) {
        String spaceKey = url.group(1);
        String id = url.group(2);
        Optional<Page> page = pageService.getPage(spaceKey, id);
        String pageId = "";
        String htmlPageName = "";
        if(page.isPresent()){
            try{
                htmlPageName = String.join("",page.get().getPageName().split(" "))+".html";
                pageId = gDrivePageDao.findTopByPageSlugContaining("/"+htmlPageName).getgDriveId();
            }catch (Exception e){
                log.error("slug exception for {}",htmlPageName);
                log.error(e);
            }

        }
        return Pair.of("https://docs.google.com/document/d/"+pageId, htmlPageName);
    }


    Pair<String, String> pageLinkReplacer(Matcher url){
        String pageName = url.group(2);
        String decodedUrlPageName =  UriUtils.decode(pageName, StandardCharsets.UTF_8).replace('+',' ')
                .replaceAll("[^a-zA-Z0-9.\\-]", "_");
        log.info("replacing page link {}",decodedUrlPageName);
        String pageId = gDrivePageDao.findTopByPageSlugContaining("/"+decodedUrlPageName).getgDriveId();
        return Pair.of(GDRIVE_DOCUMENT_URL +pageId, decodedUrlPageName);

    }

    Pair<String, String> slugLinkReplacer3(Matcher url){
        String pageId = url.group(1);
        Optional<Page> data = pageDao.findPageByPageId(pageId);
        if(data.isPresent()){
            String[] pagePath = data.get().getHtmlPageSlug().split("/");
            String pageSlug = pagePath[pagePath.length-1]+".html";
            GDrivePage gDrivePage = gDrivePageDao.findTopByPageSlugContaining("/"+pageSlug);
            return Pair.of(GDRIVE_DOCUMENT_URL+ gDrivePage.getgDriveId(), pageId);
        }else{
            return Pair.of(GDRIVE_DOCUMENT_URL+pageId, pageId);
        }
    }

    Pair<String, String> imageLinkReplacer(Matcher url, String imageUrl, Path filePath, String pageId) throws Exception {
        log.info(imageUrl);
        String htmlName = filePath.getFileName().toString().replaceFirst("(.md)$","");
        String imgPathName = "";
        String fileName = url.group(1);
        if(imageUrl.contains(".net") || imageUrl.contains(".com")){
            imgPathName = confluenceDownloadService.image(imageUrl, fileName, htmlName);
        }else{
            imgPathName = confluenceDownloadService.image(confluenceDomain + imageUrl, fileName, htmlName);
        }
        String parentId = gDrivePageDao.findGDrivePageByPageId(pageId).get().getParentFolderId();
        String imagesFolderId = gDriveService.findOrCreateAttachmentFolder(parentId, "images");
        return Pair.of(gDriveService.uploadFile(imgPathName, imagesFolderId).get(), fileName);
    }

    Pair<String, String> usernameLinkReplacer(Matcher url) {
        String userId = url.group(1);
        //TODO: need to add logic for retrieving google user name from pre-populated db
        return Pair.of("username-placeholder", userId);
    }



    Pair<String, String> attachmentLinkReplacer(Matcher url, JSONArray attachmentDetails, String htmlName, String pageId) throws ExecutionException, InterruptedException {
        try{
            String fileName = UriUtils.decode(url.group(2),StandardCharsets.UTF_8).replace('+',' ');
            JSONObject attachmentData = StreamSupport.stream(attachmentDetails.spliterator(), false)
                    .map(val -> (JSONObject) val)
                    .filter((attachmentDetail)->attachmentDetail.getString("title").equals(fileName))
                    .findFirst()
                    .get();
            Attachment attachment = downloadService.downloadAttachment(attachmentData, htmlName, pageId).get();
            return Pair.of(attachment.getUrl(), fileName);
        }catch (Exception e){
            String fileUrl = confluenceDomain+url.group();
            String fileName = UriUtils.decode(url.group(2),StandardCharsets.UTF_8).replace('+',' ').replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
            return Pair.of(downloadService.downloadRawFile(fileUrl, htmlName, fileName, pageId).get().getUrl(), fileName);
        }


    }

    Pair<String, String> attachmentLinkReplacer2(String fileName, JSONArray attachmentDetails, String htmlName, String pageId) throws ExecutionException, InterruptedException {
            JSONObject attachmentData = StreamSupport.stream(attachmentDetails.spliterator(), false)
                    .map(val -> (JSONObject) val)
                    .filter((attachmentDetail) -> attachmentDetail.getString("title").equals(fileName))
                    .findFirst()
                    .get();
            Attachment attachment = downloadService.downloadAttachment(attachmentData, htmlName, pageId).get();
            return Pair.of(attachment.getUrl(), fileName);

    }


    Pair<String, String> jiraLinkReplacer(Matcher url){
        String issueId = url.group(1);
        String jiraUrl = "";
        Optional<JiraIssue> jiraIssue = jiraIssueDao.findById(issueId);
        if(jiraIssue.isPresent()){
            jiraUrl = jiraIssue.get().getUrl();
        }else{
            jiraUrl = "https://gitlab.com/iron-mountain1/placeholder/"+issueId;
        }
        return Pair.of(jiraUrl, issueId);
    }
}
