db = db.getSiblingDB('ewallet');

db.createUser(
  {
    user: 'transaction_user',
    pwd: 'password',
    roles: [{ role: 'readWrite', db: 'ewallet' }],
  },
);

db.createCollection('transactions');

db.createCollection('bank');
db.bank.createIndex({"account.accountNumber":1},{"name": "idxAccountNumber"});
db.bank.createIndex({"bankid":1},{"name": "idxBankId"});

db.createCollection('users');
db.users.createIndex({"nationalId":1},{"name": "idxNationalId"});

db.createCollection('catalog');
db.catalog.insert({"_id": "feeRate", "value": 0.1});