package com.example.BankApp.controller;

import com.example.BankApp.model.Account;
import com.example.BankApp.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
public class BankController {

    @Autowired
    private AccountService accountService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.findAccountByUsername(username);
        model.addAttribute("account", account);
        return "dashboard";
    }

    @GetMapping("/")
    public String showHomePage(Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.findAccountByUsername(username);

        model.addAttribute("account", account);
        return "home"; // This matches dashboard.html
    }

    @GetMapping("/register")
    public String showRegistrationForm() {
        return "register";
    }

    @PostMapping("/register")
    public String registerAccount(@RequestParam String username,
                                  @RequestParam String password,
                                  RedirectAttributes redirectAttributes) {
        try {
            accountService.registerAccount(username, password);
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("registerError", e.getMessage());
            redirectAttributes.addFlashAttribute("regUsername", username);
            return "redirect:/register";
        }
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam BigDecimal amount, RedirectAttributes redirectAttributes) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.findAccountByUsername(username);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("depositError", "Amount must be greater than 0.");
            redirectAttributes.addFlashAttribute("depositAmount", amount);
            return "redirect:/dashboard";
        }

        if (amount.compareTo(new BigDecimal("100000")) > 0) {
            redirectAttributes.addFlashAttribute("depositError", "Amount too high. Please enter less than ₹100000.");
            redirectAttributes.addFlashAttribute("depositAmount", amount);
            return "redirect:/dashboard";
        }

        accountService.deposit(account, amount);
        redirectAttributes.addFlashAttribute("success", "Amount deposited successfully!");
        return "redirect:/dashboard";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam BigDecimal amount, RedirectAttributes redirectAttributes) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.findAccountByUsername(username);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("withdrawError", "Amount must be greater than 0.");
            redirectAttributes.addFlashAttribute("withdrawAmount", amount);
            return "redirect:/dashboard";
        }

        if (account.getBalance().compareTo(amount) < 0) {
            redirectAttributes.addFlashAttribute("withdrawError", "Insufficient balance.");
            redirectAttributes.addFlashAttribute("withdrawAmount", amount);
            return "redirect:/dashboard";
        }

        if (amount.compareTo(new BigDecimal("100000")) > 0) {
            redirectAttributes.addFlashAttribute("withdrawError", "Amount too high. Please enter less than ₹100000.");
            redirectAttributes.addFlashAttribute("withdrawAmount", amount);
            return "redirect:/dashboard";
        }

        try {
            accountService.withdraw(account, amount);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("withdrawError", e.getMessage());
            redirectAttributes.addFlashAttribute("withdrawAmount", amount);
            return "redirect:/dashboard";
        }

        redirectAttributes.addFlashAttribute("success", "Amount withdrawn successfully!");
        return "redirect:/dashboard";
    }

    @PostMapping("/transfer")
    public String transferAmount(@RequestParam String toUsername,
                                 @RequestParam Long toAccountNo,
                                 @RequestParam BigDecimal amount,
                                 RedirectAttributes redirectAttributes) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account fromAccount = accountService.findAccountByUsername(username);

        try {
            accountService.transferAmount(fromAccount, toAccountNo, amount, toUsername);
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("transferError", e.getMessage());
            redirectAttributes.addFlashAttribute("transferAmount", amount);
            redirectAttributes.addFlashAttribute("toUsername", toUsername);
            redirectAttributes.addFlashAttribute("toAccountNo", toAccountNo);
            return "redirect:/dashboard";
        }

        redirectAttributes.addFlashAttribute("success", "Transfer successful!");
        return "redirect:/dashboard";
    }

    @GetMapping("/transactions")
    public String transactionHistory(Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.findAccountByUsername(username);
        model.addAttribute("transactions", accountService.getTransactionHistory(account));
        return "transactions";
    }
}
