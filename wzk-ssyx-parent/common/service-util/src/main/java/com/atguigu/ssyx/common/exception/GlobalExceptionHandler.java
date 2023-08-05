package com.atguigu.ssyx.common.exception;

import com.atguigu.ssyx.common.result.Result;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author WenZK
 * @create 2023-06-11
 *
 */
//AOP
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)//异常处理器
    @ResponseBody//返回json数据
    public Result error(Exception e){
        e.printStackTrace();
        return Result.fail(null);
    }

    //自定义异常处理
    @ExceptionHandler(WzkException.class)
    @ResponseBody
    public Result error(WzkException exception){
        return Result.build(null,exception.getCode(),exception.getMessage());
    }
}
