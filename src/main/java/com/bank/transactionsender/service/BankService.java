package com.bank.transactionsender.service;

import com.bank.transactionsender.model.BaseResponse;
import com.bank.transactionsender.util.Constants;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BankService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final MongoTemplate mongoTemplate;
    private final UserService userService;

    public BankService(MongoTemplate mongoTemplate, UserService userService) {
        this.mongoTemplate = mongoTemplate;
        this.userService = userService;
    }

    public ResponseEntity createNewBank(Map<String, Object> request) {
        BaseResponse response;
        try {
            response = validateRequest(request);

            //Request not valid
            if (!response.getSuccess()) {
                return new ResponseEntity(response, HttpStatus.BAD_REQUEST);
            }

            response = userService.createUser(request);

            //If user is not created
            if (!response.getSuccess()) {
                return new ResponseEntity(response, HttpStatus.INTERNAL_SERVER_ERROR);
            } else {
                //If bank doesn't exist
                if (!this.existBankByAccountNumber(request)) {
                    request.put(Constants.USER_IDENTIFIER, response.getData().get(Constants.USER_IDENTIFIER));
                    Map<String, Object> bankData = this.getBankData(request);
                    mongoTemplate.save(bankData, Constants.BANK_COLLECTION);
                }

                return new ResponseEntity("Bank created", HttpStatus.OK);
            }

        } catch (Exception ex) {
            logger.info("Exception", ex);
            return new ResponseEntity("Bank not created", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public BaseResponse validateRequest(Map<String, Object> request) {
        BaseResponse response = new BaseResponse();
        List<String> errorDetail = new ArrayList<>();

        if (!request.containsKey(Constants.USER_NATIONAL_ID)) {
            errorDetail.add(Constants.USER_NATIONAL_ID + " is mandatory.");
        }
        if (!request.containsKey(Constants.USER_FIRST_NAME)) {
            errorDetail.add(Constants.USER_FIRST_NAME + " is mandatory.");
        }
        if (!request.containsKey(Constants.USER_LAST_NAME)) {
            errorDetail.add(Constants.USER_LAST_NAME + " is mandatory.");
        }
        if (!request.containsKey(Constants.BANK_ROUTING_NUMBER)) {
            errorDetail.add(Constants.BANK_ROUTING_NUMBER + " is mandatory.");
        }
        if (!request.containsKey(Constants.BANK_ACCOUNT_NUMBER)) {
            errorDetail.add(Constants.BANK_ACCOUNT_NUMBER + " is mandatory.");
        }
        if (!request.containsKey(Constants.BANK_ACCOUNT_CURRENCY)) {
            errorDetail.add(Constants.BANK_ACCOUNT_CURRENCY + " is mandatory.");
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

    public boolean existBankByAccountNumber(Map<String, Object> request) {
        String accountNumber = String.valueOf(request.getOrDefault(Constants.BANK_ACCOUNT_NUMBER, ""));

        CriteriaDefinition criteria = new Criteria().andOperator(
                Criteria.where(Constants.BANK_ACCOUNT_NUMBER_PATH).is(accountNumber));

        Query query = new Query(criteria);
        query.fields().exclude("_id");
        query.fields().include(Constants.USER_IDENTIFIER);

        Map dbResponse = mongoTemplate.findOne(query, Map.class, Constants.BANK_COLLECTION);

        return dbResponse == null ? false : true;
    }

    public Map<String, Object> findBankById(String searchField, Map<String, Object> request) {
        Configuration configuration = Configuration.builder().options(Option.SUPPRESS_EXCEPTIONS).mappingProvider(new JacksonMappingProvider()).build();

        long bankId = JsonPath.using(configuration).parse(request).read(searchField, Long.class);

        CriteriaDefinition criteria = new Criteria().andOperator(
                Criteria.where(Constants.BANK_IDENTIFIER).is(bankId));

        Query query = new Query(criteria);
        query.fields().exclude("_id");

        Map dbResponse = mongoTemplate.findOne(query, Map.class, Constants.BANK_COLLECTION);

        return dbResponse;
    }

    public Map<String, Object> getBankData(Map<String, Object> request) {
        Map<String, Object> bankData = new HashMap<>();
        Map<String, Object> accountData = new HashMap<>();

        String fullName = request.get(Constants.USER_FIRST_NAME) + " " + request.get(Constants.USER_LAST_NAME);

        accountData.put(Constants.BANK_ACCOUNT_NUMBER, request.get(Constants.BANK_ACCOUNT_NUMBER));
        accountData.put(Constants.BANK_ACCOUNT_CURRENCY, request.get(Constants.BANK_ACCOUNT_CURRENCY));
        accountData.put(Constants.BANK_ROUTING_NUMBER, request.get(Constants.BANK_ROUTING_NUMBER));

        if (request.get(Constants.BANK_TYPE) != null) {
            bankData.put(Constants.BANK_TYPE, request.get(Constants.BANK_TYPE));
        }

        if (request.get(Constants.BANK_SOURCE_INFORMATION) != null) {
            bankData.put(Constants.BANK_SOURCE_INFORMATION, request.get(Constants.BANK_SOURCE_INFORMATION));
        }

        bankData.put(Constants.BANK_IDENTIFIER, this.newSequence());
        bankData.put(Constants.USER_IDENTIFIER, request.get(Constants.USER_IDENTIFIER));
        bankData.put(Constants.BANK_ACCOUNT_NAME, fullName);
        bankData.put("account", accountData);

        return bankData;
    }

    public Long newSequence() {
        try {
            CriteriaDefinition criteria = new Criteria().andOperator(
                    Criteria.where("_id").is(Constants.KEY_BANK_SEQUENCE));

            Query query = new Query(criteria);
            query.fields().include(Constants.VALUE_SEQUENCE);

            Update newUpdate = new Update().inc(Constants.VALUE_SEQUENCE, 1);
            FindAndModifyOptions newOptions = new FindAndModifyOptions().returnNew(true).upsert(true);
            Map response = mongoTemplate.findAndModify(query, newUpdate, newOptions, Map.class, Constants.CATALOG_COLLECTION);

            return Long.valueOf(response.get(Constants.VALUE_SEQUENCE).toString());
        } catch (Exception ex) {
            logger.info("Exception: ", ex);
            return -1L;
        }
    }
}
