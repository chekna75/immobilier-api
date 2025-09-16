package com.ditsolution.features.storage.dto;

import lombok.Data;

@Data
public class UploadRequestDto {
    private String fileName;
    private String contentType;
}
