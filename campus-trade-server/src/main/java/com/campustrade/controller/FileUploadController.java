package com.campustrade.controller;

import com.campustrade.aspect.RateLimit;
import com.campustrade.common.Result;
import com.campustrade.service.FileUploadService;
import com.campustrade.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Api(tags = "文件上传接口")
@RestController
@RequestMapping("/api/file")
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @ApiOperation("上传图片")
    @PostMapping("/upload")
    @RateLimit
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return fileUploadService.uploadImage(file, SecurityUtil.requireCurrentUserId());
    }

    @ApiOperation("删除图片")
    @DeleteMapping("/delete")
    public Result<Void> deleteImage(@RequestParam String fileUrl) {
        return fileUploadService.deleteImage(fileUrl, SecurityUtil.requireCurrentUserId());
    }
}
