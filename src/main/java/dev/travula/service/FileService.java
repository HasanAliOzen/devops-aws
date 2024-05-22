package dev.travula.service;

import com.amazonaws.services.s3.model.S3Object;
import dev.travula.exceptions.FileDownloadException;
import dev.travula.exceptions.FileUploadException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
public interface FileService {
    String uploadFile(MultipartFile multipartFile) throws FileUploadException, IOException;

    Object downloadFile(String fileName) throws FileDownloadException, IOException;

    boolean delete(String fileName);

    S3Object getFile(String keyName);
}
