package com.icon.score;

import com.iconloop.score.test.Score;
import com.iconloop.score.test.TestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import com.iconloop.score.test.Account;
import com.iconloop.score.test.ServiceManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.spy;

public class AppTest extends TestBase {

    public static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount();
    private Score tokenScore;
    public StableCoin scoreSpy;
    private static final String name = "StableToken";
    private static final String symbol = "STO";
    private static final BigInteger decimals = BigInteger.valueOf(18);
    private static final int nIssuers = 2;

    @BeforeEach
    public void setup() throws Exception {
        tokenScore = sm.deploy(owner,StableCoin.class, name, symbol, decimals,owner.getAddress(),nIssuers);
//        System.out.println(tokenScore.call("name"));
        StableCoin t = (StableCoin) tokenScore.getInstance();
        scoreSpy = spy(t);
//        mockScoreClients();
        tokenScore.setInstance(scoreSpy);
    }
    @Test
    void testName() {
        assertEquals(name,tokenScore.call("name"));
    }
    @Test
    void testSymbol() {
        assertEquals(symbol,tokenScore.call("symbol"));
    }

    @Test
    void testDecimals(){
        assertEquals(decimals,tokenScore.call("decimals"));
    }
}