package com.bank.transactionsender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class TransactionSenderApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionSenderApplication.class, args);
    }

}
