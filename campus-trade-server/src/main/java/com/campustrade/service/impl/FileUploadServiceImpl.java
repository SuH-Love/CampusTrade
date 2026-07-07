package com.campustrade.service.impl;

import com.campustrade.common.Result;
import com.campustrade.common.ResultCode;
import com.campustrade.exception.BusinessException;
import com.campustrade.service.FileUploadService;
import com.campustrade.util.FileUploadUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Value("${file.upload.path:/data/uploads}")
    private String uploadBasePath;

    @Value("${file.upload.url-prefix:/uploads}")
    private String urlPrefix;

    @Override
    public Result<String> uploadImage(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (!FileUploadUtil.isAllowedExtension(originalFilename)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "不支持的文件类型，仅允许 jpg/jpeg/png/gif/bmp/webp");
        }

        if (!FileUploadUtil.isAllowedMimeType(file.getContentType())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "不支持的MIME类型");
        }

        if (!FileUploadUtil.isFileSizeAllowed(file.getSize())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文件大小不能超过10MB");
        }

        String safeFilename = FileUploadUtil.generateSafeFilename(originalFilename);
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = userId + "/" + datePath + "/" + safeFilename;

        try {
            Path fullPath = Paths.get(uploadBasePath, relativePath);
            Files.createDirectories(fullPath.getParent());
            Files.copy(file.getInputStream(), fullPath);
            String fileUrl = urlPrefix + "/" + relativePath;
            log.info("文件上传成功: userId={}, fileUrl={}", userId, fileUrl);
            return Result.success(fileUrl);
        } catch (IOException e) {
            log.error("文件上传失败: userId={}, filename={}", userId, originalFilename, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "文件上传失败");
        }
    }

    @Override
    public Result<Void> deleteImage(String fileUrl, Long userId) {
        if (fileUrl == null || !fileUrl.startsWith(urlPrefix)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "无效的文件URL");
        }
        String relativePath = fileUrl.substring(urlPrefix.length() + 1);
        String sanitizedPath = FileUploadUtil.sanitizePath(relativePath);
        if (!sanitizedPath.startsWith(userId + "/")) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权删除该文件");
        }
        try {
            Path fullPath = Paths.get(uploadBasePath, relativePath);
            Files.deleteIfExists(fullPath);
            log.info("文件删除成功: userId={}, fileUrl={}", userId, fileUrl);
            return Result.success();
        } catch (IOException e) {
            log.error("文件删除失败: userId={}, fileUrl={}", userId, fileUrl, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "文件删除失败");
        }
    }
}