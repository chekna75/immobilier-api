package com.ditsolution.features.social.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class SocialShareDto {
    public UUID id;
    public UUID listingId;
    public UUID userId;
    public String userName;
    public String platform;
    public String shareType;
    public OffsetDateTime sharedAt;
    public Map<String, Object> metadata;

    public SocialShareDto() {}

    public SocialShareDto(UUID id, UUID listingId, UUID userId, String userName, 
                         String platform, String shareType, OffsetDateTime sharedAt, 
                         Map<String, Object> metadata) {
        this.id = id;
        this.listingId = listingId;
        this.userId = userId;
        this.userName = userName;
        this.platform = platform;
        this.shareType = shareType;
        this.sharedAt = sharedAt;
        this.metadata = metadata;
    }
}
