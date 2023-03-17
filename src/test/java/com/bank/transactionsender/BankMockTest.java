package com.bank.transactionsender;

import com.bank.transactionsender.service.BankService;
import com.bank.transactionsender.web.controller.BankController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BankController.class)
public class BankMockTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BankService bankService;
    @Test
    public void creatingBankShouldReturnSuccessMessage() throws Exception {
        String urlPath = "/bank/save";

        this.mockMvc.perform(post(urlPath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getBankData()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    public static String getBankData() {
        return """
                {
                    "nationalId": "123456789",
                    "firstName": "John",
                    "lastName": "Doe",
                    "routingNumber": "123456789",
                    "accountNumber": "123456789",
                    "currency": "USD"
                }""";
    }
}
