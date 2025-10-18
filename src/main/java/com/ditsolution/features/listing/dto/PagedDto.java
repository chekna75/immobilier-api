package com.ditsolution.features.listing.dto;

import java.util.List;

public record PagedDto<T>(List<T> items, long total, int page, int size) {}
