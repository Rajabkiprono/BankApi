package com.example.bank.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferRequest {
    @NotBlank(message = "Source account number is required")
    private String fromAccountNumber;
    
    @NotBlank(message = "Destination account number is required")
    private String toAccountNumber;
    
    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be at least 1")
    private BigDecimal amount;
    
    private String description;
}