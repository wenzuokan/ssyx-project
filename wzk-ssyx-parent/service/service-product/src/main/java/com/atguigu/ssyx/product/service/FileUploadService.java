package com.atguigu.ssyx.product.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * @author WenZK
 * @create 2023-06-19
 *
 */
public interface FileUploadService {

    //文件上传
    Object fileUpload(MultipartFile file);
}
