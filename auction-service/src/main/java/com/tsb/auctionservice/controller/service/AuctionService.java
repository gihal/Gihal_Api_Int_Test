package com.tsb.auctionservice.controller.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsb.auctionservice.service.ParamSortOption;
import common.datetime.DateTimeParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class AuctionService {
    @Autowired
    private HttpEntity<String> httpEntity;

    @Autowired
    private RestTemplate restTemplate;

    public ResponseEntity<String> getResponse(String url) {
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
        return exchange;
    }

    public Iterable<JsonNode> getListFromResponse(String url) throws JsonProcessingException {
        ResponseEntity<String> response = getResponse(url);
        ObjectMapper objectMapper = new ObjectMapper();
        Iterator<JsonNode> elements = objectMapper.readTree(response.getBody()).get("List").elements();
        Iterable<JsonNode> iterable = () -> elements;
        return iterable;
    }

    public List<JsonNode> getNoReserveTradeAuction(String url, String categoryName, String sortOption) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        Iterable<JsonNode> iterable = getListFromResponse(url);
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
        return list;

    }

    public List<JsonNode> getExpiringAuctions(String url, Date endTime, String categoryName, String sortOption) throws JsonProcessingException {
        Iterable<JsonNode> iterable = getListFromResponse(url);
        List<JsonNode> list = StreamSupport
                .stream(iterable.spliterator(), false)
                .filter(jsonNode ->
                        (categoryName == null || jsonNode.get("Category").asText().startsWith(categoryName))
                                && (jsonNode.get("ReserveState") != null && jsonNode.get("ReserveState").asInt() == 2)
                                && (DateTimeParser.parseDate(jsonNode.get("EndDate").textValue()) != null && DateTimeParser.parseDate(jsonNode.get("EndDate").textValue()).before(endTime))
                )
                .collect(Collectors.toList());

        if (sortOption != null) {
            if (ParamSortOption.ExpiryAsc.toString().equals(sortOption)) {
                list.sort(
                        (JsonNode node1, JsonNode node2) -> DateTimeParser.parseDate(node1.get("EndDate").textValue()).compareTo(DateTimeParser.parseDate(node2.get("EndDate").textValue()))
                );
            } else if (ParamSortOption.ExpiryDesc.toString().equals(sortOption)) {
                list.sort(
                        (JsonNode node1, JsonNode node2) -> DateTimeParser.parseDate(node2.get("EndDate").textValue()).compareTo(DateTimeParser.parseDate(node1.get("EndDate").textValue()))
                );
            }
        }
        return list;
    }
}
