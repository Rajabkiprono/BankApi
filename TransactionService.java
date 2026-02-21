package com.example.bank.service;

import com.example.bankingapi.dto.request.TransferRequest;
import com.example.bankingapi.dto.response.AccountResponse;
import com.example.bankingapi.dto.response.TransactionResponse;
import com.example.bankingapi.model.Account;
import com.example.bankingapi.model.Transaction;
import com.example.bankingapi.model.TransactionType;
import com.example.bankingapi.model.User;
import com.example.bankingapi.repository.AccountRepository;
import com.example.bankingapi.repository.TransactionRepository;
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
public class TransactionService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    
    @Transactional
    public AccountResponse transfer(TransferRequest request) {
        // Validate accounts
        Account fromAccount = findAccountByNumber(request.getFromAccountNumber());
        Account toAccount = findAccountByNumber(request.getToAccountNumber());
        
        // Security check: only owner can transfer from their account
        User currentUser = getCurrentUser();
        if (!fromAccount.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You don't have permission to transfer from this account");
        }
        
        // Check sufficient balance
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance");
        }
        
        // Perform transfer
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));
        
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        
        // Create transaction records
        String transactionId = generateTransactionId();
        
        // Debit transaction for sender
        Transaction debitTransaction = Transaction.builder()
                .transactionId(transactionId)
                .type(TransactionType.TRANSFER)
                .amount(request.getAmount().negate())
                .description(request.getDescription() != null ? request.getDescription() : "Transfer to " + request.getToAccountNumber())
                .account(fromAccount)
                .destinationAccountNumber(request.getToAccountNumber())
                .timestamp(LocalDateTime.now())
                .build();
        
        // Credit transaction for receiver
        Transaction creditTransaction = Transaction.builder()
                .transactionId(transactionId)
                .type(TransactionType.TRANSFER)
                .amount(request.getAmount())
                .description(request.getDescription() != null ? request.getDescription() : "Transfer from " + request.getFromAccountNumber())
                .account(toAccount)
                .destinationAccountNumber(request.getFromAccountNumber())
                .timestamp(LocalDateTime.now())
                .build();
        
        transactionRepository.save(debitTransaction);
        transactionRepository.save(creditTransaction);
        
        return accountService.getAccount(request.getFromAccountNumber());
    }
    
    public List<TransactionResponse> getAccountTransactions(String accountNumber) {
        Account account = findAccountByNumber(accountNumber);
        
        // Security check
        User currentUser = getCurrentUser();
        if (!account.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You don't have permission to view these transactions");
        }
        
        return transactionRepository.findByAccountOrderByTimestampDesc(account)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    // Helper methods
    private User getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return accountService.findUserByEmail(userDetails.getUsername());
    }
    
    private Account findAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));
    }
    
    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + new Random().nextInt(1000);
    }
    
    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .transactionId(transaction.getTransactionId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .accountNumber(transaction.getAccount().getAccountNumber())
                .destinationAccount(transaction.getDestinationAccountNumber())
                .timestamp(transaction.getTimestamp())
                .build();
    }
}