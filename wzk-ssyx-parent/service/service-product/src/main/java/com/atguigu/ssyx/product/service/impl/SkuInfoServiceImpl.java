package com.atguigu.ssyx.product.service.impl;


import com.atguigu.ssyx.common.constant.RedisConstant;
import com.atguigu.ssyx.common.exception.WzkException;
import com.atguigu.ssyx.common.result.ResultCodeEnum;
import com.atguigu.ssyx.model.product.SkuAttrValue;
import com.atguigu.ssyx.model.product.SkuImage;
import com.atguigu.ssyx.model.product.SkuInfo;
import com.atguigu.ssyx.model.product.SkuPoster;
import com.atguigu.ssyx.mq.constant.MqConst;
import com.atguigu.ssyx.mq.service.RabbitService;
import com.atguigu.ssyx.product.mapper.SkuInfoMapper;
import com.atguigu.ssyx.product.service.SkuAttrValueService;
import com.atguigu.ssyx.product.service.SkuImageService;
import com.atguigu.ssyx.product.service.SkuInfoService;
import com.atguigu.ssyx.product.service.SkuPosterService;
import com.atguigu.ssyx.vo.product.SkuInfoQueryVo;
import com.atguigu.ssyx.vo.product.SkuInfoVo;
import com.atguigu.ssyx.vo.product.SkuStockLockVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * <p>
 * sku信息 服务实现类
 * </p>
 *
 * @author atguigu
 * @since 2023-06-18
 */
@Service
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoMapper, SkuInfo> implements SkuInfoService {

    @Autowired
    private SkuImageService skuImageService;//sku图片

    @Autowired
    private SkuAttrValueService skuAttrValueService;//sku平台属性

    @Autowired
    private SkuPosterService skuPosterService;//sku海报

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    //sku列表
    @Override
    public IPage<SkuInfo> selectPageSkuInfo(Page<SkuInfo> pageParam, SkuInfoQueryVo skuInfoQueryVo) {
        String keyword = skuInfoQueryVo.getKeyword();
        Long categoryId = skuInfoQueryVo.getCategoryId();
        String skuType = skuInfoQueryVo.getSkuType();

        LambdaQueryWrapper<SkuInfo> wrapper=new LambdaQueryWrapper<>();
        if (!StringUtils.isEmpty(keyword)){
            wrapper.like(SkuInfo::getSkuName,keyword);
        }
        if (!StringUtils.isEmpty(categoryId)){
            wrapper.eq(SkuInfo::getCategoryId,categoryId);
        }
        if (!StringUtils.isEmpty(skuType)){
            wrapper.like(SkuInfo::getSkuType,skuType);
        }
        Page<SkuInfo> pageModel = baseMapper.selectPage(pageParam, wrapper);
        return pageModel;
    }

    //添加商品sku信息
    @Override
    public void saveSkuInfo(SkuInfoVo skuInfoVo) {
        //1 添加sku信息
        SkuInfo skuInfo=new SkuInfo();
        //skuInfo.setSkuName(skuInfoVo.getSkuName());
        BeanUtils.copyProperties(skuInfoVo,skuInfo);
        baseMapper.insert(skuInfo);

        //2 保存sku海报
        List<SkuPoster> skuPosterList=skuInfoVo.getSkuPosterList();
        if (!CollectionUtils.isEmpty(skuPosterList)){
            //遍历，向每个海报对象添加商品skuId
            for (SkuPoster skuPoster:skuPosterList){
                skuPoster.setSkuId(skuInfo.getId());
            }
            skuPosterService.saveBatch(skuPosterList);
        }
        //3 保存sku图片
        List<SkuImage> skuImagesList = skuInfoVo.getSkuImagesList();
        if (!CollectionUtils.isEmpty(skuImagesList)){
            for (SkuImage skuImage:skuImagesList){
                skuImage.setSkuId(skuInfo.getId());
            }
            skuImageService.saveBatch(skuImagesList);
        }
        //4 保存sku平台属性
        List<SkuAttrValue> skuAttrValueList = skuInfoVo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)){
            for (SkuAttrValue skuAttrValue:skuAttrValueList){
                skuAttrValue.setSkuId(skuInfo.getId());
            }
            skuAttrValueService.saveBatch(skuAttrValueList);
        }
    }

    //获取sku的信息
    @Override
    public SkuInfoVo getSkuInfo(Long id) {
        SkuInfoVo skuInfoVo = new SkuInfoVo();

        //根据id查询sku基本信息
        SkuInfo skuInfo = baseMapper.selectById(id);

        //根据id查询商品图片列表
        List<SkuImage> skuImageList= skuImageService.getImageListBySkuId(id);

        //根据id查询商品海报列表
        List<SkuPoster> skuPosterList= skuPosterService.getPosterListBySkuId(id);

        //根据id查询商品属性信息列表
        List<SkuAttrValue> skuAttrValueList= skuAttrValueService.getAttrValueListBySkuId(id);

        //封装
        BeanUtils.copyProperties(skuInfo,skuInfoVo);
        skuInfoVo.setSkuImagesList(skuImageList);
        skuInfoVo.setSkuPosterList(skuPosterList);
        skuInfoVo.setSkuAttrValueList(skuAttrValueList);
        return skuInfoVo;
    }

    //修改sku信息
    @Override
    public void updateSkuInfo(SkuInfoVo skuInfoVo) {
        //1 修改sku基本信息
        SkuInfo skuInfo=new SkuInfo();
        BeanUtils.copyProperties(skuInfoVo,skuInfo);
        baseMapper.updateById(skuInfo);

        Long skuId = skuInfoVo.getId();
        //2 海报信息
        LambdaQueryWrapper<SkuPoster> wrapperSkuPoster=new LambdaQueryWrapper<>();
        wrapperSkuPoster.eq(SkuPoster::getSkuId,skuId);
        skuPosterService.remove(wrapperSkuPoster);

        List<SkuPoster> skuPosterList=skuInfoVo.getSkuPosterList();
        if (!CollectionUtils.isEmpty(skuPosterList)){
            //遍历，向每个海报对象添加商品skuId
            for (SkuPoster skuPoster:skuPosterList){
                skuPoster.setSkuId(skuId);
            }
            skuPosterService.saveBatch(skuPosterList);
        }
        //3 商品图片
        skuImageService.remove(new LambdaQueryWrapper<SkuImage>().eq(SkuImage::getSkuId,skuId));
        List<SkuImage> skuImagesList = skuInfoVo.getSkuImagesList();
        if (!CollectionUtils.isEmpty(skuImagesList)){
            for (SkuImage skuImage:skuImagesList){
                skuImage.setSkuId(skuId);
            }
            skuImageService.saveBatch(skuImagesList);
        }
        //4 商品属性
        skuAttrValueService.remove(new LambdaQueryWrapper<SkuAttrValue>().eq(SkuAttrValue::getSkuId,skuId));
        List<SkuAttrValue> skuAttrValueList = skuInfoVo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)){
            for (SkuAttrValue skuAttrValue:skuAttrValueList){
                skuAttrValue.setSkuId(skuId);
            }
            skuAttrValueService.saveBatch(skuAttrValueList);
        }

    }

    //商品审核
    @Override
    public void check(Long id, Integer status) {
        SkuInfo skuInfo = baseMapper.selectById(id);
        skuInfo.setCheckStatus(status);
        baseMapper.updateById(skuInfo);
    }

    //商品上下架
    @Override
    public void publish(Long id, Integer status) {
        if (status==1){//上架
            SkuInfo skuInfo = baseMapper.selectById(id);
            skuInfo.setPublishStatus(status);
            baseMapper.updateById(skuInfo);
            // 整合MQ数据同步到ES中
            rabbitService.sendMessage(MqConst.EXCHANGE_GOODS_DIRECT,
                    MqConst.ROUTING_GOODS_UPPER,
                    skuInfo);
        }else {//下架
            SkuInfo skuInfo = baseMapper.selectById(id);
            skuInfo.setPublishStatus(status);
            baseMapper.updateById(skuInfo);
            //整合MQ数据同步到ES中
            rabbitService.sendMessage(MqConst.EXCHANGE_GOODS_DIRECT,
                    MqConst.ROUTING_GOODS_LOWER,
                    skuInfo);
        }
    }

    //新人专享
    @Override
    public void isNewPerson(Long id, Integer status) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(id);
        skuInfo.setIsNewPerson(status);
        baseMapper.updateById(skuInfo);
    }

    //根据skuId列表得到sku信息列表
    @Override
    public List<SkuInfo> findSkuInfoList(List<Long> skuIdList) {
        List<SkuInfo> skuInfoList = baseMapper.selectBatchIds(skuIdList);
        return skuInfoList;
    }

    //根据关键字查询匹配sku信息
    @Override
    public List<SkuInfo> findSkuInfoByKeyword(String keyword) {
        List<SkuInfo> skuInfoList = baseMapper.selectList(
                new LambdaQueryWrapper<SkuInfo>().like(SkuInfo::getSkuName, keyword)
        );
        return skuInfoList;
    }

    //获取新人专享商品
    @Override
    public List<SkuInfo> findNewPersonSkuInfoList() {
        //条件1：is_new_person=1
        //条件2：publish_status=1
        //条件3：显示其中三个
        //获取第一页数据，每页显示三条记录
        Page<SkuInfo> pageParam=new Page<>(1,3);
        LambdaQueryWrapper<SkuInfo> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(SkuInfo::getIsNewPerson,1);
        wrapper.eq(SkuInfo::getPublishStatus,1);
        wrapper.orderByDesc(SkuInfo::getStock);//库存排序
        //调用方法查找
        IPage<SkuInfo> skuInfoIPage=baseMapper.selectPage(pageParam,wrapper);
        List<SkuInfo> skuInfoList = skuInfoIPage.getRecords();
        return skuInfoList;
    }

    //根据sukId获取sku信息
    @Override
    public SkuInfoVo getSkuInfoVo(Long skuId) {
        SkuInfoVo skuInfoVo=new SkuInfoVo();
        //skuId查询skuInfo
        SkuInfo skuInfo = baseMapper.selectById(skuId);
        //skuId查询sku图片
        List<SkuImage> imageListBySkuId = skuImageService.getImageListBySkuId(skuId);
        //skuId查询sku海报
        List<SkuPoster> posterListBySkuId = skuPosterService.getPosterListBySkuId(skuId);
        //skuId查询sku属性
        List<SkuAttrValue> attrValueListBySkuId = skuAttrValueService.getAttrValueListBySkuId(skuId);

        //封装到skuInfoVo
        BeanUtils.copyProperties(skuInfo,skuInfoVo);
        skuInfoVo.setSkuImagesList(imageListBySkuId);
        skuInfoVo.setSkuPosterList(posterListBySkuId);
        skuInfoVo.setSkuAttrValueList(attrValueListBySkuId);
        return skuInfoVo;
    }

    //验证和锁定库存
    @Override
    public Boolean checkAndLock(List<SkuStockLockVo> skuStockLockVoList, String orderNo) {
        //1 判断集合skuStockLockVoList是否为空
        if (CollectionUtils.isEmpty(skuStockLockVoList)){
            throw new WzkException(ResultCodeEnum.DATA_ERROR);
        }
        //2 遍历skuStockLockVoList集合得到每一个商品，验证库存并锁定库存，具备原子性
        skuStockLockVoList.stream().forEach(skuStockLockVo -> {
            this.checkLock(skuStockLockVo);
        });
        //3 只要有一个商品锁定失败，所以锁定成功的商品都解锁
        boolean flag = skuStockLockVoList.stream()
                .anyMatch(skuStockLockVo -> !skuStockLockVo.getIsLock());
        if (flag){
            //所以锁定成功的商品都解锁
            skuStockLockVoList.stream()
                    .filter(SkuStockLockVo::getIsLock)
                    .forEach(skuStockLockVo -> {
                        baseMapper.unlockStock(skuStockLockVo.getSkuId(),skuStockLockVo.getSkuNum());
                    });
            //返回失败的状态
            return false;
        }
        //4 如果所以商品都锁定成功了，redis缓存相关数据，为了方便后面解锁和所库存
        redisTemplate.opsForValue()
                .set(RedisConstant.SROCK_INFO+orderNo,skuStockLockVoList);
        return true;
    }

    //扣减库存成功，更新订单状态
    @Override
    public void minusStock(String orderNo) {
        //从redis获取锁定库存信息
        List<SkuStockLockVo> skuStockLockVoList =
                (List<SkuStockLockVo>)redisTemplate.opsForValue().get(RedisConstant.SROCK_INFO + orderNo);
        if (CollectionUtils.isEmpty(skuStockLockVoList)){
            return;
        }
        //遍历集合，得到每个对象，减库存
        skuStockLockVoList.forEach(skuStockLockVo -> {
            baseMapper.minusStock(skuStockLockVo.getSkuId(),skuStockLockVo.getSkuNum());
        });
        //删除redis数据
        redisTemplate.delete(RedisConstant.SROCK_INFO+orderNo);
    }

    //验证库存并锁定库存
    private void checkLock(SkuStockLockVo skuStockLockVo) {
        //获取锁
        //公平锁
        RLock rLock = this.redissonClient.getFairLock(RedisConstant.SKUKEY_PREFIX + skuStockLockVo.getSkuId());
        //加锁
        rLock.lock();

        try {
            //验证库存
            SkuInfo skuInfo=baseMapper.checkStock(skuStockLockVo.getSkuId(),skuStockLockVo.getSkuNum());
            //判断没有满足条件商品，设置isLock值false，返回
            if (skuInfo==null){
                skuStockLockVo.setIsLock(false);
                return;
            }
            //有满足条件商品
            //锁定库存
            Integer rows=baseMapper.lockStock(skuStockLockVo.getSkuId(),skuStockLockVo.getSkuNum());
            if (rows==1){
                skuStockLockVo.setIsLock(true);
            }
        }finally {
            //解锁
            rLock.unlock();
        }
    }


}
