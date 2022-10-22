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


### 3. Update gradle.properties

Please generate 'gradle.properties' with the following parameters to deploy the USDS contract.

   ```
   keystoreName= path to deployer wallet
   keystorePass= deployer wallet password
   admin-address= address of admin wallet
   ```
Please add the following parameter to the `gradle.properties` file to update the existing contract.
   ```
   usds-address= contract address to update (for mainnet its cxbb2871f468a3008f80b08fdde5b8b951583acf06)
   ```


### 4. Deploy the optimized jar

Deploy using either of the following commands. The `build.gradle` in `stable-coin` has 4 endpoints included. 
To deploy on Berlin, run the following command. To deploy on other networks, for example, change `deployToBerlin` 
to `deployToLisbon`.

```sh
./gradlew stable-coin:deployToBerlin
```

### 5. Run unit test

```sh
./gradlew :stable-coin:test
```

### 6. Run integration test

To run integration test goloop instance is required. [gochain-local](https://github.com/icon-project/gochain-local)
repository has the tutorial to setup local blockchain.

After local node is running, run the following command to run integration tests.

```sh
./gradlew :stable-coin:integrationTest
```
