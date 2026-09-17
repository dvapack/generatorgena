package org.flowersinvase.backend.service.storage;

import java.io.InputStream;

public interface StorageService {
    InputStream get(String objectKey);

    void delete(String objectKey);
}
