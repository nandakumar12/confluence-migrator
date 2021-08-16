package com.confluence.exporter.service;

import com.confluence.exporter.dao.GDrivePageDao;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@Service
public class GDriveService {
    @Autowired
    Drive drive;

    @Autowired
    GDrivePageDao gDrivePageDao;

    @Value("${gDrive.sharedDriveId}")
    private String sharedDriveId;

    public static final Logger log = LogManager.getLogger(GDriveService.class);

    @Async
    public CompletableFuture<String> uploadFile(String localFilePath, String folderId) throws IOException, URISyntaxException {
        Path path = Paths.get(localFilePath);
        byte[] bytes = Files.readAllBytes(path);
        String fileName = String.valueOf(path.getFileName());
        try {
            File fileMetadata = new File();
            fileMetadata.setParents(Collections.singletonList(folderId));
            fileMetadata.setName(fileName);
            File uploadFile = drive
                    .files()
                    .create(fileMetadata, new InputStreamContent(
                            Files.probeContentType(path),
                            new ByteArrayInputStream(bytes))
                    )
                    .setSupportsAllDrives(true)
                    .setFields("id")
                    .setFields("webViewLink")
                    .execute();
            log.info("uploaded "+uploadFile);
            return CompletableFuture.completedFuture(uploadFile.getWebViewLink());


        } catch (Exception e) {
            log.error("Error in uploading {}", localFilePath);
            log.error(e);
        }
        return null;
    }


    String createEmptyFile(String parentId, String fileName){
        byte[] bytes = new byte[0];
        String folderId = parentId;
        try {
            File fileMetadata = new File();
            fileMetadata.setParents(Collections.singletonList(folderId));
            fileMetadata.setMimeType("application/vnd.google-apps.document");
            fileMetadata.setName(fileName);
            File uploadFile = drive
                    .files()
                    .create(fileMetadata, new InputStreamContent(
                            "text/html",
                            new ByteArrayInputStream(bytes))
                    )
                    .setFields("*")
                    .setSupportsAllDrives(true)
                    .execute();
            log.info("uploaded "+uploadFile.getId());

            return uploadFile.getId();
        } catch (Exception e) {
            log.error("Error ", e);
        }
        return null;
    }

    public String findOrCreateFolder(String parentId, String folderName) throws Exception {
        String folderId = searchFolderId(parentId, folderName);
        if (folderId != null) {
            return folderId;
        }
        File fileMetadata = new File();
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        fileMetadata.setName(folderName);

        if (parentId != null) {
            fileMetadata.setParents(Collections.singletonList(parentId));
        }else{
            fileMetadata.setDriveId(sharedDriveId);
            fileMetadata.setParents(Collections.singletonList(sharedDriveId));
        }
        return drive.files().create(fileMetadata)
                .setSupportsAllDrives(true)
                .setFields("id")
                .execute()
                .getId();

    }

    public String createFolder(String parentId, String folderName, boolean isRoot) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        fileMetadata.setName(folderName);
        if(isRoot){
            fileMetadata.setDriveId(sharedDriveId);
            fileMetadata.setParents(Collections.singletonList(sharedDriveId));
        }else {
            fileMetadata.setParents(Collections.singletonList(parentId));
        }
        return drive.files().create(fileMetadata)
                .setSupportsAllDrives(true)
                .setFields("id")
                .execute()
                .getId();
    }


    private String searchFolderId(String parentId, String folderName) throws Exception {
        String folderId = null;
        String pageToken = null;
        FileList result = null;

        File fileMetadata = new File();
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        fileMetadata.setName(folderName);
        fileMetadata.setDriveId(sharedDriveId);

        do {
            String query = " mimeType = 'application/vnd.google-apps.folder' ";
            if (parentId == null) {
                query = query + " and '" + sharedDriveId + "' in parents and trashed = false";
            } else {
                query = query + " and '" + parentId + "' in parents and trashed = false";
            }
            result = drive.files().list().setQ(query)
                    .setSupportsAllDrives(true).setCorpora("drive")
                    .setDriveId(sharedDriveId)
                    .setIncludeItemsFromAllDrives(true)
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageToken(pageToken)
                    .execute();

            for (File file : result.getFiles()) {
                if (file.getName().equalsIgnoreCase(folderName)) {
                    folderId = file.getId();
                }
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null && folderId == null);

        return folderId;
    }

    @Async
    public CompletableFuture<File> updateFileWrapper(String fileId, String newFilename){
        int maxAttempt = 3;
        List<Exception> exceptions = new ArrayList<>();
        for(int attempted=0; attempted<maxAttempt; attempted++){
            try{
                if(attempted>0){
                    Thread.sleep(4000);
                }
                return updateFile(fileId, newFilename);
            }catch (Exception e){
                exceptions.add(e);
            }
        }
        log.error("failed after retrying ");
        throw new RuntimeException((Throwable) exceptions);
    }

    public CompletableFuture<File> updateFile(String fileId, String newFilename) throws IOException {

            File file = new File();
            java.io.File fileContent = new java.io.File(newFilename);
            FileContent mediaContent = new FileContent(Files.probeContentType(Paths.get(newFilename)), fileContent);
            File updatedFile = drive.files()
                    .update(fileId, file, mediaContent)
                    .setSupportsAllDrives(true)
                    .execute();
            log.info("updated the file {}",updatedFile);
            return CompletableFuture.completedFuture(updatedFile);
    }


    public void getFileList() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        FileList result = null;
        result = drive.files().list()
                .setSupportsAllDrives(true)
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name)")
                .execute();
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            log.info("No files found.");
        } else {
            log.info("Files:");
            for (File file : files) {
                log.info("{} ({})", file.getName(), file.getId());
            }
        }
    }

    public String findOrCreateAttachmentFolder(String parentId, String folderName) throws Exception {
        String folderId = searchFolderId(parentId, folderName);
        if(folderId != null){
            return folderId;
        }else {
            return createFolder(parentId, folderName, false);
        }

    }


}
