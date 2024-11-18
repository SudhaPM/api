package com.example.coupon_management_api.service;

import com.example.coupon_management_api.entity.*;
import com.example.coupon_management_api.repository.CouponRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CouponService {

    @Autowired
    CouponRepository couponRepository;

    public List<Coupon> getAllCoupons(){
        return couponRepository.findAll();
    }

    public Coupon getCouponById(Long id){
        return couponRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Coupon with id " +id+ "not found"));
    }

    public Coupon createCoupon(CouponRequest coupon) throws Exception {
        Coupon coupon1 = new Coupon();
        coupon1.setType(coupon.getType());
        coupon1.setDetails(new ObjectMapper().writeValueAsString(coupon.getDetails()));
        coupon1.setExpirationDate(coupon.getExpirationDate());
        return couponRepository.save(coupon1);
    }

    public Coupon updateCoupon(Long id, Coupon coupon){
        Coupon existingCoupon = getCouponById(id);
        existingCoupon.setType(coupon.getType());
        existingCoupon.setDetails(coupon.getDetails());
        existingCoupon.setExpirationDate(coupon.getExpirationDate());
        return couponRepository.save(coupon);
    }

    public void deleteCoupon(Long id){
        couponRepository.deleteById(id);
    }

    public List<CouponResponse> getApplicableCoupons(CartRequest cart){
        List<Coupon> coupons = couponRepository.findAll();//fetch all coupons
        List<CouponResponse> applicableCoupons = new ArrayList<>();

        //total cart value
        double totalCaetValue = cart.getItems().stream().mapToDouble(item -> item.getPrice()*item.getQuantity()).sum();

        for (Coupon coupon : coupons){
            try {
                Map<String,Object> details = new ObjectMapper().readValue(coupon.getDetails(), Map.class);
                if("cart-wise".equals(coupon.getType())){
                    double threshold = ((Number) details.get("threshold")).doubleValue();
                    double discountPercent = ((Number) details.get("discount")).doubleValue();
                    if (totalCaetValue>=threshold){
                        double discount = (totalCaetValue*discountPercent)/100;
                        applicableCoupons.add(new CouponResponse(coupon.getId(),coupon.getType(),discount));
                    }
                } else if ("product-wise".equals(coupon.getType())) {
                    List<Map<String,Object>> products = (List<Map<String,Object>>) details.get("products");
                    for (Map<String,Object> productRole : products){
                        Long productId = ((Number) productRole.get("product_id")).longValue();
                        int requiredQty = ((Number) productRole.get("quantity")).intValue();
                        double discountPerunit = ((Number) productRole.get("discount")).doubleValue();
                        int cartQty = cart.getItems().stream().filter(item->item.getProductId().equals(productId))
                                .mapToInt(CartItem::getQuantity).sum();
                        if(cartQty>=requiredQty){
                            double discount = discountPerunit*cartQty;
                            applicableCoupons.add(new CouponResponse(coupon.getId(),coupon.getType(),discount));
                        }
                    }
                } else if ("bxgy".equals(coupon.getType())) {
                    List<Map<String,Object>> buyProducts = (List<Map<String,Object>>) details.get("buy_products");
                    List<Map<String,Object>> getProducts = (List<Map<String,Object>>) details.get("get_products");
                    int repititionLimit = details.containsKey("repetition_limt")?((Number) details.get("repetition_limt")).intValue():Integer.MAX_VALUE;

                    int repetitions = Integer.MAX_VALUE;
                    for (Map<String,Object> buyProduct:buyProducts){
                        Long productId = ((Number) buyProduct.get("product_id")).longValue();
                        int requiredQty = ((Number) buyProduct.get("quantity")).intValue();
                        int cartQty = cart.getItems().stream().filter(item->item.getProductId().equals(productId))
                                .mapToInt(CartItem::getQuantity).sum();
                        repetitions=Math.min(repetitions,cartQty/requiredQty);
                    }
                    repetitions=Math.min(repetitions,repititionLimit);
                    if (repetitions>0){
                        double totaldiscount = 0.0;
                        for (Map<String,Object> getProduct:getProducts) {
                            Long productId = ((Number) getProduct.get("product_id")).longValue();
                            int freeQty = ((Number) getProduct.get("quantity")).intValue();
                            double productPrice = cart.getItems().stream()
                                    .filter(item -> item.getProductId().equals(productId))
                                    .mapToDouble(CartItem::getPrice)
                                    .findFirst()
                                    .orElse(0);
                            totaldiscount += freeQty * productPrice * repetitions;
                        }
                        applicableCoupons.add(new CouponResponse(coupon.getId(),coupon.getType(),totaldiscount));
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return applicableCoupons;
    }


    public ApplyCouponResponse applyCoupon(Long couponId, CartRequest request){
        Coupon coupon = couponRepository.findById(couponId).orElseThrow(()->new IllegalArgumentException("coupon not found"));

        double totalCartValue = request.getItems().stream().mapToDouble(item -> item.getPrice()*item.getQuantity()).sum();

        double totalDiscount = 0;

        try{
            Map<String,Object> details = new ObjectMapper().readValue(coupon.getDetails(), Map.class);
            switch (coupon.getType()){
                case "cart-wise":
                    totalDiscount = applyCartwiseDiscount(details, totalCartValue);
                    break;
                case "product-wise":
                    totalDiscount = applyProductwiseDiscount(details, request);
                    break;
                case "bxgy":
                    totalDiscount = applyBxGyDiscount(details, request);
                    break;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        double finalTotal = totalCartValue - totalDiscount;
        return new ApplyCouponResponse(totalDiscount, finalTotal);

    }

    private double applyCartwiseDiscount(Map<String, Object> details, double totalCartValue) {
        double threshold = ((Number) details.get("threshold")).doubleValue();
        double discountPercent = ((Number) details.get("discount")).doubleValue();
        if (totalCartValue>=threshold){
          return  (totalCartValue*discountPercent)/100;
        }
        return 0;
    }

    private double applyProductwiseDiscount(Map<String, Object> details, CartRequest request) {
        List<Map<String,Object>> products = (List<Map<String,Object>>) details.get("products");
        double totalDiscount = 0;

        for (Map<String,Object> productRole : products){
            Long productId = ((Number) productRole.get("product_id")).longValue();
            double discountPerunit = ((Number) productRole.get("discount")).doubleValue();

            for (CartItem item: request.getItems()){
                if (item.getProductId().equals(productId)){
                    double productDiscount = (item.getPrice()*item.getQuantity()*discountPerunit)/100;
                    totalDiscount+=productDiscount;
                }
            }
        }
        return totalDiscount;
    }

    private double applyBxGyDiscount(Map<String, Object> details, CartRequest cartRequest) {
        List<Map<String,Object>> buyProducts = (List<Map<String,Object>>) details.get("buy_products");
        List<Map<String,Object>> getProducts = (List<Map<String,Object>>) details.get("get_products");
        int repititionLimit = details.containsKey("repetition_limt")?((Number) details.get("repetition_limt")).intValue():Integer.MAX_VALUE;

        int repetitions = Integer.MAX_VALUE;
        for (Map<String,Object> buyProduct:buyProducts){
            Long productId = ((Number) buyProduct.get("product_id")).longValue();
            int requiredQty = ((Number) buyProduct.get("quantity")).intValue();
            int cartQty = cartRequest.getItems().stream().filter(item->item.getProductId().equals(productId))
                    .mapToInt(CartItem::getQuantity).sum();
            repetitions=Math.min(repetitions,cartQty/requiredQty);
        }
        repetitions=Math.min(repetitions,repititionLimit);
        double totaldiscount = 0.0;
        if (repetitions>0){

            for (Map<String,Object> getProduct:getProducts) {
                Long productId = ((Number) getProduct.get("product_id")).longValue();
                int freeQty = ((Number) getProduct.get("quantity")).intValue();
                double productPrice = cartRequest.getItems().stream()
                        .filter(item -> item.getProductId().equals(productId))
                        .mapToDouble(CartItem::getPrice)
                        .findFirst()
                        .orElse(0);
                totaldiscount += freeQty * productPrice * repetitions;
            }
        }

        return totaldiscount;
    }

}
