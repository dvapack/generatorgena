package org.flowersinvase.backend.generation.storage.service;

import java.io.InputStream;

public interface StorageService {
    InputStream get(String objectKey);

    void delete(String objectKey);
}
