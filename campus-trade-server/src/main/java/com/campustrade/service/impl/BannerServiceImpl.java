package com.campustrade.service.impl;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.entity.Banner;
import com.campustrade.mapper.BannerMapper;
import com.campustrade.service.BannerService;
import com.campustrade.vo.BannerVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BannerServiceImpl implements BannerService {

    @Autowired
    private BannerMapper bannerMapper;

    @Override
    public Result<List<BannerVO>> listActiveBanners() {
        List<Banner> banners = bannerMapper.selectActive();
        List<BannerVO> vos = banners.stream().map(this::toVO).collect(Collectors.toList());
        return Result.success(vos);
    }

    @Override
    public Result<PageResult<BannerVO>> listAllBanners(Integer pageNum, Integer pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<Banner> banners = bannerMapper.selectAll(offset, pageSize);
        Long total = bannerMapper.selectCount();
        List<BannerVO> vos = banners.stream().map(this::toVO).collect(Collectors.toList());
        return Result.success(new PageResult<>(vos, total));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<BannerVO> createBanner(String title, String subtitle, String imageUrl, String linkUrl, String bgColor, Integer sortOrder, Integer status) {
        Banner banner = new Banner();
        banner.setTitle(title);
        banner.setSubtitle(subtitle);
        banner.setImageUrl(imageUrl);
        banner.setLinkUrl(linkUrl);
        banner.setBgColor(bgColor);
        banner.setSortOrder(sortOrder != null ? sortOrder : 0);
        banner.setStatus(status != null ? status : 1);
        bannerMapper.insert(banner);
        return Result.success(toVO(banner));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<BannerVO> updateBanner(Long id, String title, String subtitle, String imageUrl, String linkUrl, String bgColor, Integer sortOrder, Integer status) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) return Result.error(404, "横幅不存在");
        if (title != null) banner.setTitle(title);
        if (subtitle != null) banner.setSubtitle(subtitle);
        if (imageUrl != null) banner.setImageUrl(imageUrl);
        if (linkUrl != null) banner.setLinkUrl(linkUrl);
        if (bgColor != null) banner.setBgColor(bgColor);
        if (sortOrder != null) banner.setSortOrder(sortOrder);
        if (status != null) banner.setStatus(status);
        bannerMapper.updateById(banner);
        return Result.success(toVO(banner));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteBanner(Long id) {
        bannerMapper.logicDeleteById(id);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> toggleBannerStatus(Long id) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) return Result.error(404, "横幅不存在");
        banner.setStatus(banner.getStatus() == 1 ? 0 : 1);
        bannerMapper.updateById(banner);
        return Result.success();
    }

    private BannerVO toVO(Banner banner) {
        BannerVO vo = new BannerVO();
        vo.setId(banner.getId());
        vo.setTitle(banner.getTitle());
        vo.setSubtitle(banner.getSubtitle());
        vo.setImageUrl(banner.getImageUrl());
        vo.setLinkUrl(banner.getLinkUrl());
        vo.setBgColor(banner.getBgColor());
        vo.setSortOrder(banner.getSortOrder());
        vo.setStatus(banner.getStatus());
        vo.setCreateTime(banner.getCreateTime() != null ? banner.getCreateTime().toString() : null);
        return vo;
    }
}