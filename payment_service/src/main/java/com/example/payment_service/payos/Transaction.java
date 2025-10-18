package com.example.payment_service.payos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Transaction - Thông tin giao dịch
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private String reference;
    private Integer amount;
    private String accountNumber;
    private String description;
    private String transactionDateTime;
    private String virtualAccountName;
    private String virtualAccountNumber;
    private String counterAccountBankId;
    private String counterAccountBankName;
    private String counterAccountName;
    private String counterAccountNumber;
}