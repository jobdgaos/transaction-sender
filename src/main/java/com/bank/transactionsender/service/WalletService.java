package com.bank.transactionsender.service;

import com.bank.transactionsender.model.BaseResponse;
import com.bank.transactionsender.util.Constants;
import com.bank.transactionsender.web.feign.ProviderFeign;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class WalletService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final MongoTemplate mongoTemplate;
    private final ProviderFeign providerFeign;

    public WalletService(MongoTemplate mongoTemplate, ProviderFeign providerFeign) {
        this.mongoTemplate = mongoTemplate;
        this.providerFeign = providerFeign;
    }

    public BaseResponse checkBalance(long userId) {
        BaseResponse response = new BaseResponse();
        try {
            Map<String, Object> balanceResponse = this.providerFeign.walletBalance(userId);

            response.setSuccess(true);
            response.setMessage("Balance obtained");
            response.setData(balanceResponse);

        } catch (FeignException fex) {
            response.setSuccess(false);
            response.setMessage("Code: " + fex.status() + " - Error while getting user balance");
        } catch (Exception ex) {
            response.setSuccess(false);
            response.setMessage("Error while getting user balance");
        }
        return response;
    }

    public BaseResponse sendTransaction(long userId, int amount) {
        BaseResponse response = new BaseResponse();
        try {
            Map<String, Object> transactionRequest = new HashMap<>();
            transactionRequest.put(Constants.PAYMENT_AMOUNT, amount);
            transactionRequest.put(Constants.PAYMENT_USER_IDENTIFIER, userId);

            Map<String, Object> transactionResponse = this.providerFeign.walletTransactions(transactionRequest);

            response.setSuccess(true);
            response.setMessage("EWallet transaction completed");
            response.setData(transactionResponse);

        } catch (FeignException fex) {
            Configuration configuration = Configuration.builder().options(Option.SUPPRESS_EXCEPTIONS).mappingProvider(new JacksonMappingProvider()).build();
            String errorMessage = JsonPath.using(configuration).parse(fex.contentUTF8()).read(Constants.SERVER_MESSAGE_PATH, String.class);

            response.setSuccess(false);
            response.setMessage("Code: " + fex.status() + " - " + errorMessage);
        } catch (Exception ex) {
            response.setSuccess(false);
            response.setMessage("Error while getting user balance");
        }
        return response;
    }
}
