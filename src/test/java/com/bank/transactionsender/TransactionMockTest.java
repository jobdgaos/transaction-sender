package com.bank.transactionsender;

import com.bank.transactionsender.service.TransactionService;
import com.bank.transactionsender.web.controller.TransactionController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
public class TransactionMockTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;
    @Test
    public void creatingPaymentShouldReturnSuccessMessage() throws Exception {
        String urlPath = "/transaction/transfer";

        this.mockMvc.perform(post(urlPath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getPaymentCorrectData()))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void creatingPaymentShouldFailedErrorMessage() throws Exception {
        String urlPath = "/transaction/transfer";

        Class<Map<String,Object>> clazz = (Class<Map<String,Object>>)(Class)Map.class;

        when(transactionService.createPayment(any(clazz))).thenReturn(new ResponseEntity<>("Failed", HttpStatus.INTERNAL_SERVER_ERROR));

        this.mockMvc.perform(post(urlPath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getPaymentFailedData()))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }
    @Test
    public void creatingPaymentShouldTimeoutErrorMessage() throws Exception {
        String urlPath = "/transaction/transfer";

        Class<Map<String,Object>> clazz = (Class<Map<String,Object>>)(Class)Map.class;

        when(transactionService.createPayment(any(clazz))).thenReturn(new ResponseEntity<>("Timeout", HttpStatus.INTERNAL_SERVER_ERROR));

        this.mockMvc.perform(post(urlPath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getPaymentTimeoutData()))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void gettingTransactionListWithSuccessMessage() throws Exception {
        String urlPath = "/transaction/search";

        //Class<Map<String,Object>> clazz = (Class<Map<String,Object>>)(Class)Map.class;

        //when(transactionService.createPayment(any(clazz))).thenReturn(new ResponseEntity("Timeout", HttpStatus.INTERNAL_SERVER_ERROR));

        this.mockMvc.perform(get(urlPath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("pageSize", "5"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void gettingTransactionListWithBadRequestMessage() throws Exception {
        String urlPath = "/transaction/search";

        when(transactionService.getTransactionList(
                any(Optional.class), any(Optional.class), any(Optional.class),
                any(Optional.class), any(Optional.class), any(Optional.class)))
                .thenReturn(new ResponseEntity("Bad request", HttpStatus.BAD_REQUEST));

        this.mockMvc.perform(get(urlPath)
                        .contentType(MediaType.APPLICATION_JSON)
                        )
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    public static String getPaymentCorrectData(){
        return """
                {
                    "source": {
                        "type": "COMPANY",
                        "sourceInformation": {
                            "name": "ONTOP INC"
                        },
                        "account": {
                            "accountNumber": "0245253419",
                            "currency": "USD",
                            "routingNumber": "028444018"
                        }
                    },
                    "destination": {
                        "name": "TONY STARK",
                        "account": {
                            "accountNumber": "1885226711",
                            "currency": "USD",
                            "routingNumber": "211927207"
                        }
                    },
                    "amount": 1000
                }""";
    }

    public static String getPaymentFailedData(){
        return """
                {
                    "source": {
                        "type": "COMPANY",
                        "sourceInformation": {
                            "name": "ONTOP INC"
                        },
                        "account": {
                            "accountNumber": "0245253419",
                            "currency": "USD",
                            "routingNumber": "028444018"
                        }
                    },
                    "destination": {
                        "name": "JAMES FAILED",
                        "account": {
                            "accountNumber": "1885226711",
                            "currency": "USD",
                            "routingNumber": "211927207"
                        }
                    },
                    "amount": 1000
                }""";
    }

    public static String getPaymentTimeoutData() {
        return """
                {
                    "source": {
                        "type": "COMPANY",
                        "sourceInformation": {
                            "name": "ONTOP INC"
                        },
                        "account": {
                            "accountNumber": "0245253419",
                            "currency": "USD",
                            "routingNumber": "028444018"
                        }
                    },
                    "destination": {
                        "name": "JAMES TIMEOUT",
                        "account": {
                            "accountNumber": "1885226711",
                            "currency": "USD",
                            "routingNumber": "211927207"
                        }
                    },
                    "amount": 1000
                }""";
    }
}
