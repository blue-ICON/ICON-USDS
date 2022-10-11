package com.icon.score;

import com.iconloop.score.test.Score;
import com.iconloop.score.test.TestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import com.iconloop.score.test.Account;
import com.iconloop.score.test.ServiceManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AppTest extends TestBase {

    public static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount();
    private static Score tokenScore;
    private static final String name = "StableToken";
    private static final String symbol = "STO";
    private static final BigInteger decimals = BigInteger.valueOf(18);
    private static final int nIssuers = 2;

    @BeforeAll
    public static void setup() throws Exception {
        StableCoin classUnderTest = new StableCoin(name, symbol, decimals,nIssuers);
    }
    @Test
    void testName() {
        final String name = "Stable Token";
//        StableCoin classUnderTest = new StableCoin("Stable Token", "STO", BigInteger.valueOf(18),2);
//        assertEquals(classUnderTest.name(), name);
    }
    @Test
    void appHasAGreeting() {

    }
}