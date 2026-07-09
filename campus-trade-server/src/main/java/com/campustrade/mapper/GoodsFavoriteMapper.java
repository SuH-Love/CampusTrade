package com.campustrade.mapper;

import com.campustrade.entity.GoodsFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GoodsFavoriteMapper {

    GoodsFavorite selectByUserAndGoods(@Param("userId") Long userId, @Param("goodsId") Long goodsId);

    List<GoodsFavorite> selectByUserId(@Param("userId") Long userId, @Param("offset") Integer offset,
                                        @Param("pageSize") Integer pageSize);

    Long selectCountByUserId(@Param("userId") Long userId);

    List<Long> selectGoodsIdsByUserId(@Param("userId") Long userId, @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    List<Long> selectGoodsIdsByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status, @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    Long selectCountByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    int insert(GoodsFavorite favorite);

    int deleteByUserAndGoods(@Param("userId") Long userId, @Param("goodsId") Long goodsId);
}