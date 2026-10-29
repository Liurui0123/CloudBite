package com.cloudbite.menu.controller;

import com.cloudbite.common.ApiResult;
import com.cloudbite.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "gif", "webp");

    @Value("${cloudbite.upload.dir:D:/web-ai-code/cloudBite/uploads}")
    private String uploadDir;

    @PostMapping
    public ApiResult<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的图片");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase()
                : "";
        if (!ALLOWED.contains(ext)) {
            throw new BizException("仅支持 jpg / jpeg / png / gif / webp");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BizException("图片不能超过 5MB");
        }
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath();
            Files.createDirectories(dir);
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            file.transferTo(dir.resolve(filename).toFile());
            return ApiResult.ok(Map.of("url", "/uploads/" + filename, "name", original));
        } catch (IOException e) {
            throw new BizException("图片保存失败: " + e.getMessage());
        }
    }
}