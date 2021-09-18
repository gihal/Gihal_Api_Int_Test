package com.tsb.auctionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class AuctionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuctionServiceApplication.class, args);
    }

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public HttpEntity<String> getHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "OAuth oauth_consumer_key=F5D8D8DD2E3930F1660717B47349A119,oauth_token=1D95711C9E300A787414D812BC60F6B2,oauth_signature_method=PLAINTEXT,oauth_version=1.0,oauth_signature=569B5F6E66386F57A99129DD34024778%26ADDAF845704EAD682F7963F01F6A80EF");
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);
        return httpEntity;
    }

}
