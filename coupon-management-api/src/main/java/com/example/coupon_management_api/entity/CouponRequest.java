package com.example.coupon_management_api.entity;

import java.time.LocalDate;
import java.util.Map;

public class CouponRequest {
    private String type;
    private Map<String,Object> details;
    private LocalDate expirationDate;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }
}
