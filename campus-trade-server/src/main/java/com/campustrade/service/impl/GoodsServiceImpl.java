package com.campustrade.service.impl;

import com.campustrade.common.PageResult;
import com.campustrade.common.Result;
import com.campustrade.common.ResultCode;
import com.campustrade.constant.MQConstant;
import com.campustrade.constant.RedisConstant;
import com.campustrade.dto.GoodsCreateDTO;
import com.campustrade.dto.GoodsQueryDTO;
import com.campustrade.dto.GoodsUpdateDTO;
import com.campustrade.entity.Goods;
import com.campustrade.entity.GoodsCategory;
import com.campustrade.entity.GoodsFavorite;
import com.campustrade.entity.User;
import com.campustrade.enum_.GoodsStatus;
import com.campustrade.mapper.GoodsMapper;
import com.campustrade.mapper.GoodsCategoryMapper;
import com.campustrade.mapper.GoodsFavoriteMapper;
import com.campustrade.mapper.UserMapper;
import com.campustrade.mapper.UserFollowMapper;
import com.campustrade.service.GoodsService;
import com.campustrade.service.LogService;
import com.campustrade.service.NotificationService;
import com.campustrade.service.ai.FaqVectorService;
import com.campustrade.vo.GoodsVO;
import com.campustrade.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GoodsServiceImpl implements GoodsService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private GoodsCategoryMapper categoryMapper;

    @Autowired
    private GoodsFavoriteMapper favoriteMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private LogService logService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserFollowMapper userFollowMapper;

    @Autowired
    private FaqVectorService faqVectorService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<GoodsVO> createGoods(Long userId, GoodsCreateDTO dto) {
        Goods goods = new Goods();
        goods.setUserId(userId);
        goods.setCategoryId(dto.getCategoryId());
        goods.setTitle(dto.getTitle());
        goods.setDescription(dto.getDescription());
        goods.setPrice(dto.getPrice());
        goods.setOriginalPrice(dto.getOriginalPrice());
        goods.setCoverImage(dto.getCoverImage());
        goods.setImages(dto.getImages());
        goods.setCondition(dto.getCondition());
        goods.setStock(dto.getStock() != null && dto.getStock() > 0 ? dto.getStock() : 1);
        goods.setStatus(GoodsStatus.DRAFT.getCode());
        goods.setViewCount(0);
        goods.setFavoriteCount(0);
        goodsMapper.insert(goods);
        return Result.success(toVO(goods));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<GoodsVO> updateGoods(Long userId, Long goodsId, GoodsUpdateDTO dto) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) return Result.error(ResultCode.GOODS_NOT_FOUND);
        if (!goods.getUserId().equals(userId)) return Result.error(ResultCode.GOODS_NOT_OWNER);
        String oldStatus = goods.getStatus();
        if (dto.getCategoryId() != null) goods.setCategoryId(dto.getCategoryId());
        if (dto.getTitle() != null) goods.setTitle(dto.getTitle());
        if (dto.getDescription() != null) goods.setDescription(dto.getDescription());
        if (dto.getPrice() != null) goods.setPrice(dto.getPrice());
        if (dto.getOriginalPrice() != null) goods.setOriginalPrice(dto.getOriginalPrice());
        if (dto.getCoverImage() != null) goods.setCoverImage(dto.getCoverImage());
        if (dto.getImages() != null) goods.setImages(dto.getImages());

        boolean needReaudit = GoodsStatus.ONLINE.getCode().equals(oldStatus)
                || GoodsStatus.OFFLINE.getCode().equals(oldStatus)
                || GoodsStatus.APPROVED.getCode().equals(oldStatus);
        if (needReaudit) {
            goods.setStatus(GoodsStatus.PENDING.getCode());
        }

        int rows = goodsMapper.updateById(goods);
        if (rows == 0) return Result.error(ResultCode.DATA_VERSION_ERROR);
        redisTemplate.delete(RedisConstant.GOODS_DETAIL_PREFIX + goodsId);

        if (needReaudit) {
            redisTemplate.delete("mq:consumed:goods:audit:" + goodsId);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rabbitTemplate.convertAndSend(MQConstant.GOODS_AUDIT_EXCHANGE, MQConstant.GOODS_AUDIT_KEY, goodsId);
                }
            });
        }

        return Result.success(toVO(goods));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteGoods(Long userId, Long goodsId) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) return Result.error(ResultCode.GOODS_NOT_FOUND);
        if (!goods.getUserId().equals(userId)) return Result.error(ResultCode.GOODS_NOT_OWNER);
        goodsMapper.logicDeleteById(goodsId);
        redisTemplate.delete(RedisConstant.GOODS_DETAIL_PREFIX + goodsId);
        return Result.success();
    }

    @Override
    public Result<GoodsVO> getGoodsDetail(Long goodsId, Long currentUserId) {
        String cacheKey = RedisConstant.GOODS_DETAIL_PREFIX + goodsId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            if (cached instanceof String && "NULL".equals(cached)) {
                return Result.error(ResultCode.GOODS_NOT_FOUND);
            }
            GoodsVO vo = (GoodsVO) cached;
            if (vo.getCategoryName() == null && vo.getCategoryId() != null) {
                GoodsCategory cat = categoryMapper.selectById(vo.getCategoryId());
                if (cat != null) vo.setCategoryName(cat.getCategoryName());
            }
            if (currentUserId != null) {
                GoodsFavorite fav = favoriteMapper.selectByUserAndGoods(currentUserId, goodsId);
                vo.setIsFavorited(fav != null);
            } else {
                vo.setIsFavorited(false);
            }
            Goods fresh = goodsMapper.selectById(goodsId);
            if (fresh != null) {
                vo.setViewCount(fresh.getViewCount());
                vo.setFavoriteCount(fresh.getFavoriteCount());
            }
            return Result.success(vo);
        }

        String lockKey = RedisConstant.LOCK_GOODS_PREFIX + goodsId;
        int maxRetry = 30;
        for (int i = 0; i < maxRetry; i++) {
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
            if (locked != null && locked) {
                try {
                    Goods goods = goodsMapper.selectById(goodsId);
                    if (goods == null) {
                        redisTemplate.opsForValue().set(cacheKey, "NULL", 60, TimeUnit.SECONDS);
                        return Result.error(ResultCode.GOODS_NOT_FOUND);
                    }

                    goodsMapper.incrementViewCount(goodsId);
                    Goods updated = goodsMapper.selectById(goodsId);
                    GoodsVO vo = toVO(updated != null ? updated : goods);

                    if (vo.getCategoryId() != null) {
                        GoodsCategory cat = categoryMapper.selectById(vo.getCategoryId());
                        if (cat != null) vo.setCategoryName(cat.getCategoryName());
                    }

                    User seller = userMapper.selectById(goods.getUserId());
                    if (seller != null) {
                        vo.setUsername(seller.getNickname() != null ? seller.getNickname() : seller.getUsername());
                        vo.setUserAvatar(seller.getAvatar());
                        vo.setSellerRealVerified(seller.getRealVerified());
                    }

                    if (currentUserId != null) {
                        GoodsFavorite fav = favoriteMapper.selectByUserAndGoods(currentUserId, goodsId);
                        vo.setIsFavorited(fav != null);
                    }

                    redisTemplate.opsForValue().set(cacheKey, vo, RedisConstant.GOODS_DETAIL_TTL, TimeUnit.SECONDS);
                    return Result.success(vo);
                } finally {
                    redisTemplate.delete(lockKey);
                }
            }
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
        return Result.error(ResultCode.GOODS_NOT_FOUND);
    }

    @Override
    public Result<PageResult<GoodsVO>> listGoods(GoodsQueryDTO dto) {
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        String status = dto.getStatus();
        if (status == null && dto.getUserId() == null) {
            status = GoodsStatus.ONLINE.getCode();
        }

        // 特殊关键词处理
        if (dto.getKeyword() != null) {
            String kw = dto.getKeyword().trim();
            if ("今日上新".equals(kw)) {
                dto.setTodayOnly(true);
                dto.setKeyword(null);
            } else if ("低价好物".equals(kw)) {
                dto.setSortBy("price_asc");
                dto.setKeyword(null);
                List<java.math.BigDecimal> prices = goodsMapper.selectOnlinePrices();
                if (!prices.isEmpty()) {
                    dto.setMaxPrice(prices.get(prices.size() / 2));
                }
            }
        }

        List<GoodsVO> vos;
        Long total = goodsMapper.selectCount(dto.getCategoryId(), dto.getKeyword(),
                dto.getMinPrice(), dto.getMaxPrice(), status, dto.getUserId(), dto.getTodayOnly());

        if (dto.getKeyword() != null && !dto.getKeyword().trim().isEmpty() && total > 0) {
            int fetchSize = offset + dto.getPageSize() * 2;
            int candidateLimit = Math.min((int)(long) total, fetchSize);
            List<GoodsVO> candidates = goodsMapper.selectListVO(dto.getCategoryId(), dto.getKeyword(),
                    dto.getMinPrice(), dto.getMaxPrice(), status, dto.getUserId(), 0, candidateLimit,
                    dto.getTodayOnly(), dto.getSortBy());
            int rankLimit = Math.min(offset + dto.getPageSize(), candidates.size());
            List<GoodsVO> ranked = faqVectorService.rankBySimilarity(dto.getKeyword(), candidates,
                    vo -> (vo.getTitle() != null ? vo.getTitle() : "") + " " +
                         (vo.getDescription() != null ? vo.getDescription() : ""),
                    rankLimit);
            int start = Math.min(offset, ranked.size());
            int end = Math.min(offset + dto.getPageSize(), ranked.size());
            vos = ranked.subList(start, end);
        } else {
            vos = goodsMapper.selectListVO(dto.getCategoryId(), dto.getKeyword(),
                    dto.getMinPrice(), dto.getMaxPrice(), status, dto.getUserId(), offset, dto.getPageSize(),
                    dto.getTodayOnly(), dto.getSortBy());
        }
        return Result.success(new PageResult<>(vos, total));
    }

    @Override
    public Result<PageResult<GoodsVO>> hotGoods() {
        String cacheKey = RedisConstant.GOODS_HOT_KEY;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return Result.success((PageResult<GoodsVO>) cached);

        List<GoodsVO> vos = goodsMapper.selectHotGoodsVO(20);
        PageResult<GoodsVO> result = new PageResult<>(vos, (long) vos.size());
        redisTemplate.opsForValue().set(cacheKey, result, RedisConstant.GOODS_HOT_TTL, TimeUnit.SECONDS);
        return Result.success(result);
    }

    @Override
    public Result<PageResult<GoodsVO>> recommendGoods() {
        String cacheKey = RedisConstant.GOODS_RECOMMEND_KEY;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return Result.success((PageResult<GoodsVO>) cached);

        List<GoodsVO> vos = goodsMapper.selectRecommendGoodsVO(20);
        PageResult<GoodsVO> result = new PageResult<>(vos, (long) vos.size());
        redisTemplate.opsForValue().set(cacheKey, result, RedisConstant.GOODS_RECOMMEND_TTL, TimeUnit.SECONDS);
        return Result.success(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> submitAudit(Long userId, Long goodsId) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) return Result.error(ResultCode.GOODS_NOT_FOUND);
        if (!goods.getUserId().equals(userId)) return Result.error(ResultCode.GOODS_NOT_OWNER);
        if (!GoodsStatus.DRAFT.getCode().equals(goods.getStatus()) && !GoodsStatus.REJECTED.getCode().equals(goods.getStatus()))
            return Result.error(ResultCode.GOODS_STATUS_ERROR);
        goods.setStatus(GoodsStatus.PENDING.getCode());
        int rows = goodsMapper.updateById(goods);
        if (rows == 0) return Result.error(ResultCode.DATA_VERSION_ERROR);
        redisTemplate.delete("mq:consumed:goods:audit:" + goodsId);

        final Long mqGoodsId = goodsId;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rabbitTemplate.convertAndSend(MQConstant.GOODS_AUDIT_EXCHANGE, MQConstant.GOODS_AUDIT_KEY, mqGoodsId);
            }
        });
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> auditGoods(Long goodsId, String status, String rejectReason) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) return Result.error(ResultCode.GOODS_NOT_FOUND);
        if (GoodsStatus.APPROVED.getCode().equals(status)) {
            goods.setStatus(GoodsStatus.APPROVED.getCode());
            goods.setRejectReason(null);
        } else {
            goods.setStatus(status);
        }
        if (GoodsStatus.REJECTED.getCode().equals(status)) goods.setRejectReason(rejectReason);
        int rows = goodsMapper.updateById(goods);
        if (rows == 0) return Result.error(ResultCode.DATA_VERSION_ERROR);
        if (GoodsStatus.APPROVED.getCode().equals(status)) goodsMapper.clearRejectReason(goodsId);
        redisTemplate.delete(RedisConstant.GOODS_DETAIL_PREFIX + goodsId);
        redisTemplate.delete(RedisConstant.GOODS_HOT_KEY);
        redisTemplate.delete(RedisConstant.GOODS_RECOMMEND_KEY);

        if (GoodsStatus.APPROVED.getCode().equals(status)) {
            notificationService.sendNotification(goods.getUserId(), "商品审核通过",
                    "您的商品「" + goods.getTitle() + "」已通过审核，可上架出售", "GOODS", goodsId);
        } else if (GoodsStatus.REJECTED.getCode().equals(status)) {
            notificationService.sendNotification(goods.getUserId(), "商品审核未通过",
                    "您的商品「" + goods.getTitle() + "」未通过审核，原因：" + (rejectReason != null ? rejectReason : "无"), "GOODS", goodsId);
        }

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> onlineGoods(Long userId, Long goodsId) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) return Result.error(ResultCode.GOODS_NOT_FOUND);
        if (!goods.getUserId().equals(userId)) return Result.error(ResultCode.GOODS_NOT_OWNER);
        if (!GoodsStatus.APPROVED.getCode().equals(goods.getStatus()) && !GoodsStatus.OFFLINE.getCode().equals(goods.getStatus()))
            return Result.error(ResultCode.GOODS_STATUS_ERROR);
        goods.setStatus(GoodsStatus.ONLINE.getCode());
        int rows = goodsMapper.updateById(goods);
        if (rows == 0) return Result.error(ResultCode.DATA_VERSION_ERROR);
        redisTemplate.delete(RedisConstant.GOODS_DETAIL_PREFIX + goodsId);
        redisTemplate.delete(RedisConstant.GOODS_HOT_KEY);
        redisTemplate.delete(RedisConstant.GOODS_RECOMMEND_KEY);

        List<Long> followerIds = userFollowMapper.selectFollowerIds(goods.getUserId(), 0, 1000);
        for (Long followerId : followerIds) {
            notificationService.sendNotification(followerId, "关注商家新品",
                    "您关注的商家发布了新商品「" + goods.getTitle() + "」", "FOLLOW", goodsId);
        }

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> offlineGoods(Long userId, Long goodsId) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) return Result.error(ResultCode.GOODS_NOT_FOUND);
        if (!goods.getUserId().equals(userId)) return Result.error(ResultCode.GOODS_NOT_OWNER);
        if (!GoodsStatus.ONLINE.getCode().equals(goods.getStatus())) return Result.error(ResultCode.GOODS_STATUS_ERROR);
        goods.setStatus(GoodsStatus.OFFLINE.getCode());
        int rows = goodsMapper.updateById(goods);
        if (rows == 0) return Result.error(ResultCode.DATA_VERSION_ERROR);
        redisTemplate.delete(RedisConstant.GOODS_DETAIL_PREFIX + goodsId);
        redisTemplate.delete(RedisConstant.GOODS_HOT_KEY);
        redisTemplate.delete(RedisConstant.GOODS_RECOMMEND_KEY);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> favoriteGoods(Long userId, Long goodsId) {
        GoodsFavorite exist = favoriteMapper.selectByUserAndGoods(userId, goodsId);
        if (exist != null) return Result.error(ResultCode.GOODS_ALREADY_FAVORITED);
        GoodsFavorite fav = new GoodsFavorite();
        fav.setUserId(userId);
        fav.setGoodsId(goodsId);
        favoriteMapper.insert(fav);
        goodsMapper.incrementFavoriteCount(goodsId);
        redisTemplate.delete(RedisConstant.GOODS_DETAIL_PREFIX + goodsId);
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> unfavoriteGoods(Long userId, Long goodsId) {
        favoriteMapper.deleteByUserAndGoods(userId, goodsId);
        goodsMapper.decrementFavoriteCount(goodsId);
        redisTemplate.delete(RedisConstant.GOODS_DETAIL_PREFIX + goodsId);
        return Result.success();
    }

    @Override
    public Result<PageResult<GoodsVO>> listFavoriteGoods(Long userId, Integer pageNum, Integer pageSize, String keyword, String status) {
        int offset = (pageNum - 1) * pageSize;
        List<GoodsVO> vos = favoriteMapper.selectFavoriteGoodsVOByUserId(userId, keyword, status, offset, pageSize);
        Long total = favoriteMapper.selectFavoriteCountByUserId(userId, keyword, status);
        return Result.success(new PageResult<>(vos, total));
    }

    @Override
    public Result<PageResult<GoodsVO>> listGoodsByAdmin(GoodsQueryDTO dto) {
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        List<GoodsVO> vos = goodsMapper.selectListVO(dto.getCategoryId(), dto.getKeyword(),
                dto.getMinPrice(), dto.getMaxPrice(), dto.getStatus(), dto.getUserId(), offset, dto.getPageSize(),
                dto.getTodayOnly(), dto.getSortBy());
        Long total = goodsMapper.selectCount(dto.getCategoryId(), dto.getKeyword(),
                dto.getMinPrice(), dto.getMaxPrice(), dto.getStatus(), dto.getUserId(), dto.getTodayOnly());
        return Result.success(new PageResult<>(vos, total));
    }

    private GoodsVO toVO(Goods goods) {
        GoodsVO vo = new GoodsVO();
        vo.setId(goods.getId());
        vo.setUserId(goods.getUserId());
        vo.setCategoryId(goods.getCategoryId());
        vo.setTitle(goods.getTitle());
        vo.setDescription(goods.getDescription());
        vo.setPrice(goods.getPrice());
        vo.setOriginalPrice(goods.getOriginalPrice());
        vo.setCondition(goods.getCondition());
        vo.setCoverImage(goods.getCoverImage());
        vo.setImages(goods.getImages());
        vo.setStatus(goods.getStatus());
        vo.setRejectReason(goods.getRejectReason());
        vo.setViewCount(goods.getViewCount());
        vo.setFavoriteCount(goods.getFavoriteCount());
        vo.setStock(goods.getStock());
        vo.setCreateTime(goods.getCreateTime());
        return vo;
    }

    private List<GoodsVO> toVOList(List<Goods> goodsList) {
        if (goodsList == null || goodsList.isEmpty()) return List.of();

        List<Long> userIds = goodsList.stream().map(Goods::getUserId).distinct().collect(Collectors.toList());
        List<Long> categoryIds = goodsList.stream().map(Goods::getCategoryId).distinct().collect(Collectors.toList());

        Map<Long, User> userMap = userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, GoodsCategory> categoryMap = categoryMapper.selectByIds(categoryIds).stream()
                .collect(Collectors.toMap(GoodsCategory::getId, c -> c));

        return goodsList.stream().map(goods -> {
            GoodsVO vo = toVO(goods);
            User user = userMap.get(goods.getUserId());
            if (user != null) {
                vo.setUsername(user.getNickname() != null ? user.getNickname() : user.getUsername());
                vo.setUserAvatar(user.getAvatar());
                vo.setSellerRealVerified(user.getRealVerified());
            }
            GoodsCategory cat = categoryMap.get(goods.getCategoryId());
            if (cat != null) vo.setCategoryName(cat.getCategoryName());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public long countGoods() {
        Long count = goodsMapper.selectCountAll();
        return count != null ? count : 0L;
    }

    @Override
    public long countPendingAudit() {
        Long count = goodsMapper.selectCountByStatus(GoodsStatus.PENDING.getCode());
        return count != null ? count : 0L;
    }

}