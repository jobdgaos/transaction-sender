package com.bank.transactionsender.web.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "PROVIDER-SERVICE", url = "http://mockoon.tools.getontop.com:3000")
public interface ProviderFeign {

    @RequestMapping(path = "/api/v1/payments", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    ResponseEntity<Map<String, Object>> paymentCreation(@RequestBody Map<String, Object> request);

    @RequestMapping(path = "/wallets/transactions", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    Map<String, Object> walletTransactions(@RequestBody Map<String, Object> request);

    @RequestMapping(path = "/wallets/balance", method = RequestMethod.GET)
    Map<String, Object> walletBalance(@RequestParam(value = "user_id") long userId);
}
