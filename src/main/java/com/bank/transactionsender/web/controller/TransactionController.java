package com.bank.transactionsender.web.controller;

import com.bank.transactionsender.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(value = "/transaction")
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

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public ResponseEntity searchTransaction(@RequestParam("minDate") Optional<Date> minDate, @RequestParam("maxDate") Optional<Date> maxDate,
                                            @RequestParam("minAmount") Optional<Integer> minAmount, @RequestParam("maxAmount") Optional<Integer> maxAmount,
                                            @RequestParam("currentPage") Optional<Integer> currentPage, @RequestParam("pageSize") Optional<Integer> pageSize) {

        return transactionService.getTransactionList(minDate, maxDate, minAmount, maxAmount, currentPage, pageSize);
    }


}
