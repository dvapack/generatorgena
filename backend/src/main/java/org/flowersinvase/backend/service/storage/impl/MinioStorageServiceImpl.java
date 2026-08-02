package org.flowersinvase.backend.service.storage.impl;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowersinvase.backend.exception.minio.StorageUnavailableException;
import org.flowersinvase.backend.config.minio.MinioProperties;
import org.flowersinvase.backend.service.storage.StorageService;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageServiceImpl implements StorageService {
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;


    @Override
    public InputStream get(String objectKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(objectKey)
                            .build()
            );
        } catch (MinioException e) {
            log.error("Ошибка minio при получении объекта: objectKey={} ", objectKey, e);
            throw new StorageUnavailableException("Файловое хранилище временно недоступно", e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(objectKey)
                            .build()
            );
        } catch (MinioException e) {
            log.error("Ошибка minio при удалении объекта: objectKey={} ", objectKey, e);
            throw new StorageUnavailableException("Файловое хранилище временно недоступно", e);
        }
    }
}
