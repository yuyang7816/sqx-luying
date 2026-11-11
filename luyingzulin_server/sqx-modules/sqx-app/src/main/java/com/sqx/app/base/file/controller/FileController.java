package com.sqx.app.base.file.controller;

import com.sqx.app.base.file.service.FileService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.file.domain.UploadRes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

/**
 * @author chengming
 * @since 2025/4/29
 */
@Tag(name = "文件", description = "文件")
@RestController
@RequestMapping("file")
public class FileController {
    @Resource
    private FileService fileService;

    @Operation(summary = "普通文件上传")
    @PostMapping("upload")
    public Result<UploadRes> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.failed("不能上传空文件");
        }
        return fileService.upload(file);
    }
}
