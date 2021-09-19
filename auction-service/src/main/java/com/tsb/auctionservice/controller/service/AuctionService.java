package com.tsb.auctionservice.controller.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsb.auctionservice.service.ParamSortOption;
import common.datetime.DateTimeParser;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
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

    public Map<HttpStatus, String> createDraftListing(String itemJsonStr) throws ParseException {
        Map<HttpStatus, String> response = new HashMap<>();
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(itemJsonStr);

        final String draftUrl = "https://api.tmsandbox.co.nz/v1/Selling/Drafts.json";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "OAuth oauth_consumer_key=F5D8D8DD2E3930F1660717B47349A119,oauth_token=1D95711C9E300A787414D812BC60F6B2,oauth_signature_method=PLAINTEXT,oauth_version=1.0,oauth_signature=569B5F6E66386F57A99129DD34024778%26ADDAF845704EAD682F7963F01F6A80EF");
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<String> request = new HttpEntity<String>(jsonObject.toJSONString(), headers);

        RestTemplate template = new RestTemplate();
        ResponseEntity<String> rs = template.exchange(draftUrl, HttpMethod.POST, request, String.class);
        response.put(rs.getStatusCode(), rs.getBody());

        return response;
    }
}
