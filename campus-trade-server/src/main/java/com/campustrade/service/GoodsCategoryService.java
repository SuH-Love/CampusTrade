package com.campustrade.service;

import com.campustrade.entity.GoodsCategory;
import com.campustrade.vo.GoodsCategoryVO;

import java.util.List;

public interface GoodsCategoryService {

    List<GoodsCategory> listAll();

    List<GoodsCategoryVO> listAllVO();
}
