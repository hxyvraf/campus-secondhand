package com.campus.trade.service;

import com.campus.trade.common.BusinessException;
import com.campus.trade.common.ResultCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** 商品图片上传：保存到本地 uploads/yyyyMMdd 目录，返回可直接访问的相对地址 */
@Service
public class FileStorageService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final String uploadDir;
    private final long maxSizeBytes;
    private final List<String> allowedExt;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir,
                              @Value("${app.upload.max-size-mb:5}") long maxSizeMb,
                              @Value("${app.upload.allowed-ext:jpg,jpeg,png}") String allowedExt) {
        this.uploadDir = uploadDir;
        this.maxSizeBytes = maxSizeMb * 1024 * 1024;
        this.allowedExt = Arrays.stream(allowedExt.split(",")).map(String::trim).map(s -> s.toLowerCase(Locale.ROOT)).toList();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "上传文件不能为空");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new BusinessException(ResultCode.FILE_SIZE_EXCEEDED,
                    "文件大小超过限制（最大 " + (maxSizeBytes / 1024 / 1024) + "MB）");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
                : "";
        if (!allowedExt.contains(ext)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORTED);
        }

        String day = LocalDate.now().format(DAY_FORMAT);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Paths.get(uploadDir, day).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(fileName));
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "图片保存失败：" + e.getMessage());
        }
        return "/uploads/" + day + "/" + fileName;
    }
}
