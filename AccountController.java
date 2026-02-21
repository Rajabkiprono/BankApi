package com.example.bank.controller;

import com.example.bankingapi.dto.request.DepositRequest;
import com.example.bankingapi.dto.response.AccountResponse;
import com.example.bankingapi.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@RequestParam String accountName) {
        return ResponseEntity.ok(accountService.createAccount(accountName));
    }
    
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts() {
        return ResponseEntity.ok(accountService.getMyAccounts());
    }
    
    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }
    
    @PostMapping("/deposit")
    public ResponseEntity<AccountResponse> deposit(@Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(accountService.deposit(request));
    }
}