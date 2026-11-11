package com.sqx.common.file.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.file.domain.UploadParam;
import com.sqx.common.file.domain.UploadRes;
import com.sqx.common.file.enums.FileError;
import com.sqx.common.file.utils.FileUtil;
import com.sqx.common.redis.utils.ConfigUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * @author chengming
 * @since 2025/1/17
 */
@Slf4j
public class AliOssFileStorage implements IFileStorage {

    private static final DateTimeFormatter DATE_FOLDER_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Override
    public UploadRes upload(MultipartFile file) {
        String endpoint = ConfigUtil.getConfigValue("file_endpoint");
        String bucketName = ConfigUtil.getConfigValue("file_bucket");
        String accessKeyId = ConfigUtil.getConfigValue("file_ali_oss_accessKeyId");
        String secretAccessKey = ConfigUtil.getConfigValue("file_ali_oss_secretAccessKey");
        String domain = ConfigUtil.getConfigValue("file_cdn_domain");

        String extension = FileUtil.getFileExtension(file.getOriginalFilename());
        String objectKey = buildObjectKey(extension);

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, secretAccessKey);
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName,
                    objectKey,
                    new ByteArrayInputStream(file.getBytes())
            );
            PutObjectResult result = ossClient.putObject(putObjectRequest);
            ossClient.setObjectAcl(bucketName, objectKey, CannedAccessControlList.PublicRead);
            log.info("文件上传成功, key: {}, eTag: {}", objectKey, result.getETag());
        } catch (OSSException oe) {
            log.error("文件上传失败, 错误信息{}", oe.getErrorMessage());
            log.error("Request ID: {}", oe.getRequestId());
            log.error("Host ID: {}", oe.getHostId());
            throw new BizException(FileError.UPLOAD_FAILED);
        } catch (IOException e) {
            log.error("文件读取失败: {}", e.getMessage());
            throw new BizException(FileError.UPLOAD_FAILED);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        return buildUploadRes(objectKey, extension, file.getSize(), domain);
    }

    @Override
    public UploadRes upload(UploadParam uploadParam) {
        String extension = FileUtil.getFileExtension(uploadParam.getFileName());
        String objectKey = buildObjectKey(extension);

        OSS ossClient = new OSSClientBuilder().build(
                uploadParam.getEndpoint(),
                uploadParam.getAccessKeyId(),
                uploadParam.getSecretAccessKey()
        );
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    uploadParam.getBucket(),
                    objectKey,
                    uploadParam.getInputStream()
            );
            PutObjectResult result = ossClient.putObject(putObjectRequest);
            ossClient.setObjectAcl(uploadParam.getBucket(), objectKey, CannedAccessControlList.PublicRead);
            log.info("文件上传成功, key: {}, eTag: {}", objectKey, result.getETag());
        } catch (OSSException oe) {
            log.error("文件上传失败, 错误信息{}", oe.getErrorMessage());
            log.error("Request ID: {}", oe.getRequestId());
            log.error("Host ID: {}", oe.getHostId());
            throw new BizException(FileError.UPLOAD_FAILED);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        return buildUploadRes(objectKey, extension, 0L, ConfigUtil.getConfigValue("file_cdn_domain"));
    }

    private static String buildObjectKey(String extension) {
        String dateFolder = LocalDateTime.now().format(DATE_FOLDER_FORMATTER);
        String fileName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        return dateFolder + "/" + fileName;
    }

    private static UploadRes buildUploadRes(String objectKey, String extension, long size, String domain) {
        String normalizedKey = FileUtil.normalizeObjectKey(objectKey);
        UploadRes res = new UploadRes();
        res.setPath(FileUtil.joinPublicUrl(domain, normalizedKey));
        res.setRelativePath("/" + normalizedKey);
        res.setExtension(extension);
        res.setSize(size);
        return res;
    }
}
