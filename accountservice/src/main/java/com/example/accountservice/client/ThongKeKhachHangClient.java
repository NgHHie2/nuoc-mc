package com.example.accountservice.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.accountservice.model.Account;

@Component
public class ThongKeKhachHangClient {
    @Autowired
    private WebClient.Builder webClientBuilder;

    @Value("${thongkekhachhang.service.url}") 
    private String thongKeaccountserviceUrl;

    public Boolean updateKhachHang (String state, Account kh) {
        switch (state) {
            case "create":
                return webClientBuilder.build().post()
                    .uri(thongKeaccountserviceUrl)
                    .bodyValue(kh)  
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();
            
            case "update":
                return webClientBuilder.build().put()
                    .uri(thongKeaccountserviceUrl + "/" + kh.getId())
                    .bodyValue(kh)  
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();
        
            case "delete":
                return webClientBuilder.build().delete()
                    .uri(thongKeaccountserviceUrl + "/" + kh.getId())
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();
                    
            default:
                return false;
        }
    }
}
