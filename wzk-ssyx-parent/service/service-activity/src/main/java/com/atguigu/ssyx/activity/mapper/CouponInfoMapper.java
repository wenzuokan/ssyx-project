package com.atguigu.ssyx.activity.mapper;


import com.atguigu.ssyx.model.activity.CouponInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 优惠券信息 Mapper 接口
 * </p>
 *
 * @author WenZK
 * @since 2023-06-27
 */
public interface CouponInfoMapper extends BaseMapper<CouponInfo> {

    //根据条件查询：skuId+分类id+userId
    List<CouponInfo> selectCouponInfoList(@Param("skuId") Long id,
                                          @Param("categoryId") Long categoryId,
                                          @Param("userId") Long userId);

    //1 根据用户id获取用户全部优惠卷
    List<CouponInfo> selectCartCouponInfoList(Long userId);
}
