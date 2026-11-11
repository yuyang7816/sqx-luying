package com.sqx.app.base.file.service.impl;

import com.sqx.app.base.file.service.FileService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.file.domain.UploadRes;
import com.sqx.common.file.factory.FileStorageFactory;
import com.sqx.common.file.storage.IFileStorage;
import com.sqx.common.redis.utils.ConfigUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author chengming
 * @since 2025/4/29
 */
@Service
public class FileServiceImpl implements FileService {
    @Override
    public Result<UploadRes> upload(MultipartFile file) {
        String fileStorageType = ConfigUtil.getConfigValue("file_storage_type");
        IFileStorage fileStorage = FileStorageFactory.getFileStorage(fileStorageType);
        UploadRes uploadRes = fileStorage.upload(file);

        return Result.success(uploadRes);
    }
}
