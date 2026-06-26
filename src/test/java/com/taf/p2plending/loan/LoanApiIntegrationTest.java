package com.taf.p2plending.loan;

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
class LoanApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserService userService;

    @Test
    void request_loan_returns_201_pending_with_funding_deadline() throws Exception {
        Long borrowerId = userService.createUser(
                new CreateUserRequest("Borrower", "borrower.loan@example.com")).id();

        String created = mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"borrowerId":%d,"principal":1000.00,"annualInterestRate":0.12,"termMonths":12}
                                """.formatted(borrowerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.fundedAmount").value(0))
                .andExpect(jsonPath("$.fundingDeadline").exists())
                .andReturn().getResponse().getContentAsString();

        Long loanId = com.jayway.jsonpath.JsonPath.parse(created).read("$.id", Long.class);
        mvc.perform(get("/api/loans/{id}", loanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.principal").value(1000.00));
    }

    @Test
    void request_loan_for_unknown_borrower_returns_404() throws Exception {
        mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"borrowerId":999999,"principal":1000.00,"annualInterestRate":0.12,"termMonths":12}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void request_loan_with_invalid_body_returns_400() throws Exception {
        Long borrowerId = userService.createUser(
                new CreateUserRequest("Borrower2", "borrower2.loan@example.com")).id();

        mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"borrowerId":%d,"principal":-5.00,"annualInterestRate":0.12,"termMonths":0}
                                """.formatted(borrowerId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_unknown_loan_returns_404() throws Exception {
        mvc.perform(get("/api/loans/{id}", 888888))
                .andExpect(status().isNotFound());
    }
}
