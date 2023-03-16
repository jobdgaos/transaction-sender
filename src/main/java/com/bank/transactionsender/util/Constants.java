package com.bank.transactionsender.util;

public class Constants {
    //MongoDB
    public final static String USER_COLLECTION = "users";
    public final static String BANK_COLLECTION = "bank";
    public final static String TRANSACTION_COLLECTION = "transactions";
    public final static String CATALOG_COLLECTION = "catalog";
    public final static String KEY_USER_SEQUENCE = "userSequence";
    public final static String KEY_BANK_SEQUENCE = "bankSequence";
    public final static String VALUE_SEQUENCE = "value";
    public final static String BANK_ACCOUNT_NUMBER_PATH = "account.accountNumber";
    public final static String BANK_DESTINATION_IDENTIFIER_PATH = "$.destination.bankId";
    public final static String BANK_SOURCE_IDENTIFIER_PATH = "$.source.bankId";

    //User
    public final static String USER_IDENTIFIER = "userId";
    public final static String USER_NATIONAL_ID = "nationalId";
    public final static String USER_FIRST_NAME = "firstName";
    public final static String USER_LAST_NAME = "lastName";
    public final static String USER_BALANCE = "balance";

    //Bank
    public final static String BANK_IDENTIFIER = "bankId";
    public final static String BANK_TYPE = "type";
    public final static String BANK_ACCOUNT_NAME = "name";
    public final static String BANK_ROUTING_NUMBER = "routingNumber";
    public final static String BANK_ACCOUNT_NUMBER = "accountNumber";
    public final static String BANK_ACCOUNT_CURRENCY = "currency";
    public final static String BANK_SOURCE_INFORMATION = "sourceInformation";
    public final static String BANK_TRANSFER_RATE = "feeRate";

    //Payment
    public final static String PAYMENT_SOURCE = "source";
    public final static String PAYMENT_DESTINATION = "destination";
    public final static String PAYMENT_AMOUNT = "amount";
    public final static String PAYMENT_USER_IDENTIFIER = "user_id";

    //Transaction
    public final static String TRANSACTION_PAYMENT = "payment";
    public final static String TRANSACTION_PAYMENT_INFO = "paymentInfo";
    public final static String TRANSACTION_REQUEST_INFO = "requestInfo";
    public final static String TRANSACTION_REVERSE = "reverse";
    public final static String TRANSACTION_AMOUNT_PATH = "payment.amount";
    public final static String TRANSACTION_DATE_PATH = "payment.creationDate";
    //Server
    public final static String SERVER_ERROR_MESSAGE_PATH = "$.requestInfo.error";
    public final static String SERVER_MESSAGE_PATH = "$.message";
}
