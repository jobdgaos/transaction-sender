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
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.*;

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
        Map<String, Object> transactionRequest = new HashMap<>();

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
                                //Set source transaction data
                                transactionRequest.put(Constants.PAYMENT_SOURCE, response.getData());

                                double feeRate = bankService.getFeeRate();
                                if (feeRate > 0) {
                                    response = walletService.sendTransaction(destinationUser, (int) (amount * (1 - feeRate)));
                                    logger.info("destination transaction: {}", response);
                                } else {
                                    response.setSuccess(false);
                                    response.setMessage("failed to get fee rate");
                                }

                                //If the second transaction failed, the first its reversed
                                if (!response.getSuccess()) {
                                    response = walletService.sendTransaction(sourceUser, amount);
                                    logger.info("reverse transaction: {}", response);

                                    //Set reverse transaction data
                                    transactionRequest.put(Constants.TRANSACTION_REVERSE, response.getData());
                                } else {
                                    //Set destination transaction data
                                    transactionRequest.put(Constants.PAYMENT_DESTINATION, response.getData());
                                }
                            }

                            transactionRequest.put(Constants.TRANSACTION_PAYMENT, this.getPaymentData(paymentResponse.getBody()));

                            logger.info("transactionRequest: {}", transactionRequest);

                            mongoTemplate.save(transactionRequest, Constants.TRANSACTION_COLLECTION);
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

        return new ResponseEntity("Payment completed successfully", HttpStatus.OK);
    }

    public BaseResponse validateRequest(Map<String, Object> request) {
        BaseResponse response = new BaseResponse();
        List<String> errorDetail = new ArrayList<>();

        if (!request.containsKey(Constants.PAYMENT_SOURCE)) {
            errorDetail.add(Constants.PAYMENT_SOURCE + " is mandatory.");
        }
        if (!request.containsKey(Constants.PAYMENT_DESTINATION)) {
            errorDetail.add(Constants.PAYMENT_DESTINATION + " is mandatory.");
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

    public Map<String, Object> getPaymentData(Map<String, Object> paymentResponse) {
        Map<String, Object> paymentInfo = new HashMap<>((Map) paymentResponse.get(Constants.TRANSACTION_PAYMENT_INFO));
        Map<String, Object> requestInfo = new HashMap<>((Map) paymentResponse.get(Constants.TRANSACTION_REQUEST_INFO));

        paymentInfo.put("status", requestInfo.get("status"));
        paymentInfo.put("creationDate", new Date());

        return paymentInfo;
    }

    public void cleanRequest(Map<String, Object> request) {
        request.remove(Constants.BANK_IDENTIFIER);
        request.remove(Constants.USER_IDENTIFIER);
    }

    public ResponseEntity<BaseResponse> getTransactionList(Optional<Date> minDate, Optional<Date> maxDate,
                                                           Optional<Integer> minAmount, Optional<Integer> maxAmount,
                                                           Optional<Integer> currentPage, Optional<Integer> pageSize) {
        ResponseEntity responseEntity;
        BaseResponse response = new BaseResponse();
        Query query;
        List<Criteria> criteriaList = new ArrayList<>();
        Map<String, Object> responseList = new HashMap<>();

        if (pageSize.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("Page size parameter is mandatory");
            responseEntity = new ResponseEntity(response, HttpStatus.BAD_REQUEST);
        } else {

            if (minAmount.isPresent()) {
                criteriaList.add(Criteria.where(Constants.TRANSACTION_AMOUNT_PATH).gte(minAmount.get()));
            }

            if (maxAmount.isPresent()) {
                criteriaList.add(Criteria.where(Constants.TRANSACTION_AMOUNT_PATH).lte(maxAmount.get()));
            }

            if (minDate.isPresent()) {
                criteriaList.add(Criteria.where(Constants.TRANSACTION_DATE_PATH).gte(minDate.get()));
            }

            if (maxDate.isPresent()) {
                criteriaList.add(Criteria.where(Constants.TRANSACTION_DATE_PATH).lte(maxDate.get()));
            }


            if (!criteriaList.isEmpty()) {
                Criteria criteria = new Criteria().andOperator(criteriaList);
                query = new Query(criteria);
            } else {
                query = new Query();
            }

            query.fields().exclude("_id");


            Long transactionCount = mongoTemplate.count(query, Constants.TRANSACTION_COLLECTION);

            int maxPages = (int) Math.ceil((transactionCount / pageSize.get()) + 0.5);

            if (currentPage.isEmpty() || currentPage.get() <= 0) {
                currentPage = Optional.of(1);
            }

            if (currentPage.get() > maxPages) {
                currentPage = Optional.of(maxPages);
            }

            query.skip((currentPage.get() - 1) * pageSize.get());
            query.limit(pageSize.get());

            query.with(Sort.by(Sort.Direction.DESC, Constants.TRANSACTION_DATE_PATH));

            List<Map> transactionList = mongoTemplate.find(query, Map.class, Constants.TRANSACTION_COLLECTION);

            logger.info("Count: {}", transactionCount);
            logger.info("maxPage: {}", maxPages);
            logger.info("currentPage: {}", currentPage.get());
            logger.info("pageListCount: {}", transactionList.size());

            responseList.put("total", transactionCount);
            responseList.put("maxPages", maxPages);
            responseList.put("currentPage", currentPage.get());
            responseList.put("pageList", transactionList);


            response.setSuccess(true);
            response.setMessage("Transaction list");
            response.setData(responseList);
            responseEntity = new ResponseEntity(response, HttpStatus.OK);
        }

        return responseEntity;
    }
}
