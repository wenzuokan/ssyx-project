package com.atguigu.ssyx.activity.service.impl;


import com.atguigu.ssyx.activity.mapper.CouponInfoMapper;
import com.atguigu.ssyx.activity.mapper.CouponRangeMapper;
import com.atguigu.ssyx.activity.mapper.CouponUseMapper;
import com.atguigu.ssyx.activity.service.CouponInfoService;
import com.atguigu.ssyx.client.product.ProductFeignClient;
import com.atguigu.ssyx.enums.CouponRangeType;
import com.atguigu.ssyx.enums.CouponStatus;
import com.atguigu.ssyx.model.activity.CouponInfo;
import com.atguigu.ssyx.model.activity.CouponRange;
import com.atguigu.ssyx.model.activity.CouponUse;
import com.atguigu.ssyx.model.order.CartInfo;
import com.atguigu.ssyx.model.product.Category;
import com.atguigu.ssyx.model.product.SkuInfo;
import com.atguigu.ssyx.vo.activity.CouponRuleVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 优惠券信息 服务实现类
 * </p>
 *
 * @author WenZK
 * @since 2023-06-27
 */
@Service
public class CouponInfoServiceImpl extends ServiceImpl<CouponInfoMapper, CouponInfo> implements CouponInfoService {

    @Autowired
    private CouponRangeMapper couponRangeMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private CouponUseMapper couponUseMapper;

    //1 优惠卷分页查询
    @Override
    public IPage<CouponInfo> selectPageCouponInfo(Long page, Long limit) {
        Page<CouponInfo> pageParam=new Page<>(page,limit);
        IPage<CouponInfo> couponInfoPage = baseMapper.selectPage(pageParam, null);
        List<CouponInfo> couponInfoList = couponInfoPage.getRecords();
        couponInfoList.stream().forEach(item ->{
            item.setCouponTypeString(item.getCouponType().getComment());
            CouponRangeType rangeType=item.getRangeType();
            if (rangeType!=null){
                item.setRangeTypeString(rangeType.getComment());
            }
        });
        return couponInfoPage;
    }

    //3 根据id查询优惠卷
    @Override
    public CouponInfo getCouponInfo(Long id) {
        CouponInfo couponInfo = baseMapper.selectById(id);
        couponInfo.setCouponTypeString(couponInfo.getCouponType().getComment());
        if (couponInfo.getRangeType()!=null){
            couponInfo.setRangeTypeString(couponInfo.getRangeType().getComment());
        }
        return couponInfo;
    }

    //4 根据优惠卷id查询规则数据
    @Override
    public Map<String, Object> findCouponRuleList(Long id) {
        //1 根据优惠卷id查询优惠卷信息
        CouponInfo couponInfo = baseMapper.selectById(id);

        //2 根据优惠卷id查询coupon_range 查询里面对应range_id
        List<CouponRange> couponRangeList = couponRangeMapper.selectList(
                new LambdaQueryWrapper<CouponRange>().eq(CouponRange::getCouponId, id)
        );
        // 如果规则类型 SKU range_id就是skuId
        // 如果规则类型 CATEGORY range_id就是分类id值
        List<Long> rangeIdList = couponRangeList.stream().map(CouponRange::getCouponId).collect(Collectors.toList());

        Map<String,Object> result=new HashMap<>();
        //3 分别判断封装不同数据
        if (!CollectionUtils.isEmpty(rangeIdList)){
            if (couponInfo.getRangeType()==CouponRangeType.SKU){
                // 如果规则类型是SKU，得到skuId,远程调用多个skuId值获取对应sku信息
                List<SkuInfo> skuInfoList = productFeignClient.findSkuInfoList(rangeIdList);
                result.put("skuInfoList",skuInfoList);
            }else if (couponInfo.getRangeType()==CouponRangeType.CATEGORY){
                // 如果规则类型是分类，得到分类Id，远程调用根据多个分类id获取对应分类信息
                List<Category> categoryList= productFeignClient.findCategoryList(rangeIdList);
                result.put("categoryList",categoryList);
            }
        }
        return result;
    }

    //5 添加优惠卷规则数据
    @Override
    public void saveCouponRule(CouponRuleVo couponRuleVo) {
        //根据优惠卷id删除规则数据
        couponRangeMapper.delete(
                new LambdaQueryWrapper<CouponRange>().eq(CouponRange::getCouponId,couponRuleVo.getCouponId())
        );

        //更新优惠卷基本信息
        CouponInfo couponInfo = baseMapper.selectById(couponRuleVo.getCouponId());
        couponInfo.setRangeType(couponRuleVo.getRangeType());
        couponInfo.setConditionAmount(couponRuleVo.getConditionAmount());
        couponInfo.setAmount(couponRuleVo.getAmount());
        couponInfo.setConditionAmount(couponRuleVo.getConditionAmount());
        couponInfo.setRangeDesc(couponRuleVo.getRangeDesc());
        baseMapper.updateById(couponInfo);

        //添加优惠卷规则信息
        List<CouponRange> couponRangeList = couponRuleVo.getCouponRangeList();
        for (CouponRange couponRange:couponRangeList){
            //设置优惠卷id
            couponRange.setCouponId(couponRuleVo.getCouponId());
            //添加
            couponRangeMapper.insert(couponRange);
        }

    }

    //2 根据skuId+userId查询优惠卷信息
    @Override
    public List<CouponInfo> findCouponInfoList(Long skuId, Long userId) {
        //远程调用：根据skuId获取skuInfo
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        //根据条件查询：skuId+分类id+userId
        List<CouponInfo> couponInfoList= baseMapper.selectCouponInfoList(skuInfo.getId(),
                skuInfo.getCategoryId(),userId);
        return null;
    }

    private Map<Long,List<Long>> findCouponIdToSkuIdMap(List<CartInfo> cartInfoList,
                                                        List<CouponRange> couponRangesList){
        Map<Long,List<Long>> couponIdToSkuIdMap=new HashMap<>();
        Map<Long, List<CouponRange>> couponRangeToRangeListMap = couponRangesList.stream()
                .collect(Collectors.groupingBy(couponRange -> couponRange.getCouponId()));
        Iterator<Map.Entry<Long, List<CouponRange>>> iterator = couponRangeToRangeListMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, List<CouponRange>> entry = iterator.next();
            Long couponId = entry.getKey();
            List<CouponRange> rangeList = entry.getValue();

            Set<Long> skuIdSet = new HashSet<>();
            for (CartInfo cartInfo : cartInfoList) {
                for(CouponRange couponRange : rangeList) {
                    if(CouponRangeType.SKU == couponRange.getRangeType() &&
                            couponRange.getRangeId().longValue() == cartInfo.getSkuId().intValue()) {
                        skuIdSet.add(cartInfo.getSkuId());
                    } else if(CouponRangeType.CATEGORY == couponRange.getRangeType() &&
                            couponRange.getRangeId().longValue() == cartInfo.getCategoryId().intValue()) {
                        skuIdSet.add(cartInfo.getSkuId());
                    } else {

                    }
                }
            }
            couponIdToSkuIdMap.put(couponId, new ArrayList<>(skuIdSet));
        }
        return couponIdToSkuIdMap;
    }
    //3 获取购物车可用优惠卷列表
    @Override
    public List<CouponInfo> findCartCouponInfo(List<CartInfo> cartInfoList, Long userId) {
        //1 根据用户id获取用户全部优惠卷
        //coupon_use coupon_info
        List<CouponInfo> userAllCouponInfoList=baseMapper.selectCartCouponInfoList(userId);
        if(CollectionUtils.isEmpty(userAllCouponInfoList)){
            return new ArrayList<CouponInfo>();
        }

        //2 从第一步返回list集合中，获取所以优惠卷id列表
        List<Long> couponIdList = userAllCouponInfoList.stream().map(couponInfo -> couponInfo.getId())
                .collect(Collectors.toList());

        //3 查询优惠卷使用范围
        //couponRangeList
        LambdaQueryWrapper<CouponRange> wrapper=new LambdaQueryWrapper<>();
        wrapper.in(CouponRange::getCouponId,couponIdList);
        List<CouponRange> couponRangeList = couponRangeMapper.selectList(wrapper);

        //4 获取优惠卷id 对应skuId列表
        //优惠卷id进行分组
        Map<Long,List<Long>> couponIdToSkuIdMap=this.findCouponIdToSkuIdMap(cartInfoList,couponRangeList);

        //5 遍历全部优惠卷集合，判断优惠卷类型
        //全场通用
        BigDecimal reduceAmount=new BigDecimal(0);
        CouponInfo optimalCouponInfo=null;
        for (CouponInfo couponInfo:userAllCouponInfoList){
            //全场通用
            if (CouponRangeType.ALL==couponInfo.getRangeType()){
                //全场通用
                //判断是否满足优惠使用门槛
                //计算购物车商品总价
                BigDecimal totalAmount=computeTotalAmount(cartInfoList);
                if (totalAmount.subtract(couponInfo.getConditionAmount()).doubleValue()>=0){
                    couponInfo.setIsSelect(1);
                }
            }else {
                //优惠卷id获取对应skuId列表
                List<Long> skuIdList = couponIdToSkuIdMap.get(couponInfo.getId());
                //满足使用范围购物项
                List<CartInfo> currentCartInfoList = cartInfoList.stream()
                        .filter(cartInfo -> skuIdList.contains(cartInfo.getSkuId()))
                        .collect(Collectors.toList());
                BigDecimal totalAmount = computeTotalAmount(currentCartInfoList);
                if(totalAmount.subtract(couponInfo.getConditionAmount()).doubleValue() >= 0){
                    couponInfo.setIsSelect(1);
                }
            }
            if (couponInfo.getIsSelect().intValue()==1 &&
                    couponInfo.getAmount().subtract(reduceAmount).doubleValue()>0){
                reduceAmount=couponInfo.getAmount();
                optimalCouponInfo=couponInfo;
            }
        }

        //6 返回List<CouponInfo>
        if (optimalCouponInfo!=null){
            optimalCouponInfo.setIsOptimal(1);
        }
        return userAllCouponInfoList;
    }

    //获取购物车对应优惠卷
    @Override
    public CouponInfo findRangeSkuIdList(List<CartInfo> cartInfoList, Long couponId) {
        //根据优惠卷id查询基本信息
        CouponInfo couponInfo = baseMapper.selectById(couponId);
        if (couponInfo==null){
            return null;
        }
        //根据couponId查询对应CouponRange数据
        List<CouponRange> couponRangeList = couponRangeMapper.selectList(
                new LambdaQueryWrapper<CouponRange>()
                        .eq(CouponRange::getCouponId, couponId)
        );
        //对应sku信息
        Map<Long, List<Long>> couponIdToSkuIdMap = this.findCouponIdToSkuIdMap(cartInfoList, couponRangeList);
        //遍历map,得到value,封装到couponInfo对象
        List<Long> skuIdList = couponIdToSkuIdMap.entrySet().iterator().next().getValue();
        couponInfo.setSkuIdList(skuIdList);
        return couponInfo;
    }

    //更新优惠卷的使用状态
    @Override
    public void updateCouponInfoUserStatus(Long couponId, Long userId, Long orderId) {
        //根据CouponId查询优惠卷信息
        CouponUse couponUse = couponUseMapper.selectOne(
                new LambdaQueryWrapper<CouponUse>()
                        .eq(CouponUse::getCouponId, couponId)
                        .eq(CouponUse::getUserId, userId)
                        .eq(CouponUse::getOrderId, orderId)
        );
        //设置修改值
        couponUse.setCouponStatus(CouponStatus.USED);

        //调用方法修改
        couponUseMapper.updateById(couponUse);

    }

    private BigDecimal computeTotalAmount(List<CartInfo> cartInfoList) {
        BigDecimal total = new BigDecimal("0");
        for (CartInfo cartInfo : cartInfoList) {
            //是否选中
            if(cartInfo.getIsChecked().intValue() == 1) {
                BigDecimal itemTotal = cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
                total = total.add(itemTotal);
            }
        }
        return total;
    }

}
