package dev.travula.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import dev.travula.exceptions.FileDownloadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    @Value("${aws.bucket.name}")
    private String bucketName;

    private final AmazonS3 s3Client;

    public S3Object getFile(String keyName) {
        return s3Client.getObject(bucketName, keyName);
    }

    @Override
    public String uploadFile(MultipartFile multipartFile) throws IOException {
        // convert multipart file  to a file
        File file = new File(Objects.requireNonNull(multipartFile.getOriginalFilename()));
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)){
            fileOutputStream.write(multipartFile.getBytes());
        }

        // generate file name
        String fileName = generateFileName(multipartFile);

        // upload file
        PutObjectRequest request = new PutObjectRequest(bucketName, fileName, file);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("plain/"+ FilenameUtils.getExtension(multipartFile.getOriginalFilename()));
        metadata.addUserMetadata("Title", "File Upload - " + fileName);
        metadata.setContentLength(file.length());
        request.setMetadata(metadata);
        s3Client.putObject(request);

        // delete file
        file.delete();

        return fileName;
    }

    @Override
    public Object downloadFile(String fileName) throws FileDownloadException, IOException {
        if (bucketIsEmpty()) {
            throw new FileDownloadException("Requested bucket does not exist or is empty");
        }
        S3Object s3Object = s3Client.getObject(bucketName, fileName);

        S3ObjectInputStream inputStream = s3Object.getObjectContent();
        FileOutputStream fos = new FileOutputStream("src/main/resources/static/images/"+fileName);
        byte[] read_buf = new byte[1024];
        int read_len = 0;
        while ((read_len = inputStream.read(read_buf)) > 0) {
            fos.write(read_buf, 0, read_len);
        }
        inputStream.close();
        fos.close();

        Path pathObject = Paths.get("src/main/resources/static/images/"+fileName);
        Resource resource = new UrlResource(pathObject.toUri());

        if (resource.exists() || resource.isReadable()) {
            return resource;
        } else {
            throw new FileDownloadException("Could not find the file!");
        }

        /*try (S3ObjectInputStream s3is = object.getObjectContent()) {
            try (FileOutputStream fileOutputStream = new FileOutputStream("src/main/resources/static/images/"+fileName)) {
                byte[] read_buf = new byte[1024];
                int read_len = 0;
                while ((read_len = s3is.read(read_buf)) > 0) {
                    fileOutputStream.write(read_buf, 0, read_len);
                }
            }
            s3is.close();
            Path pathObject = Paths.get("src/main/resources/static/images/"+fileName);
            Resource resource = new UrlResource(pathObject.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new FileDownloadException("Could not find the file!");
            }



        }*/
    }

    @Override
    public boolean delete(String fileName) {
        File file = Paths.get(fileName).toFile();
        if (file.exists()) {
            file.delete();
            return true;
        }
        return false;
    }

    private boolean bucketIsEmpty() {
        ListObjectsV2Result result = s3Client.listObjectsV2(this.bucketName);
        if (result == null){
            return false;
        }
        List<S3ObjectSummary> objects = result.getObjectSummaries();
        return objects.isEmpty();
    }

    private String generateFileName(MultipartFile multiPart) {
        return new Date().getTime() + "-" + Objects.requireNonNull(multiPart.getOriginalFilename()).replace(" ", "_");
    }
}
