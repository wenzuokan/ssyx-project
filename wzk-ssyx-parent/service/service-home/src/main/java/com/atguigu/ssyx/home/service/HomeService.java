package com.atguigu.ssyx.home.service;

import java.util.Map;

/**
 * @author WenZK
 * @create 2023-07-09
 *
 */
public interface HomeService {

    //首页数据显示
    Map<String, Object> homeData(Long userId);
}
