package com.atguigu.ssyx.common.exception;

import com.atguigu.ssyx.common.result.ResultCodeEnum;
import lombok.Data;

/**
 * @author WenZK
 * @create 2023-06-11
 *
 */
@Data
public class WzkException extends RuntimeException{

    private Integer code;

    public WzkException(String message,Integer code){
        super(message);
        this.code=code;
    }

    public WzkException(ResultCodeEnum resultCodeEnum){
        super(resultCodeEnum.getMessage());
        this.code=resultCodeEnum.getCode();
    }
}
