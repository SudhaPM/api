package com.example.coupon_management_api.entity;

public class ApplyCouponResponse {
    private double totalDiscount;
    private double finalTotal;

    public ApplyCouponResponse(double totalDiscount, double finalTotal) {
        this.totalDiscount = totalDiscount;
        this.finalTotal = finalTotal;
    }

    public double getTotalDiscount() {
        return totalDiscount;
    }

    public void setTotalDiscount(double totalDiscount) {
        this.totalDiscount = totalDiscount;
    }

    public double getFinalTotal() {
        return finalTotal;
    }

    public void setFinalTotal(double finalTotal) {
        this.finalTotal = finalTotal;
    }
}
