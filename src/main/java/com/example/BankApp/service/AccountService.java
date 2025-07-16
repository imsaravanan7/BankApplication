package com.example.BankApp.service;

import com.example.BankApp.model.Account;
import com.example.BankApp.model.Transaction;
import com.example.BankApp.repository.AccountRepository;
import com.example.BankApp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AccountService implements UserDetailsService {

    @Autowired
    PasswordEncoder passwordEncoder;
    private static final String BANK_PREFIX = "522003"; // With fixed unique number for bank

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    public Account findAccountByUsername(String username) {
        return accountRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public Account registerAccount(String username, String password) {
        if (accountRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        // Generate the random account number doesn't exist in DB
        Account account = new Account();
        long accountNumber;
        do {
            accountNumber = ThreadLocalRandom.current().nextLong(5203000000L, 5203999999L);
        } while (accountRepository.existsByAccountNo(accountNumber));

        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(password)); // Encrypt password
        account.setBalance(BigDecimal.ZERO); // Initial balance set to 0
        account.setAccountNo(accountNumber); // Set the unique account number
        return accountRepository.save(account);
    }

    public void deposit(Account account, BigDecimal amount) {
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        Transaction transaction = new Transaction(
                amount,
                "Deposit",
                LocalDateTime.now(),
                account
        );
        transactionRepository.save(transaction);
    }

    public void withdraw(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        Transaction transaction = new Transaction(
                amount,
                "Withdrawal",
                LocalDateTime.now(),
                account
        );
        transactionRepository.save(transaction);
    }

    public List<Transaction> getTransactionHistory(Account account) {
        return transactionRepository.findByAccountId(account.getId());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Account account = findAccountByUsername(username);
        if (account == null) {
            throw new UsernameNotFoundException("Username or Password not found");
        }
        return new Account(
                account.getUsername(),
                account.getAccountNo(),
                account.getPassword(),
                account.getBalance(),
                account.getTransactions(),
                authorities());
    }

    public Collection<? extends GrantedAuthority> authorities() {
        return Arrays.asList(new SimpleGrantedAuthority("USER"));
    }

    public void transferAmount(Account fromAccount, Long toAccountNo, BigDecimal amount, String toUsername) {
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }

        if (fromAccount.getAccountNo().equals(toAccountNo)) {
            throw new RuntimeException("You cannot transfer funds to your own account.");
        }

        boolean checkExistingAcNo = accountRepository.existsByAccountNo(toAccountNo);
        if (!checkExistingAcNo) {
            throw new RuntimeException("Recipient account number not found.");
        }

        Account toAccount = accountRepository.findByAcNoAndUsername(toAccountNo, toUsername)
                .orElseThrow(() -> new RuntimeException("The provided recipient username does not match the account number."));

        // Deduct from sender's account
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        accountRepository.save(fromAccount);

        // Add to recipient's account
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(toAccount);

        // Create transaction records for both accounts
        Transaction debitTransaction = new Transaction(
                amount,
                "Transfer Out to " + toAccount.getUsername(),
                LocalDateTime.now(),
                fromAccount
        );
        transactionRepository.save(debitTransaction);

        Transaction creditTransaction = new Transaction(
                amount,
                "Transfer In from " + fromAccount.getUsername(),
                LocalDateTime.now(),
                toAccount
        );
        transactionRepository.save(creditTransaction);
    }

}
