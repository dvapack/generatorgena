package org.flowersinvase.backend.storage;

import java.io.InputStream;

public interface StorageService {
    InputStream get(String objectKey);

    void delete(String objectKey);
}
