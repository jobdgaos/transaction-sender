package com.bank.transactionsender.service;

import com.bank.transactionsender.model.BaseResponse;
import com.bank.transactionsender.util.Constants;
import com.bank.transactionsender.web.feign.ProviderFeign;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TransactionService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final MongoTemplate mongoTemplate;

    private final BankService bankService;

    private final WalletService walletService;
    private final ProviderFeign providerFeign;

    public TransactionService(MongoTemplate mongoTemplate, BankService bankService, WalletService walletService, ProviderFeign providerFeign) {
        this.mongoTemplate = mongoTemplate;
        this.bankService = bankService;
        this.walletService = walletService;
        this.providerFeign = providerFeign;
    }

    public ResponseEntity createPayment(Map<String, Object> request) {
        BaseResponse response;
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            response = validateRequest(request);

            //Request not valid
            if (!response.getSuccess()) {
                return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
            }

            Map destinationBank = bankService.findBankById(Constants.BANK_DESTINATION_IDENTIFIER_PATH, request);

            if (destinationBank == null) {
                return new ResponseEntity("Destination bank not found", HttpStatus.NOT_FOUND);
            } else {
                Map sourceBank = bankService.findBankById(Constants.BANK_SOURCE_IDENTIFIER_PATH, request);

                if (sourceBank == null) {
                    return new ResponseEntity("Source bank not found", HttpStatus.NOT_FOUND);
                } else {
                    long sourceUser = (long) sourceBank.get(Constants.USER_IDENTIFIER);
                    long destinationUser = (long) destinationBank.get(Constants.USER_IDENTIFIER);

                    response = walletService.checkBalance(sourceUser);

                    if (!response.getSuccess()) {
                        return new ResponseEntity(response.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
                    } else {
                        int balance = (int) response.getData().get(Constants.USER_BALANCE);
                        int amount = (int) request.get(Constants.PAYMENT_AMOUNT);
                        if (amount > balance) {
                            return new ResponseEntity("Insufficient funds", HttpStatus.NOT_FOUND);
                        } else {
                            cleanRequest(sourceBank);
                            cleanRequest(destinationBank);

                            request.put("source", sourceBank);
                            request.put("destination", destinationBank);

                            logger.info("Transfer: {}", objectMapper.writeValueAsString(request));
                            ResponseEntity<Map<String, Object>> paymentResponse = providerFeign.paymentCreation(request);

                            response = walletService.sendTransaction(sourceUser, -amount);
                            logger.info("source transaction: {}", response);

                            if (response.getSuccess()) {
                                double feeRate = bankService.getFeeRate();
                                if(feeRate > 0) {
                                    response = walletService.sendTransaction(destinationUser, (int) (amount * (1-feeRate)));
                                    logger.info("destination transaction: {}", response);
                                } else{
                                    response.setSuccess(false);
                                    response.setMessage("failed to get fee rate");
                                }

                                //If the second transaction failed, the first its reversed
                                if (!response.getSuccess()) {
                                    response = walletService.sendTransaction(sourceUser, amount);
                                    logger.info("reverse transaction: {}", response);
                                }
                            }
                            //Save all the data
                            logger.info("paymentResponse: {}", paymentResponse);
                        }
                    }
                }
            }

        } catch (FeignException fex) {
            logger.error("Status: {}", fex.status());
            logger.error("Content: {}", fex.contentUTF8());

            if (fex.status() == HttpStatus.BAD_REQUEST.value()) {
                return new ResponseEntity("Payment not created. Body is invalid.", HttpStatus.BAD_REQUEST);
            } else if (fex.status() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                Configuration configuration = Configuration.builder().options(Option.SUPPRESS_EXCEPTIONS).mappingProvider(new JacksonMappingProvider()).build();

                String errorMessage = JsonPath.using(configuration).parse(fex.contentUTF8()).read(Constants.SERVER_ERROR_MESSAGE_PATH, String.class);
                return new ResponseEntity("Payment not created. " + errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception ex) {
            logger.info("Exception", ex);
            return new ResponseEntity("Payment not created", HttpStatus.INTERNAL_SERVER_ERROR);

        }

        return new ResponseEntity("Payment created successfully", HttpStatus.OK);
    }

    public BaseResponse validateRequest(Map<String, Object> request) {
        BaseResponse response = new BaseResponse();
        List<String> errorDetail = new ArrayList<>();

        if (!request.containsKey(Constants.PAYMENT_SOURCE)) {
            errorDetail.add(Constants.PAYMENT_SOURCE + " is mandatory.");
        }
        if (!request.containsKey(Constants.PAYMENT_destination)) {
            errorDetail.add(Constants.PAYMENT_destination + " is mandatory.");
        }
        if (!request.containsKey(Constants.PAYMENT_AMOUNT)) {
            errorDetail.add(Constants.PAYMENT_AMOUNT + " is mandatory.");
        }

        if (!errorDetail.isEmpty()) {
            Map<String, Object> data = new HashMap<>();
            data.put("errorList", errorDetail);
            response.setSuccess(false);
            response.setMessage("Data is missing");
            response.setData(data);
        } else {
            response.setSuccess(true);
            response.setMessage("Data is complete");
        }

        return response;
    }

    public void cleanRequest(Map<String, Object> request) {
        request.remove(Constants.BANK_IDENTIFIER);
        request.remove(Constants.USER_IDENTIFIER);
    }
}
