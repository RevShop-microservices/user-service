package com.example.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerAnalyticsDTO {
    private Long sellerId;
    private int totalProducts;
    private int totalOrders;
    private int totalUnitsSold;
    private double totalRevenue;
    private int lowStockProducts;
    private List<TopProductDTO> topSellingProducts;
}
