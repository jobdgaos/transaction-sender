# transaction-sender
Middleware to process wallet transfer transactions

## About the service
The service is an interface to complete wallet transactions to banks, it allows the user to store bank information to make transfers more easily. All the generated data is stored and retrieved from MongoDB.

## Requirements
To build and run the application it's needed:

- [JDK 17](https://www.oracle.com/java/technologies/downloads/#java17)
- [Maven 3](https://maven.apache.org)
- [Docker Compose](https://docs.docker.com/compose/install/)

## How to run
* Clone this repository

### Set up a mock server
* Postman enables the mock server creation [Setting up mock servers](https://learning.postman.com/docs/designing-and-developing-your-api/mocking-data/setting-up-mock/)
* Create a new mock from the collection in ```mocks``` folder 

### Start containers
* Run all containers with ```docker-compose -f compose/docker-compose.yml up -d```

### Start service
* Test can run using ```mvn clean test```
* Run the service using ```mvn spring-boot:run```

Once the application is running it'll look something like this
```
o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 6106 (http) with context path ''
c.b.t.TransactionSenderApplication       : Started TransactionSenderApplication in 2.654 seconds (process running for 3.194)
```

## API examples

### Create source bank
```
POST /bank/save
Accept: application/json
Content-Type: application/json

{
    "nationalId": "1234567",
    "firstName": "Mark",
    "lastName": "Marcus",
    "routingNumber": "028444018",
    "accountNumber": "0245253419",
    "currency": "USD",
    "type": "COMPANY",
    "sourceInformation": {
        "name": "Company INC"
    }
}

RESPONSE: HTTP 200 (OK)
```

### Create destination bank
```
POST /bank/save
Accept: application/json
Content-Type: application/json

{
    "nationalId": "654321224",
    "firstName": "Pete",
    "lastName": "Perks",
    "routingNumber": "211927207224",
    "accountNumber": "1885226711224",
    "currency": "USD"
}

RESPONSE: HTTP 200 (OK)
```

### Create payment
```
POST /transaction/transfer
Accept: application/json
Content-Type: application/json

{
    "source": {
        "bankId": 1
    },
    "destination": {
        "bankId": 2
    },
    "amount": 50
}

RESPONSE: HTTP 200 (OK)
```

### Retrieve a paginated list of transactions
```
GET /transaction/search?pageSize=5
Accept: application/json
Content-Type: application/json

RESPONSE: HTTP 200 (OK)
```