package dev.travula.service;

import dev.travula.exceptions.FileDownloadException;
import dev.travula.exceptions.FileUploadException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
public interface FileService {
    String uploadFile(MultipartFile multipartFile) throws FileUploadException, IOException;

    Object downloadFile(String fileName) throws FileDownloadException, IOException;

    boolean delete(String fileName);
}
