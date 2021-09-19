package com.tsb.auctionservice.domain;

import org.json.simple.JSONObject;

import java.math.BigDecimal;

public class ShippingOption {
    private String optionName;
    private BigDecimal shippingCost;
    private boolean buyNowOption;
    private int shippingType;

    public String getOptionName() {
        return optionName;
    }

    public void setOptionName(String optionName) {
        this.optionName = optionName;
    }

    public BigDecimal getShippingCost() {
        if (this.buyNowOption) {
            return BigDecimal.ZERO;
        }
        return shippingCost;
    }

    public void setShippingCost(BigDecimal shippingCost) {
        this.shippingCost = shippingCost;
    }


    public boolean isBuyNowOption() {
        return buyNowOption;
    }

    public void setBuyNowOption(boolean buyNowOption) {
        this.buyNowOption = buyNowOption;
    }

    public int getShippingType() {
        return shippingType;
    }

    public void setShippingType(int shippingType) {
        this.shippingType = shippingType;
    }

    public JSONObject getShippingOptionJson() {
        JSONObject json = new JSONObject();
        json.put("Type", this.shippingType);
        json.put("Price", this.buyNowOption ? 0.0 : this.shippingCost);
        json.put("Method", this.optionName);
        return json;
    }
}
