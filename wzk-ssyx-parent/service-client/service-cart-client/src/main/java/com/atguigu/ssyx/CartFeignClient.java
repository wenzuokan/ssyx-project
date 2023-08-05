package com.atguigu.ssyx;

import com.atguigu.ssyx.model.order.CartInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * @author WenZK
 * @create 2023-07-23
 *
 */
@FeignClient("service-cart")
public interface CartFeignClient {

    @GetMapping("/api/cart/inner/getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable("userId")Long userId);
}
