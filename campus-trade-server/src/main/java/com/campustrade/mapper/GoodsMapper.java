package com.campustrade.mapper;

import com.campustrade.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Mapper
public interface GoodsMapper {

    Goods selectById(@Param("id") Long id);

    List<Goods> selectByIds(@Param("ids") Collection<Long> ids);

    List<Goods> selectList(@Param("categoryId") Long categoryId, @Param("keyword") String keyword,
                           @Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice,
                           @Param("status") String status, @Param("userId") Long userId,
                           @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    Long selectCount(@Param("categoryId") Long categoryId, @Param("keyword") String keyword,
                     @Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice,
                     @Param("status") String status, @Param("userId") Long userId);

    List<Goods> selectHotGoods(@Param("limit") Integer limit);

    List<Goods> selectRecommendGoods(@Param("limit") Integer limit);

    int insert(Goods goods);

    int updateById(Goods goods);

    int logicDeleteById(@Param("id") Long id);

    int incrementViewCount(@Param("id") Long id);

    int incrementFavoriteCount(@Param("id") Long id);

    int decrementFavoriteCount(@Param("id") Long id);

    int decrementStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    Long selectCountAll();

    Long selectCountByStatus(@Param("status") String status);
}