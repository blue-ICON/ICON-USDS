package com.icon.score;

import com.iconloop.score.test.Score;
import com.iconloop.score.test.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.function.Executable;
import java.math.BigInteger;
import java.util.List;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.ServiceManager;
import score.Address;

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
    protected final Address EOA_ZERO = Address.fromString("hx0000000000000000000000000000000000000000");


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
    void admin(){
        assertEquals(owner.getAddress(),tokenScore.call("getAdmin"));
    }

    @Test
    void testDecimals(){
        assertEquals(decimals,tokenScore.call("decimals"));
    }

    @Test()
    void addRemoveIssuers(){
        //not by owner
        Account A = sm.createAccount();
        Account B = sm.createAccount();
        Executable addIssuerNotByAdmin = () -> tokenScore.invoke(A,"addIssuer",A.getAddress());
        String expectedErrorMessage = "Only admin can add issuer";
        expectErrorMessage(addIssuerNotByAdmin,expectedErrorMessage);

        //add issuers by owner
        tokenScore.invoke(owner,"addIssuer",A.getAddress());
        tokenScore.invoke(owner,"addIssuer",B.getAddress());

        //get issuers
        List<Address> issuers =(List<Address>)tokenScore.call("getIssuers");
        assertEquals(A.getAddress(),issuers.get(0));
        assertEquals(B.getAddress(),issuers.get(1));

        //add already issued issuers
        Executable alreadyissued = () -> tokenScore.invoke(owner,"addIssuer",A.getAddress());
        expectedErrorMessage = A.getAddress() + " is already an issuer";
        expectErrorMessage(alreadyissued,expectedErrorMessage);

        //remove issuers by not admin
        Executable removeIssuerByNonAdmin = () -> tokenScore.invoke(A,"removeIssuer",A.getAddress());
        expectedErrorMessage = "Only admin can remove issuer";
        expectErrorMessage(removeIssuerByNonAdmin,expectedErrorMessage);

        //remove issuer A
        tokenScore.invoke(owner,"removeIssuer",A.getAddress());

        //try to remove A again
        Executable removeNotAnIssuer = () -> tokenScore.invoke(owner,"removeIssuer",A.getAddress());
        expectedErrorMessage = A.getAddress() + " not an issuer";
        expectErrorMessage(removeNotAnIssuer,expectedErrorMessage);
    }

    @Test
    void changeAdmin(){
        //not by admin
        Account A = sm.createAccount();
        Account B = sm.createAccount();
        Executable changeAdminNotByAdmin = () -> tokenScore.invoke(A,"transferAdminRight",A.getAddress());
        String expectedErrorMessage = "Only admin can transfer their admin right";
        expectErrorMessage(changeAdminNotByAdmin,expectedErrorMessage);

        //change admin and get admin
        tokenScore.invoke(owner,"transferAdminRight",A.getAddress());
        assertEquals(A.getAddress(),tokenScore.call("getAdmin"));

        //check whether previous admin lost admin rights
        Executable addIssuerNotByAdmin = () -> tokenScore.invoke(owner,"addIssuer",A.getAddress());
        expectedErrorMessage = "Only admin can add issuer";
        expectErrorMessage(addIssuerNotByAdmin,expectedErrorMessage);
    }

    @Test
    void mint_test_by_not_nonIssuers(){
        Account A = sm.createAccount();
        BigInteger value = BigInteger.TEN.pow(decimals.intValue());
//        Executable mintNotByIssuerCall = () -> tokenScore.invoke(A,"mintTo", owner.getAddress(), value);
//        String expectedErrorMessage = "Only issuers can mint";
//        expectErrorMessage(mintNotByIssuerCall,expectedErrorMessage);

        tokenScore.invoke(owner,"addIssuer",A.getAddress());
        tokenScore.invoke(owner,"approve",A.getAddress(),value);
        tokenScore.invoke(A,"mintTo", owner.getAddress(), BigInteger.valueOf(2));
    }

    @Test
    void mint_test_to_zero_Address(){
        BigInteger value = BigInteger.TEN.pow(decimals.intValue());
        Executable mintToZero = () -> tokenScore.invoke(owner,"mintTo", EOA_ZERO, value);
        String expectedErrorMessage = "Cannot mint to zero address";
        expectErrorMessage(mintToZero,expectedErrorMessage);
    }

    @Test
    void transfer() {
        Account A = sm.createAccount();
        BigInteger value = BigInteger.TEN.pow(decimals.intValue());
        tokenScore.invoke(owner, "transfer", A.getAddress(), value, "to alice".getBytes());
        owner.subtractBalance(symbol, value);
        assertEquals(owner.getBalance(symbol),
                tokenScore.call("balanceOf", tokenScore.getOwner().getAddress()));
        assertEquals(value,
                tokenScore.call("balanceOf", A.getAddress()));

        // transfer self
        tokenScore.invoke(A, "transfer", A.getAddress(), value, "self transfer".getBytes());
        assertEquals(value, tokenScore.call("balanceOf", A.getAddress()));
    }

    private void expectErrorMessage(Executable contractCall, String errorMessage) {
        AssertionError e = Assertions.assertThrows(AssertionError.class, contractCall);
        assertEquals(errorMessage, e.getMessage());
    }

}