package com.bank.transactionsender.service;

import com.bank.transactionsender.model.BaseResponse;
import com.bank.transactionsender.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class UserService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final MongoTemplate mongoTemplate;

    public UserService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public BaseResponse createUser(Map<String, Object> request) {
        BaseResponse response = new BaseResponse();
        Map dbResponse;
        Map<String, Object> userRequest = new HashMap<>(request);
        try {
            String nationalId = String.valueOf(userRequest.getOrDefault(Constants.USER_NATIONAL_ID, ""));

            CriteriaDefinition criteria = new Criteria().andOperator(
                    Criteria.where(Constants.USER_NATIONAL_ID).is(nationalId));

            Query query = new Query(criteria);
            query.fields().exclude("_id");
            query.fields().include(Constants.USER_IDENTIFIER);

            dbResponse = mongoTemplate.findOne(query, Map.class, Constants.USER_COLLECTION);

            //If user does not exist
            if (dbResponse == null) {
                this.cleanUserRequest(userRequest);
                userRequest.put("userId", this.newSequence());
                dbResponse = mongoTemplate.save(userRequest, Constants.USER_COLLECTION);
            }

            response.setSuccess(true);
            response.setMessage("User created");
            response.setData(dbResponse);
        } catch (Exception ex) {
            logger.error("Exception", ex);
            response.setSuccess(false);
            response.setMessage("User not created");
        }

        return response;
    }

    public Long newSequence() {
        try {
            CriteriaDefinition criteria = new Criteria().andOperator(
                    Criteria.where("_id").is(Constants.KEY_USER_SEQUENCE));

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

    public void cleanUserRequest(Map<String, Object> request) {
        request.remove(Constants.BANK_ACCOUNT_NUMBER);
        request.remove(Constants.BANK_ROUTING_NUMBER);
        request.remove(Constants.BANK_ACCOUNT_CURRENCY);
        request.remove(Constants.BANK_TYPE);
        request.remove(Constants.BANK_SOURCE_INFORMATION);
    }
}
