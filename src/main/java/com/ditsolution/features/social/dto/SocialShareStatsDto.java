package com.ditsolution.features.social.dto;

import java.util.Map;

public class SocialShareStatsDto {
    public Long totalShares;
    public Map<String, Long> sharesByPlatform;
    public Map<String, Long> sharesByType;
    public Long uniqueUsers;
    public Long uniqueListings;
    public Map<String, Long> recentShares; // Par jour/semaine

    public SocialShareStatsDto() {}

    public SocialShareStatsDto(Long totalShares, Map<String, Long> sharesByPlatform, 
                              Map<String, Long> sharesByType, Long uniqueUsers, 
                              Long uniqueListings, Map<String, Long> recentShares) {
        this.totalShares = totalShares;
        this.sharesByPlatform = sharesByPlatform;
        this.sharesByType = sharesByType;
        this.uniqueUsers = uniqueUsers;
        this.uniqueListings = uniqueListings;
        this.recentShares = recentShares;
    }
}
