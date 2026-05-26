package com.example.user_service.client;

import com.example.user_service.dto.SellerAnalyticsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "PRODUCT-SERVICE")
public interface SellerClient {

    @GetMapping("/api/seller/analytics/{sellerId}")
    SellerAnalyticsDTO getSellerAnalytics(@PathVariable Long sellerId);
}
