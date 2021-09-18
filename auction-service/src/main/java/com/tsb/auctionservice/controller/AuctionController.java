package com.tsb.auctionservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsb.auctionservice.service.ParamSortOption;
import common.datetime.DateTimeParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
public class AuctionController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private HttpEntity<String> httpEntity;


    @GetMapping(value = "/auction")
    public ResponseEntity<Object> getAuctions(
            @RequestParam(name = "categoryName", required = false) final String categoryName,
            @RequestParam(name = "sortOption", required = false) final String sortOption) {
        String url = "https://api.tmsandbox.co.nz/v1/Search/General.json?listed_as=Auctions&depth=0";
        /*if (categoryName != null && !categoryName.isEmpty()) {
            url += "&category=" + categoryName;
        }
        if (sortOrder != null && !sortOrder.isEmpty()) {
            url += "&sort_order=" + sortOrder;
        }*/


        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        try {

            Iterator<JsonNode> elements = objectMapper.readTree(exchange.getBody()).get("List").elements();
            Iterable<JsonNode> iterable = () -> elements;
            List<JsonNode> list = StreamSupport
                    .stream(iterable.spliterator(), false)
                    .filter(jsonNode ->
                            (categoryName == null || jsonNode.get("Category").asText().startsWith(categoryName))
                                    && (jsonNode.get("HasReserve") == null || !jsonNode.get("HasReserve").asBoolean())
                    )
                    .collect(Collectors.toList());

            if (sortOption != null) {
                if (ParamSortOption.Title.toString().equals(sortOption)) {
                    list.sort(
                            (JsonNode node1, JsonNode node2) -> node1.get("Title").textValue().compareTo(node2.get("Title").textValue())
                    );
                } else if (ParamSortOption.ExpiryAsc.toString().equals(sortOption)) {
                    list.sort(
                            (JsonNode node1, JsonNode node2) -> DateTimeParser.parseDate(node1.get("EndDate").textValue()).compareTo(DateTimeParser.parseDate(node2.get("EndDate").textValue()))
                    );
                } else if (ParamSortOption.ExpiryDesc.toString().equals(sortOption)) {
                    list.sort(
                            (JsonNode node1, JsonNode node2) -> DateTimeParser.parseDate(node2.get("EndDate").textValue()).compareTo(DateTimeParser.parseDate(node1.get("EndDate").textValue()))
                    );
                } else if (ParamSortOption.PriceAsc.toString().equals(sortOption)) {
                    list.sort(
                            (JsonNode node1, JsonNode node2) -> node1.get("StartPrice").decimalValue().compareTo(node2.get("StartPrice").decimalValue())
                    );
                } else if (ParamSortOption.PriceDesc.toString().equals(sortOption)) {
                    list.sort(
                            (JsonNode node1, JsonNode node2) -> node2.get("StartPrice").decimalValue().compareTo(node1.get("StartPrice").decimalValue())
                    );
                }

            }

            return ResponseEntity.ok().body(list);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error on processing data...");
        }
    }
}
