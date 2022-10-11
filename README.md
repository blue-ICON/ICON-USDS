# Java SCORE Examples

This repository contains template of a java SCORE and instructions on how to deploy a simple java score.


## How to Run
### A. IntelliJ IDEA
If you are using Intellij IDEA, then you don't need to manually run the following steps.

- First, download gradle dependencies. When you open the project in intellij, 
there will be an option to do so. You might need to click the elephant refresh button, 
which appears on top right after download.
- On doing this, a small blue square will be seen on the hello-world folder structure.
- Gradle tab will now be visible on the left side. Click the gradle tab.
- Expand `hello-world` by clicking it.
- Expand build and deployment.
- Double-click on build cogwheel to build the project.
- Double-click on optimizedJar to build optimized jar.
- Give absolute path of keystore file in gradle.properties.
```shell
keystoreName=/home/username/Java-Score-Template/JavTest.json
```
- Under deployment, double-click deployToSejong to deploy.

### B. TERMINAL
If you're not using Intellij IDEA, follow the following steps to deploy the Hello World contract.
### 1. Build the project

```
$ ./gradlew build
```
The compiled jar bundle will be generated at `./hello-world/build/libs/hello-world-0.1.0.jar`.

### 2. Optimize the jar

You need to optimize your jar bundle before you deploy it to local or ICON networks.
This involves some pre-processing to ensure the actual deployment successful.

`gradle-javaee-plugin` is a Gradle plugin to automate the process of generating the optimized jar bundle.
Run the `optimizedJar` task to generate the optimized jar bundle.

```
$ ./gradlew optimizedJar
```
The output jar will be located at `./hello-world/build/libs/hello-world-0.1.0-optimized.jar`.


### 3. Deploy the optimized jar

You can deploy using either of the following commands. The `build.gradle` in `hello-world` has 4 endpoints included. To deploy on Sejong, run the following command. To deploy on other networks, for example Lison, change `deployToSejong` to `deployToLisbon`.

1. 
    ```sh
    ./gradlew hello-world:deployToSejong -PkeystoreName=<your_wallet_json> -PkeystorePass=<password>
    # example below
    ./gradlew hello-world:deployToSejong -PkeystoreName='JavaTest.json' -PkeystorePass='p@ssw0rd'

    ```

To use the following command, make sure you have `gradle.properties` file with KeyWallet and password linked.

2. 
    ```sh
    ./gradlew hello-world:deployToSejong

    ```
