package org.flowersinvase.backend.model;

import java.io.InputStream;

public record DownloadedAsset(
        InputStream content,
        String contentType,
        Integer contentLength,
        String filename
) {
}
