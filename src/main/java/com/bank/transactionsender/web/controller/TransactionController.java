package com.bank.transactionsender.web.controller;

import com.bank.transactionsender.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(value = "/process")
public class TransactionController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @RequestMapping(value = "/transfer", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity processTransfer(@RequestBody Map<String, Object> request) {

        return transactionService.createPayment(request);
    }


}
