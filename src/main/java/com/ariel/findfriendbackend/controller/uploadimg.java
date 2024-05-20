package com.ariel.findfriendbackend.controller;


import com.ariel.findfriendbackend.utils.FileUtil;
import com.ariel.findfriendbackend.utils.QiniuOssUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;


@Controller
@Slf4j
@RequestMapping("upload")
@CrossOrigin(origins = {"http://localhost:3000/"})
public class uploadimg {
    @PostMapping("/img")
    @ResponseBody
    public void up1img(@RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        //校验文件是否符合要求
       // FileUtil.isFileAllowed(file.getOriginalFilename());
        System.out.println("收到了请求上传单张图片==");
        System.out.println(file);
        if (file.isEmpty()) {
            log.error("文件为空");
            return;
        }
        String fileName = file.getOriginalFilename();
        InputStream inputStream = file.getInputStream();
        QiniuOssUtils utils = new QiniuOssUtils();

        String upload = utils.upload(inputStream, fileName);
        System.out.println(upload);
    }
}
