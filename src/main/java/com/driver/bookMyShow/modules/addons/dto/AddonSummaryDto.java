package com.driver.bookMyShow.modules.addons.dto;

import com.driver.bookMyShow.modules.addons.enums.AddonType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddonSummaryDto {
    private Integer addonId;
    private AddonType type;
    private Double amount;
    private String status;
    private String details; // Human-readable description
    private Object metadata; // Type-specific data
}
