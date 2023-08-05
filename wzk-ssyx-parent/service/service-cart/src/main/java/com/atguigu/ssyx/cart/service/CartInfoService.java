package com.atguigu.ssyx.cart.service;

import com.atguigu.ssyx.model.order.CartInfo;

import java.util.List;

/**
 * @author WenZK
 * @create 2023-07-18
 *
 */
public interface CartInfoService {

    //添加商品到购物车
    void addToCart(Long userId, Long skuId, Integer skuNum);

    //根据skuId删除购物车
    void deleteCart(Long skuId, Long userId);

    //清空购物车
    void deleteAllCart(Long userId);

    //批量删除购物车 skuId
    void batchDeleteCart(List<Long> skuIdList, Long userId);

    //购物车列表
    List<CartInfo> getCartList(Long userId);

    //1 根据skuId选中
    void checkCart(Long userId, Long skuId, Integer isChecked);

    //2 全选
    void checkAllCart(Long userId, Integer isChecked);

    //3 批量选中
    void batchCheckCart(List<Long> skuIdList, Long userId, Integer isChecked);

    //获取购物车选中的商品
    List<CartInfo> getCartCheckedList(Long userId);

    //根据用户id删除选中的购物车记录
    void deleteCartChecked(Long userId);
}
