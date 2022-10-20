# Stable Coin (Java)

This repository contains StableCoin score in Java and instruction on how to deploy and run tests.


## Build
```sh
$ ./gradlew :stable-coin:build
```

## Optimized Jar
```sh
$ ./gradlew :stable-coin:optimizedJar
```

## Deploy
To deploy to mainnet or other testnet, change deployToSejong  to deployToMainnet, deployToLisbon or so on based on stable-coin/build.gradle. The parameters required to deploy the contract are listed on stable-coin/build.gradle as well.
```sh
$ ./gradlew stable-coin:deployToSejong -PkeystoreName=<your_wallet_json> -PkeystorePass=<password>
```

## Run unit tests
```sh
$ ./gradlew :stable-coin:test
```

## Run integration tests
```sh
$ ./gradlew stable-coin:integrationTest
```
