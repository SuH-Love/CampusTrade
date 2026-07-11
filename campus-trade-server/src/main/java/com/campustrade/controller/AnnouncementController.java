package com.campustrade.controller;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.entity.Announcement;
import com.campustrade.mapper.AnnouncementMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "系统公告接口")
@RestController
@RequestMapping("/api/announcement")
public class AnnouncementController {

    @Autowired
    private AnnouncementMapper announcementMapper;

    @ApiOperation("获取活跃公告（公开）")
    @GetMapping("/active")
    public Result<List<Announcement>> getActiveAnnouncements() {
        return Result.success(announcementMapper.selectActive());
    }

    @ApiOperation("公告列表（管理端）")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('goods:audit')")
    public Result<PageResult<Announcement>> listAnnouncements(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Announcement> list = announcementMapper.selectAll(offset, pageSize);
        Long total = announcementMapper.selectCount();
        return Result.success(new PageResult<>(list, total));
    }

    @ApiOperation("创建公告")
    @PostMapping
    @PreAuthorize("hasAuthority('goods:audit')")
    public Result<Void> createAnnouncement(@RequestBody Announcement announcement) {
        announcement.setId(null);
        if (announcement.getStatus() == null) announcement.setStatus(1);
        if (announcement.getSortOrder() == null) announcement.setSortOrder(0);
        announcementMapper.insert(announcement);
        return Result.success();
    }

    @ApiOperation("修改公告")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('goods:audit')")
    public Result<Void> updateAnnouncement(@PathVariable Long id, @RequestBody Announcement announcement) {
        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) return Result.error(404, "公告不存在");
        announcement.setId(id);
        announcement.setVersion(existing.getVersion());
        announcementMapper.updateById(announcement);
        return Result.success();
    }

    @ApiOperation("删除公告")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('goods:audit')")
    public Result<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementMapper.logicDeleteById(id);
        return Result.success();
    }
}