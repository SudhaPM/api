package com.example.coupon_management_api.controller;

import com.example.coupon_management_api.entity.*;
import com.example.coupon_management_api.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coupons")
public class CouponController {

    @Autowired
    CouponService couponService;

    @GetMapping
    public ResponseEntity<List<Coupon>> getAllCoupons(){
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Coupon> getCouponById(@PathVariable Long id){
        return ResponseEntity.ok(couponService.getCouponById(id));
    }

    @PostMapping("/add")
    public ResponseEntity<Coupon> createCoupon(@RequestBody CouponRequest coupon) throws Exception {
        return ResponseEntity.ok(couponService.createCoupon(coupon));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Coupon> updateCoupon(@PathVariable Long id, @RequestBody Coupon coupon){
        return ResponseEntity.ok(couponService.updateCoupon(id, coupon));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long id){
        couponService.deleteCoupon(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/applicable-coupons")
    public ResponseEntity<List<CouponResponse>> getApplicableCoupons(@RequestBody CartRequest request){
        return ResponseEntity.ok(couponService.getApplicableCoupons(request));
    }

    @PostMapping("/applicable-coupons/{id}")
    public ResponseEntity<ApplyCouponResponse> applyCoupon(@PathVariable Long id, @RequestBody CartRequest request){
        return ResponseEntity.ok(couponService.applyCoupon(id, request));
    }

}
