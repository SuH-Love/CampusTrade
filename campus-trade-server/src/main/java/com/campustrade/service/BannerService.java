package com.campustrade.service;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.vo.BannerVO;

import java.util.List;

public interface BannerService {

    Result<List<BannerVO>> listActiveBanners();

    Result<PageResult<BannerVO>> listAllBanners(Integer pageNum, Integer pageSize);

    Result<BannerVO> createBanner(String title, String subtitle, String imageUrl, String linkUrl, String bgColor, String buttonText, Integer sortOrder, Integer status);

    Result<BannerVO> updateBanner(Long id, String title, String subtitle, String imageUrl, String linkUrl, String bgColor, String buttonText, Integer sortOrder, Integer status);

    Result<Void> deleteBanner(Long id);

    Result<Void> toggleBannerStatus(Long id);
}