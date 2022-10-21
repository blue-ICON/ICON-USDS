# Stable Coin (Java)

This repository contains StableCoin score in Java and instruction on how to deploy and run tests. 

## How to Run

### 1. Build the project

```
./gradlew build
```
The compiled jar bundle will be generated at `./stable-coin/build/libs/stable-coin-0.1.0.jar`

### 2. Optimize the jar

`gradle-javaee-plugin` is a Gradle plugin to automate the process of generating the optimized jar bundle.
Run the `optimizedJar` task to generate the optimized jar bundle.

```
./gradlew optimizedJar
```
The output jar will be located at `./stable-coin/build/libs/stable-coin-0.1.0-optimized.jar`

### 3. Deploy the optimized jar

Deploy using either of the following commands. The `build.gradle` in `stable-coin` has 4 endpoints included. 
To deploy on Berlin, run the following command. To deploy on other networks, for example, change `deployToBerlin` 
to `deployToLisbon`.

```sh
./gradlew stable-coin:deployToBerlin -PkeystoreName=<your_wallet_json> -PkeystorePass=<password>
```
```sh
./gradlew stable-coin:deployToBerlin -PkeystoreName='JavaTest.json' -PkeystorePass='p@ssw0rd'
```

### 4. Run unit test

```sh
./gradlew :stable-coin:test
```

### 5. Run integration test

To run integration test goloop instance is required. [gochain-local](https://github.com/icon-project/gochain-local)
repository has the tutorial to setup local blockchain.

After local node is running, run the following command to run integration tests.

```sh
./gradlew :stable-coin:integrationTest
```
