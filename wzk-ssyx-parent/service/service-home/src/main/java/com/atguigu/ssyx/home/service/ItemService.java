package com.atguigu.ssyx.home.service;

import java.util.Map;

/**
 * @author WenZK
 * @create 2023-07-16
 *
 */
public interface ItemService {

    //获取sku详细信息
    Map<String, Object> item(Long id, Long userId);
}
