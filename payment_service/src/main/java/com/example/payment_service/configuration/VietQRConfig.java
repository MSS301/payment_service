package com.example.payment_service.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payos.vietqr")
@Data
public class VietQRConfig {
    private String bankID;
    private String accountNo;
    private String template;
}