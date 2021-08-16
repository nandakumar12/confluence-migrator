package com.confluence.exporter.service;

import com.confluence.exporter.dao.GDrivePageDao;
import com.confluence.exporter.model.GDrivePage;
import com.confluence.exporter.model.Node;
import com.confluence.exporter.model.Page;
import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


/**
 * This service class holds several methods for creating the hierarchy structure of
 * the confluence space, so that the same structure will be maintained in the
 * gitlab too
 *
 * @author  Nandakumar12
 */
@Service
public class StructureBuilder {

    @Autowired
    @Qualifier("restTemplate")
    RestTemplate restTemplate;

    @Value("${confluence.domain}")
    String confluenceDomain;

    @Value("${confluence.slugs.confluence-child-page-slug}")
    String confluenceChildPage;

    @Value("${confluence.slugs.confluence-space-slug}")
    String confluenceSpaceDetail;

    @Value("${confluence.slugs.confluence-metadeta-slug}")
    String confluenceMetaData;

    @Autowired
    PageService pageService;

    @Autowired
    NodeService nodeService;

    @Autowired
    GDriveService gDriveService;

    @Autowired
    GDrivePageDao gDrivePageDao;


    public static Logger log = LogManager.getLogger(StructureBuilder.class);

    /**
     * This method will build a General Tree structure from the json data returned
     * by confluence api, so that we can able to maintain the same hierarchy structure among
     * the gitlab wikis
     *
     * @param spaceKey This the confluence space identifier for which we need to build the structure
     * @return nothing
     *
     */

    public Node buildStructure(String spaceKey, boolean updateDb) throws Exception {

        String spaceDetail = restTemplate.getForObject(confluenceDomain+confluenceSpaceDetail+spaceKey, String.class );
        String homePageUrl = new JSONObject(spaceDetail).getJSONObject("_expandable").getString("homepage");
        int index = homePageUrl.lastIndexOf("/");
        String homePageId = homePageUrl.substring(index+1);
        log.info("homepage id {} ",homePageId);
        String homePageName = new JSONObject(restTemplate.getForObject(confluenceDomain+confluenceMetaData+"/"+homePageId,String.class)).getString("title");
        Node root= nodeService.createNode(new Node(homePageName.replaceAll("[^a-zA-Z0-9.\\-]", "_"),homePageId,spaceKey, null,new HashSet<>()));


        String childPageUrl = confluenceDomain+confluenceChildPage;
        childPageUrl = String.format(childPageUrl, root.getId());
        List<Page> result = new ArrayList<>();

        result.add(new Page(root.getSpaceKey(), homePageId, root.getTitle(), root.getTitle(), Page.status.NOT_STARTED.name()));
        String fileName = root.getTitle().replace(" ","")+".html";
        String rootFolderId = gDriveService.createFolder(null, root.getTitle(), true);
        String fileId = gDriveService.createEmptyFile(rootFolderId, fileName);
        gDrivePageDao.save(new GDrivePage(root.getTitle()+"/"+fileName, fileId, homePageId, rootFolderId));

        formHierarchy(root, childPageUrl, result, root.getTitle(), rootFolderId);

        if(updateDb){

            for (List<Page> tmpList: Lists.partition(result, 500)) {
                 pageService.createPages(tmpList);
            }

            log.info("tree has been built");
            log.info("creating page slugs");
        }
        log.info("populated database with pageslugs");
        return root;

    }


    void formHierarchy(Node parent, String nextPage, List<Page> result, String slug, String parentFoldedGdriveId){
        JSONObject childPages = new JSONObject(restTemplate.getForObject(nextPage, String.class));
        JSONArray childPageArr = childPages.getJSONArray("results");
        JSONObject nextLink = childPages.getJSONObject("_links");

        childPageArr.forEach(o ->{

            JSONObject page = (JSONObject) o;
            String pageId = page.getString("id");
            String pageTitle = page.getString("title").replaceAll("[^a-zA-Z0-9.\\-]", "_");

            String fileName = pageTitle.replace(" ","")+".html";
            String parentId = null;
            try {
                parentId = gDriveService.createFolder(parentFoldedGdriveId, pageTitle, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String fileId = gDriveService.createEmptyFile(parentId, fileName);
            String tmpSlug = slug+"/"+pageTitle;
            gDrivePageDao.save(new GDrivePage(tmpSlug+"/"+fileName, fileId, pageId, parentId ));

            Node child = nodeService.createNode(new Node(pageTitle.replaceAll("[^a-zA-Z0-9.\\-]", "_"),pageId, parent.getSpaceKey(), null,new HashSet<>()));
            nodeService.setChild(parent, child);
            String childPageUrl = confluenceDomain+confluenceChildPage;
            childPageUrl = String.format(childPageUrl, child.getId());
            result.add(new Page(parent.getSpaceKey(), pageId, pageTitle, tmpSlug, Page.status.NOT_STARTED.name()));
            formHierarchy(child, childPageUrl, result, tmpSlug , parentId);
        } );


        String base = nextLink.getString("base");
        String nextPageId;
        try {
            nextPageId = nextLink.getString("next");
        } catch (org.json.JSONException jsonException) {
            nextPageId = null;
        }

        if (nextPageId != null) {
            nextPage = base + nextPageId;
            formHierarchy(parent, nextPage, result, slug, parentFoldedGdriveId);
        }

    }
}
