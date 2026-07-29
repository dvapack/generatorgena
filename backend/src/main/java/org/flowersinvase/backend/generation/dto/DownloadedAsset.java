package org.flowersinvase.backend.generation.dto;

import java.io.InputStream;

public record DownloadedAsset(
        InputStream content,
        String contentType,
        Integer contentLength,
        String filename
) {
}
