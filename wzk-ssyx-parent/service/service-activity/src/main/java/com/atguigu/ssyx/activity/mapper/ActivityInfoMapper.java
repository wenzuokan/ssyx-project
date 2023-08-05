package com.atguigu.ssyx.activity.mapper;


import com.atguigu.ssyx.model.activity.ActivityInfo;
import com.atguigu.ssyx.model.activity.ActivityRule;
import com.atguigu.ssyx.model.activity.ActivitySku;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <p>
 * 活动表 Mapper 接口
 * </p>
 *
 * @author WenZK
 * @since 2023-06-27
 */

public interface ActivityInfoMapper extends BaseMapper<ActivityInfo> {

    //如果之前参加过，活动正在进行中，排除商品
    List<Long> selectSkuIdListExist(@Param("skuIdList")List<Long> skuIdList);

    //根据skuId进行查询，查询sku对应活动里面列表
    List<ActivityRule> findActivityRule(Long skuId);

    //根据所有skuId获取参与活动
    List<ActivitySku> selectCartActivity(@Param("skuIdList") List<Long> skuIdList);
}
