package com.bank.transactionsender.web.controller;

import com.bank.transactionsender.service.BankService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(value = "/bank")
public class BankController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    @RequestMapping(value = "/save", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity addNewBank(@RequestBody Map<String, Object> request) {
        return bankService.createNewBank(request);
    }
}
