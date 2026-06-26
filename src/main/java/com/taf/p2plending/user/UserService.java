package com.taf.p2plending.user;

import com.taf.p2plending.common.exception.EmailAlreadyUsedException;
import com.taf.p2plending.user.dto.CreateUserRequest;
import com.taf.p2plending.user.dto.UserResponse;
import com.taf.p2plending.wallet.Wallet;
import com.taf.p2plending.wallet.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final WalletRepository wallets;

    public UserService(UserRepository users, WalletRepository wallets) {
        this.users = users;
        this.wallets = wallets;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (users.existsByEmail(request.email())) {
            throw new EmailAlreadyUsedException(request.email());
        }
        User user = users.save(new User(request.name(), request.email()));
        Wallet wallet = wallets.save(new Wallet(user.getId()));
        return new UserResponse(user.getId(), user.getName(), user.getEmail(),
                wallet.getId(), wallet.getBalance());
    }
}
