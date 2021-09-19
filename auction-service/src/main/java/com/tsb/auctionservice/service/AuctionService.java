package com.tsb.auctionservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsb.auctionservice.domain.ShippingOption;
import com.tsb.auctionservice.service.ParamSortOption;
import common.datetime.DateTimeParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class AuctionService {

    @Autowired
    private HttpHeaders httpHeaders;

    @Autowired
    private HttpEntity<String> httpEntity;

    @Autowired
    private RestTemplate restTemplate;

    public ResponseEntity<String> getResponse(String url) {
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
        return exchange;
    }

    /**
     * @param url The url that use to request and get data from trademe api
     * @return Returns a Iterable
     * @throws JsonProcessingException Throws any JsonProcessingException when trying read the tree
     */
    public Iterable<JsonNode> getListFromResponse(String url) throws JsonProcessingException {
        ResponseEntity<String> response = getResponse(url);
        ObjectMapper objectMapper = new ObjectMapper();
        Iterator<JsonNode> elements = objectMapper.readTree(response.getBody()).get("List").elements();
        Iterable<JsonNode> iterable = () -> elements;
        return iterable;
    }

    /**
     * Get no reserve auctions and sort by given sorting option
     *
     * @param url          The url that use to request and get data from trademe api
     * @param categoryName Category name interested
     * @param sortOption   Sorting option
     * @return Returns a list of JsonNode that contains no reserve auctions
     * @throws JsonProcessingException Throws any JsonProcessingException when trying to  get the list from response
     */
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

    /**
     * Get expiring auctions before the given end date and sort by the given sorting option
     *
     * @param url          The url that use to request and get data from trademe api
     * @param endTime      Auction end time
     * @param categoryName Category name interested
     * @param sortOption   Sorting option
     * @return Returns a list of JsonNode that contains expiring auctions
     * @throws JsonProcessingException Throws any JsonProcessingException when trying to  get the list from response
     */
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

    /**
     * Create a new listing
     *
     * @param itemJsonStr Json value of the item that is going to create
     * @return Returns a map of http statuses
     * @throws ParseException Throws when it can't parse the given json
     */
    public Map<HttpStatus, String> createDraftListing(String itemJsonStr) throws ParseException {
        Map<HttpStatus, String> response = new HashMap<>();
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(itemJsonStr);

        final String draftUrl = "https://api.tmsandbox.co.nz/v1/Selling/Drafts.json";
        HttpEntity<String> request = new HttpEntity<String>(jsonObject.toJSONString(), httpHeaders);

        RestTemplate template = new RestTemplate();
        ResponseEntity<String> rs = template.exchange(draftUrl, HttpMethod.POST, request, String.class);
        response.put(rs.getStatusCode(), rs.getBody());

        return response;
    }

    /**
     * Update the listing with the given shipping option and update shipping cost as free of the option is buy now
     *
     * @param listingID       Listing id that going to updated
     * @param shippingOptions The list of shipping options
     * @return Returns a map of http statuses
     * @throws ParseException Throws any ParseException when trying to parse the response body
     */
    public Map<HttpStatus, String> updateShippingOption(Long listingID, List<ShippingOption> shippingOptions) throws ParseException {
        Map<HttpStatus, String> httpResponse = new HashMap<>();

        final String listingUrl = "https://api.tmsandbox.co.nz/v1/Selling/Listings/" + listingID + ".json";
        ResponseEntity<String> response = getResponse(listingUrl);
        JSONParser parser = new JSONParser();
        JSONObject parse = (JSONObject) parser.parse(response.getBody());
        JSONArray shippingOptionsArray = ((JSONArray) parse.get("ShippingOptions"));
        if (shippingOptionsArray == null) {
            shippingOptionsArray = new JSONArray();
        }
        for (ShippingOption shippingOption : shippingOptions) {
            boolean existingOption = false;
            for (int i = 0; i < shippingOptionsArray.size(); i++) {
                JSONObject json = ((JSONObject) shippingOptionsArray.get(i));
                if (json.get("Method").equals(shippingOption.getOptionName())) {
                    json.put("Price", shippingOption.getShippingCost());
                    existingOption = true;
                    break;
                }
            }
            if (!existingOption) {
                shippingOptionsArray.add(shippingOption.getShippingOptionJson());
            }
        }


        final String draftUrl = "https://api.tmsandbox.co.nz/v1/Selling/Edit.json";
        HttpEntity<String> request = new HttpEntity<String>(parse.toJSONString(), httpHeaders);

        RestTemplate template = new RestTemplate();
        ResponseEntity<String> rs = template.exchange(draftUrl, HttpMethod.POST, request, String.class);
        httpResponse.put(rs.getStatusCode(), rs.getBody());

        return httpResponse;
    }

    /**
     * Get motors withing the given price and minimum engine size
     * @param minEngineSize Minimum engine size interested
     * @param priceMax Maximum price interested
     * @param preferredColour Preferred colour if any
     * @return Returns a Json string
     */
    public String getMotorCars(Integer minEngineSize, BigDecimal priceMax, String preferredColour) {
        final String url = "https://api.tmsandbox.co.nz/v1/Search/Motors/Used.json?engine_size_min=" + minEngineSize + "&price_max=" + priceMax + "&search_string=" + preferredColour;
        return getResponse(url).getBody();
    }
}
