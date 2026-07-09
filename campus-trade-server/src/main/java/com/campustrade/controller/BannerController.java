package com.campustrade.controller;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.service.BannerService;
import com.campustrade.vo.BannerVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "横幅接口")
@RestController
@RequestMapping("/api/banner")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    @ApiOperation("获取活跃横幅(公开)")
    @GetMapping("/active")
    public Result<List<BannerVO>> listActiveBanners() {
        return bannerService.listActiveBanners();
    }

    @ApiOperation("横幅列表(管理端)")
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<PageResult<BannerVO>> listAllBanners(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return bannerService.listAllBanners(pageNum, pageSize);
    }

    @ApiOperation("创建横幅")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<BannerVO> createBanner(@RequestBody BannerCreateRequest req) {
        return bannerService.createBanner(req.title, req.subtitle, req.imageUrl, req.linkUrl, req.bgColor, req.buttonText, req.sortOrder, req.status);
    }

    @ApiOperation("修改横幅")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<BannerVO> updateBanner(@PathVariable Long id, @RequestBody BannerCreateRequest req) {
        return bannerService.updateBanner(id, req.title, req.subtitle, req.imageUrl, req.linkUrl, req.bgColor, req.buttonText, req.sortOrder, req.status);
    }

    @ApiOperation("删除横幅")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Void> deleteBanner(@PathVariable Long id) {
        return bannerService.deleteBanner(id);
    }

    @ApiOperation("切换横幅状态")
    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Void> toggleBannerStatus(@PathVariable Long id) {
        return bannerService.toggleBannerStatus(id);
    }

    @lombok.Data
    public static class BannerCreateRequest {
        private String title;
        private String subtitle;
        private String imageUrl;
        private String linkUrl;
        private String bgColor;
        private String buttonText;
        private Integer sortOrder;
        private Integer status;
    }
}