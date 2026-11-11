package com.sqx.app.base.file.service;

import com.sqx.common.core.domain.Result;
import com.sqx.common.file.domain.UploadRes;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author chengming
 * @since 2025/4/29
 */
public interface FileService {
    Result<UploadRes> upload(MultipartFile file);
}
