package com.campustrade.service;

import com.campustrade.common.Result;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    Result<String> uploadImage(MultipartFile file, Long userId);

    Result<Void> deleteImage(String fileUrl, Long userId);
}