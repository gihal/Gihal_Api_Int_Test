package com.tsb.auctionservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.tsb.auctionservice.controller.service.AuctionService;
import com.tsb.auctionservice.domain.ShippingOption;
import common.datetime.DateTimeParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
public class AuctionController {

    @Autowired
    private AuctionService service;

    @GetMapping(value = "/auction", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<JsonNode>> getAuctions(
            @RequestParam(name = "categoryName", required = false) final String categoryName,
            @RequestParam(name = "sortOption", required = false) final String sortOption) {
        String url = "https://api.tmsandbox.co.nz/v1/Search/General.json?listed_as=Auctions";
        /*if (categoryName != null && !categoryName.isEmpty()) {
            url += "&category=" + categoryName;
        }
        if (sortOption != null && !sortOption.isEmpty()) {
            url += "&sort_order=" + sortOption;
        }*/
        try {
            List<JsonNode> list = service.getNoReserveTradeAuction(url, categoryName, sortOption);
            return ResponseEntity.ok().body(list);
        } catch (JsonProcessingException jsonProcessingException) {
            jsonProcessingException.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error on processing data.");
        } catch (HttpServerErrorException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }

    @GetMapping("/auction/expiring")
    public ResponseEntity<List<JsonNode>> getExpiringAuction(
            @RequestParam(name = "startTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm") Date startTime,
            @RequestParam(name = "endTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm") Date endTime,
            @RequestParam(name = "categoryName", required = false) String categoryName,
            @RequestParam(name = "sortOption", required = false) String sortOption) {
        String startTimeStr = DateTimeParser.formatDate(startTime);
        String url = "https://api.tmsandbox.co.nz/v1/Search/General.json?listed_as=Auctions&date_from=" + startTimeStr;
        try {
            List<JsonNode> list = service.getExpiringAuctions(url, endTime, categoryName, sortOption);
            return ResponseEntity.ok().body(list);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error on processing data.");
        } catch (HttpServerErrorException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }

    @GetMapping("/auction/motors")
    public ResponseEntity<String> getSpeedCars(
            @RequestParam(name = "minEngineSize") Integer minEngineSize,
            @RequestParam(name = "priceMax") BigDecimal priceMax,
            @RequestParam(name = "preferredColour", required = false) String preferredColour) {

        try {
            String motorCars = service.getMotorCars(minEngineSize, priceMax, preferredColour);
            return ResponseEntity.ok().body(motorCars);
        } catch (HttpServerErrorException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }


    @PostMapping("/auction")
    public ResponseEntity<String> createDraftListing(@RequestBody String item) {

        try {
            Map<HttpStatus, String> result = service.createDraftListing(item);
            HttpStatus key = null;
            for (HttpStatus status : result.keySet()) {
                key = status;
                if (status.value() != HttpStatus.OK.value()) {
                    return new ResponseEntity<>("Item not created", status);
                }
            }
            return new ResponseEntity<>(result.get(key), HttpStatus.CREATED);
        } catch (ParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Json.");
        } catch (HttpServerErrorException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }

    }

    @PostMapping("auction/update")
    public ResponseEntity<String> updateShippingOption(@RequestParam(name = "listingId") Long listingId,
                                                       @RequestBody List<ShippingOption> shippingOptions) {
        try {
            Map<HttpStatus, String> result = service.updateShippingOption(listingId, shippingOptions);
            HttpStatus key = null;
            for (HttpStatus status : result.keySet()) {
                key = status;
                if (status.value() != HttpStatus.OK.value()) {
                    return new ResponseEntity<>("Item not updates", status);
                }
            }
            return new ResponseEntity<>(result.get(key), HttpStatus.OK);
        } catch (ParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Json.");
        }
    }

}
