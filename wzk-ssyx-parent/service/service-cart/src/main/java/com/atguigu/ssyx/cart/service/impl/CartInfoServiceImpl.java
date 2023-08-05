package com.atguigu.ssyx.cart.service.impl;

import com.atguigu.ssyx.cart.service.CartInfoService;
import com.atguigu.ssyx.client.product.ProductFeignClient;
import com.atguigu.ssyx.common.constant.RedisConstant;
import com.atguigu.ssyx.common.exception.WzkException;
import com.atguigu.ssyx.common.result.ResultCodeEnum;
import com.atguigu.ssyx.enums.SkuType;
import com.atguigu.ssyx.model.order.CartInfo;
import com.atguigu.ssyx.model.product.SkuInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author WenZK
 * @create 2023-07-18
 *
 */
@Service
public class CartInfoServiceImpl implements CartInfoService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    private String getCartKey(Long userId){
        return RedisConstant.USER_KEY_PREFIX+userId+RedisConstant.USER_CART_KEY_SUFFIX;
    }
    //设置key过期时间
    private void setCartKeyExpire(String key){
        redisTemplate.expire(key,RedisConstant.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }
    //添加商品到购物车
    @Override
    public void addToCart(Long userId, Long skuId, Integer skuNum) {
        //1 因为购物车数据存储到redis里面，
        // 从redis里面根据key获取数据，这个key包含userId
        String cartKey=this.getCartKey(userId);
        BoundHashOperations<String,String, CartInfo> hashOperations = redisTemplate.boundHashOps(cartKey);
        //2 根据第一步查询结果，得到是skuId+skuNum关系
        CartInfo cartInfo=null;
        //目的：判断是否是第一次添加这个商品到购物车
        // 进行判断，判断结果，是否有skuId
         if (hashOperations.hasKey(skuId.toString())){
             //3 如果结果里面包含skuId,不是第一次添加
             //3.1 根据skuId，获取对应数量，更新数量
             cartInfo = hashOperations.get(skuId.toString());
             //把购物车存在商品之前数量获取数量，在进行数量更新操作
             Integer currentSkuNum = cartInfo.getSkuNum() + skuNum;
             if (currentSkuNum<1){
                 return;
             }
             //更新cartInfo对象
             cartInfo.setSkuNum(currentSkuNum);
             cartInfo.setCurrentBuyNum(currentSkuNum);
             //判断商品数量不能大于限购数量
             Integer perLimit = cartInfo.getPerLimit();
             if (currentSkuNum>perLimit){
                 throw new WzkException(ResultCodeEnum.SKU_LIMIT_ERROR);
             }
             //更新其他值
             cartInfo.setIsChecked(1);
             cartInfo.setUpdateTime(new Date());
         }else {
             //4 如果结果里面没有skuId，就是第一次添加
             //4.1 直接进行添加
             skuNum=1;
             //远程调用根据skuId获取skuInfo
             SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
             if (skuInfo==null){
                 throw new WzkException(ResultCodeEnum.DATA_ERROR);
             }
             //封装cartInfo对象
             cartInfo=new CartInfo();
             cartInfo.setSkuId(userId);
             cartInfo.setCategoryId(skuInfo.getCategoryId());
             cartInfo.setSkuType(skuInfo.getSkuType());
             cartInfo.setIsNewPerson(skuInfo.getIsNewPerson());
             cartInfo.setUserId(userId);
             cartInfo.setCartPrice(skuInfo.getPrice());
             cartInfo.setSkuNum(skuNum);
             cartInfo.setCurrentBuyNum(skuNum);
             cartInfo.setSkuType(SkuType.COMMON.getCode());
             cartInfo.setPerLimit(skuInfo.getPerLimit());
             cartInfo.setImgUrl(skuInfo.getImgUrl());
             cartInfo.setSkuName(skuInfo.getSkuName());
             cartInfo.setWareId(skuInfo.getWareId());
             cartInfo.setIsChecked(1);
             cartInfo.setStatus(1);
             cartInfo.setCreateTime(new Date());
             cartInfo.setUpdateTime(new Date());
         }

        //5 更新redis缓存
        hashOperations.put(skuId.toString(),cartInfo);

        //6 设置有效时间
        this.setCartKeyExpire(cartKey);

    }

    //根据skuId删除购物车
    @Override
    public void deleteCart(Long skuId, Long userId) {
        BoundHashOperations<String,String,CartInfo> hashOperations = redisTemplate.boundHashOps(this.getCartKey(userId));
        if (hashOperations.hasKey(skuId.toString())){
            hashOperations.delete(skuId.toString());
        }
    }

    //清空购物车
    @Override
    public void deleteAllCart(Long userId) {
        String cartKey=this.getCartKey(userId);
        BoundHashOperations<String,String,CartInfo> hashOperations = redisTemplate.boundHashOps(cartKey);
        List<CartInfo> cartInfoList = hashOperations.values();
        for (CartInfo cartInfo:cartInfoList){
            hashOperations.delete(cartInfo.getSkuId().toString());
        }
    }

    //批量删除购物车 skuId
    @Override
    public void batchDeleteCart(List<Long> skuIdList, Long userId) {
        String cartKey=this.getCartKey(userId);
        BoundHashOperations<String,String,CartInfo> hashOperations = redisTemplate.boundHashOps(cartKey);
        skuIdList.forEach(skuId->{
            hashOperations.delete(skuId).toString();
        });
    }

    //购物车列表
    @Override
    public List<CartInfo> getCartList(Long userId) {
        //判断用户userId
        List<CartInfo> cartInfoList=new ArrayList<>();
        if (StringUtils.isEmpty(userId)){
            return cartInfoList;
        }
        //从redis获取购物车数据
        String cartKey = this.getCartKey(userId);
        BoundHashOperations<String,String,CartInfo> boundHashOperations = redisTemplate.boundHashOps(cartKey);
        cartInfoList = boundHashOperations.values();
        if (!CollectionUtils.isEmpty(cartInfoList)){
            //根据商品添加时间，降序
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo cartInfo, CartInfo t1) {
                    return cartInfo.getCreateTime().compareTo(t1.getCreateTime());
                }
            });
        }
        return cartInfoList;
    }

    //1 根据skuId选中
    @Override
    public void checkCart(Long userId, Long skuId, Integer isChecked) {
        //获取redis的key
        String cartKey = this.getCartKey(userId);
        BoundHashOperations<String,String,CartInfo> boundHashOperations = redisTemplate.boundHashOps(cartKey);
        CartInfo cartInfo = boundHashOperations.get(skuId.toString());
        if (cartInfo!=null){
            cartInfo.setIsChecked(isChecked);
            //更新
            boundHashOperations.put(skuId.toString(),cartInfo);
            //设置key过期时间
            this.setCartKeyExpire(cartKey);
        }
    }

    //2 全选
    @Override
    public void checkAllCart(Long userId, Integer isChecked) {
        String cartKey = this.getCartKey(userId);
        BoundHashOperations<String,String,CartInfo> boundHashOperations = redisTemplate.boundHashOps(cartKey);
        List<CartInfo> cartInfoList = boundHashOperations.values();
        cartInfoList.stream().forEach(cartInfo -> {
            cartInfo.setIsChecked(isChecked);
            boundHashOperations.put(cartInfo.toString(),cartInfo);
        });
        this.setCartKeyExpire(cartKey);
    }

    //3 批量选中
    @Override
    public void batchCheckCart(List<Long> skuIdList, Long userId, Integer isChecked) {
        String cartKey = this.getCartKey(userId);
        BoundHashOperations<String,String,CartInfo> boundHashOperations = redisTemplate.boundHashOps(cartKey);
        skuIdList.forEach(skuId -> {
            CartInfo cartInfo = boundHashOperations.get(skuId.toString());
            cartInfo.setIsChecked(isChecked);
            boundHashOperations.put(cartInfo.getSkuId().toString(),cartInfo);
        });
        this.setCartKeyExpire(cartKey);
    }

    //获取购物车选中的商品
    @Override
    public List<CartInfo> getCartCheckedList(Long userId) {
        String cartKey = this.getCartKey(userId);
        BoundHashOperations<String,String,CartInfo> boundHashOperations = redisTemplate.boundHashOps(cartKey);
        List<CartInfo> cartInfoList = boundHashOperations.values();
        //isChecked = 1 购物项选中
        List<CartInfo> cartInfoListNew = cartInfoList.stream()
                .filter(cartInfo -> {
                    return cartInfo.getIsChecked().intValue() == 1;
                }).collect(Collectors.toList());

        return cartInfoListNew;
    }

    //根据用户id删除选中的购物车记录
    @Override
    public void deleteCartChecked(Long userId) {
        //根据用户id查询选中购物车记录
        List<CartInfo> cartInfoList = this.getCartCheckedList(userId);
        //查询list集合进行遍历，得到每个skuId集合
        List<Long> skuIdList = cartInfoList.stream().map(
                item -> item.getSkuId()
        ).collect(Collectors.toList());
        //构建redis的key值
        //hash类型 key filed-value
        String cartKey = this.getCartKey(userId);

        //根据key查询filed-value
        BoundHashOperations<String,String,CartInfo> hashOperations = redisTemplate.boundHashOps(cartKey);
        //根据filed(skuId)删除redis数据
        skuIdList.forEach(skuId->{
            hashOperations.delete(skuId.toString());
        });
    }

}
