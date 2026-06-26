package com.taf.p2plending.wallet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taf.p2plending.support.AbstractIntegrationTest;
import com.taf.p2plending.user.UserService;
import com.taf.p2plending.user.dto.CreateUserRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class WalletApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserService userService;

    @Test
    void create_user_returns_201_with_wallet() throws Exception {
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice\",\"email\":\"alice.api@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.walletId").exists())
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void create_user_with_invalid_email_returns_400() throws Exception {
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deposit_returns_200_and_updates_balance() throws Exception {
        Long userId = userService.createUser(new CreateUserRequest("Bob", "bob.api@example.com")).id();

        mvc.perform(post("/api/users/{id}/wallet/deposit", userId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":500.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500.00));

        mvc.perform(get("/api/users/{id}/wallet", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500.00));
    }

    @Test
    void withdraw_over_balance_returns_409() throws Exception {
        Long userId = userService.createUser(new CreateUserRequest("Carl", "carl.api@example.com")).id();
        mvc.perform(post("/api/users/{id}/wallet/deposit", userId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":100.00}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/users/{id}/wallet/withdraw", userId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":200.00}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deposit_with_negative_amount_returns_400() throws Exception {
        Long userId = userService.createUser(new CreateUserRequest("Dave", "dave.api@example.com")).id();

        mvc.perform(post("/api/users/{id}/wallet/deposit", userId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":-5.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wallet_for_unknown_user_returns_404() throws Exception {
        mvc.perform(get("/api/users/{id}/wallet", 999999))
                .andExpect(status().isNotFound());
    }
}
