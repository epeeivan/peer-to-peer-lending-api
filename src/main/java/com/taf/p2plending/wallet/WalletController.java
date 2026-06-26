package com.taf.p2plending.wallet;

import com.taf.p2plending.wallet.dto.AmountRequest;
import com.taf.p2plending.wallet.dto.WalletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public WalletResponse balance(@PathVariable Long userId) {
        return walletService.getBalance(userId);
    }

    @PostMapping("/deposit")
    public WalletResponse deposit(@PathVariable Long userId, @Valid @RequestBody AmountRequest request) {
        return walletService.deposit(userId, request.amount());
    }

    @PostMapping("/withdraw")
    public WalletResponse withdraw(@PathVariable Long userId, @Valid @RequestBody AmountRequest request) {
        return walletService.withdraw(userId, request.amount());
    }
}
