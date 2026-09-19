package org.example.knowqa.document.service;

import io.minio.*;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {
    @Resource
    private MinioClient minioClient;
    @Value("${minio.bucketName}")
    private String bucketName;
    @Value("${minio.endpoint}")
    private String endpoint;
    public String uploadFile(MultipartFile file, String objectName) throws Exception {
        createBucketIfNotExists(true);
        minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(file.getInputStream(),file.getSize(),-1)
                        .contentType(file.getContentType())
                .build());
        return String.format("%s/%s/%s",endpoint,bucketName,objectName);
    }

    private void createBucketIfNotExists(boolean publicRead) throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            // 设置 bucket 策略为公共读
            if (publicRead) {
                String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::" + bucketName + "/*\"]}]}";
                minioClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder()
                                .bucket(bucketName)
                                .config(policy)
                                .build()
                );
            }
        }
    }
}
