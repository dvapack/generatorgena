package org.flowersinvase.backend.dto.generation;

import java.io.InputStream;

public record DownloadedAsset(
        InputStream content,
        String contentType,
        Integer contentLength,
        String filename
) {
}
