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
            byte[] fileBytes = file.getBytes();
            if (!isValidImageMagicBytes(fileBytes, originalFilename)) {
                throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文件内容与类型不匹配");
            }
            Path fullPath = Paths.get(uploadBasePath, relativePath);
            Files.createDirectories(fullPath.getParent());
            Files.write(fullPath, fileBytes);
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
        Path basePath = Paths.get(uploadBasePath).toAbsolutePath().normalize();
        Path fullPath = basePath.resolve(relativePath).normalize();
        if (!fullPath.startsWith(basePath)) throw new com.campustrade.exception.BusinessException("非法路径");
        if (!fullPath.startsWith(Paths.get(uploadBasePath, String.valueOf(userId)))) throw new com.campustrade.exception.BusinessException("无权删除");
        String sanitizedPath = FileUploadUtil.sanitizePath(relativePath);
        if (!sanitizedPath.startsWith(userId + "/")) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权删除该文件");
        }
        try {
            Files.deleteIfExists(fullPath);
            log.info("文件删除成功: userId={}, fileUrl={}", userId, fileUrl);
            return Result.success();
        } catch (IOException e) {
            log.error("文件删除失败: userId={}, fileUrl={}", userId, fileUrl, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR.getCode(), "文件删除失败");
        }
    }

    private boolean isValidImageMagicBytes(byte[] bytes, String originalFilename) {
        if (bytes == null || bytes.length < 4) return false;
        String ext = originalFilename != null ? originalFilename.toLowerCase() : "";
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) return ext.endsWith(".jpg") || ext.endsWith(".jpeg");
        if (bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) return ext.endsWith(".png");
        if (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46) return ext.endsWith(".gif");
        if (bytes[0] == 0x42 && bytes[1] == 0x4D) return ext.endsWith(".bmp");
        if (bytes.length >= 12 && bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46 && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50) return ext.endsWith(".webp");
        return false;
    }
}