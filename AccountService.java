package com.example.bank.service;

import com.example.bankingapi.dto.request.DepositRequest;
import com.example.bankingapi.dto.response.AccountResponse;
import com.example.bankingapi.model.Account;
import com.example.bankingapi.model.Transaction;
import com.example.bankingapi.model.TransactionType;
import com.example.bankingapi.model.User;
import com.example.bankingapi.repository.AccountRepository;
import com.example.bankingapi.repository.TransactionRepository;
import com.example.bankingapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    
    public AccountResponse createAccount(String accountName) {
        User currentUser = getCurrentUser();
        
        // Generate unique account number
        String accountNumber = generateAccountNumber();
        
        Account account = Account.builder()
                .accountNumber(accountNumber)
                .accountName(accountName)
                .balance(BigDecimal.ZERO)
                .user(currentUser)
                .createdAt(LocalDateTime.now())
                .build();
        
        account = accountRepository.save(account);
        
        return mapToResponse(account);
    }
    
    public List<AccountResponse> getMyAccounts() {
        User currentUser = getCurrentUser();
        return accountRepository.findByUser(currentUser)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public AccountResponse getAccount(String accountNumber) {
        Account account = findAccountByNumber(accountNumber);
        
        // Security check: only owner can view
        User currentUser = getCurrentUser();
        if (!account.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You don't have permission to view this account");
        }
        
        return mapToResponse(account);
    }
    
    @Transactional
    public AccountResponse deposit(DepositRequest request) {
        Account account = findAccountByNumber(request.getAccountNumber());
        
        // Update balance
        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);
        
        // Create transaction record
        Transaction transaction = Transaction.builder()
                .transactionId(generateTransactionId())
                .type(TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .description("Deposit to account")
                .account(account)
                .timestamp(LocalDateTime.now())
                .build();
        
        transactionRepository.save(transaction);
        
        return mapToResponse(account);
    }
    
    // Helper methods
    private User getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    private Account findAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }
    
    private String generateAccountNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(random.nextInt(10));
        }
        String accountNumber = sb.toString();
        
        // Ensure uniqueness
        while (accountRepository.existsByAccountNumber(accountNumber)) {
            accountNumber = generateAccountNumber();
        }
        
        return accountNumber;
    }
    
    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + new Random().nextInt(1000);
    }
    
    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountName(account.getAccountName())
                .balance(account.getBalance())
                .createdAt(account.getCreatedAt())
                .build();
    }
}