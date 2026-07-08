package com.campustrade.service;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.dto.GoodsCreateDTO;
import com.campustrade.dto.GoodsQueryDTO;
import com.campustrade.dto.GoodsUpdateDTO;
import com.campustrade.vo.GoodsVO;

public interface GoodsService {

    Result<GoodsVO> createGoods(Long userId, GoodsCreateDTO dto);

    Result<GoodsVO> updateGoods(Long userId, Long goodsId, GoodsUpdateDTO dto);

    Result<Void> deleteGoods(Long userId, Long goodsId);

    Result<GoodsVO> getGoodsDetail(Long goodsId, Long currentUserId);

    Result<PageResult<GoodsVO>> listGoods(GoodsQueryDTO dto);

    Result<PageResult<GoodsVO>> hotGoods();

    Result<PageResult<GoodsVO>> recommendGoods();

    Result<Void> submitAudit(Long userId, Long goodsId);

    Result<Void> auditGoods(Long goodsId, String status, String rejectReason);

    Result<Void> onlineGoods(Long userId, Long goodsId);

    Result<Void> offlineGoods(Long userId, Long goodsId);

    Result<Void> favoriteGoods(Long userId, Long goodsId);

    Result<Void> unfavoriteGoods(Long userId, Long goodsId);

    Result<PageResult<GoodsVO>> listFavoriteGoods(Long userId, Integer pageNum, Integer pageSize);

    Result<PageResult<GoodsVO>> listGoodsByAdmin(GoodsQueryDTO dto);

    long countGoods();

    long countPendingAudit();
}