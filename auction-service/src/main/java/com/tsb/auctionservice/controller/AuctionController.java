package com.tsb.auctionservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.tsb.auctionservice.controller.service.AuctionService;
import common.datetime.DateTimeParser;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;

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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

}
