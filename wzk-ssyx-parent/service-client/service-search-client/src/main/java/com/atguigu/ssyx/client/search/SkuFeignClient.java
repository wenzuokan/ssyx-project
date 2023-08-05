package com.atguigu.ssyx.client.search;

import com.atguigu.ssyx.model.search.SkuEs;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * @author WenZK
 * @create 2023-07-10
 *
 */
@FeignClient("service-search")
public interface SkuFeignClient {

    @GetMapping("/api/search/inner/findHotSkuList")
    public List<SkuEs> findHotSkuList();

    //更新商品热度
    @GetMapping("/api/search/inner/incrHotScore/{skuId}")
    public boolean incrHotScore(@PathVariable("skuId")Long skuId);

}
