package com.campustrade.controller;

import com.campustrade.common.Result;
import com.campustrade.entity.Cart;
import com.campustrade.entity.Goods;
import com.campustrade.mapper.CartMapper;
import com.campustrade.mapper.GoodsMapper;
import com.campustrade.util.SecurityUtil;
import com.campustrade.vo.CartVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Api(tags = "购物车接口")
@RestController
@RequestMapping("/api/cart")
public class CartController {
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private GoodsMapper goodsMapper;

    @ApiOperation("购物车列表")
    @GetMapping
    public Result<List<CartVO>> listCart() {
        Long userId = SecurityUtil.requireCurrentUserId();
        List<Cart> carts = cartMapper.selectByUserId(userId);
        List<CartVO> vos = carts.stream().map(c -> {
            CartVO vo = new CartVO();
            vo.setId(c.getId());
            vo.setGoodsId(c.getGoodsId());
            vo.setQuantity(c.getQuantity());
            Goods goods = goodsMapper.selectById(c.getGoodsId());
            if (goods != null) {
                vo.setTitle(goods.getTitle());
                vo.setCoverImage(goods.getCoverImage());
                vo.setPrice(goods.getPrice());
                vo.setStatus(goods.getStatus());
                vo.setSellerId(goods.getUserId());
                vo.setStock(goods.getStock() != null ? goods.getStock() : 1);
            }
            return vo;
        }).collect(Collectors.toList());
        return Result.success(vos);
    }

    @ApiOperation("加入购物车")
    @PostMapping("/{goodsId}")
    public Result<Void> addToCart(@PathVariable Long goodsId) {
        Long userId = SecurityUtil.requireCurrentUserId();
        Cart existing = cartMapper.selectByUserAndGoods(userId, goodsId);
        if (existing != null) {
            cartMapper.updateQuantity(existing.getId(), existing.getQuantity() + 1);
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setGoodsId(goodsId);
            cart.setQuantity(1);
            try {
                cartMapper.insert(cart);
            } catch (Exception e) {
                cartMapper.restoreByUserAndGoods(userId, goodsId);
            }
        }
        return Result.success();
    }

    @ApiOperation("修改数量")
    @PutMapping("/{id}")
    public Result<Void> updateQuantity(@PathVariable Long id, @RequestParam Integer quantity) {
        if (quantity <= 0) cartMapper.deleteById(id);
        else cartMapper.updateQuantity(id, quantity);
        return Result.success();
    }

    @ApiOperation("移除购物车")
    @DeleteMapping("/{id}")
    public Result<Void> removeFromCart(@PathVariable Long id) {
        cartMapper.deleteById(id);
        return Result.success();
    }

    @ApiOperation("清空购物车")
    @DeleteMapping
    public Result<Void> clearCart() {
        cartMapper.deleteByUserId(SecurityUtil.requireCurrentUserId());
        return Result.success();
    }
}