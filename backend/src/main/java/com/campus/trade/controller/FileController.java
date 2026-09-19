package com.campus.trade.controller;

import com.campus.trade.common.Result;
import com.campus.trade.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** 图片上传模块 */
@Tag(name = "05-文件", description = "商品图片上传")
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Operation(summary = "上传商品图片",
            description = "multipart/form-data，字段名 file；仅支持 jpg/jpeg/png，单张最大 5MB；"
                    + "类型不支持返回 400/40002，超过大小返回 400/40003。返回的 url 可直接访问或用于发布商品")
    @PostMapping("/images")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.store(file);
        return Result.ok("上传成功", Map.of("url", url));
    }
}
