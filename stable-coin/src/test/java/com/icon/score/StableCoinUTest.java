package com.icon.score;

import com.iconloop.score.test.Score;
import com.iconloop.score.test.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.function.Executable;
import java.math.BigInteger;
import com.iconloop.score.test.Account;
import com.iconloop.score.test.ServiceManager;
import score.Address;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class StableCoinUTest extends TestBase {

    public static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount();
    private Score tokenScore;
    private static final String name = "StableToken";
    private static final String symbol = "STO";
    private static final BigInteger decimals = BigInteger.valueOf(18);
    private static final BigInteger nIssuers = BigInteger.valueOf(2);
    protected final Address EOA_ZERO = new Address(new byte[21]);

    private static Account Alice, Bob, C;


    @BeforeEach
    public void setup() throws Exception {
        tokenScore = sm.deploy(owner, StableCoin.class, name, symbol, decimals, owner.getAddress(), nIssuers);
        Alice = sm.createAccount();
        Bob = sm.createAccount();
        C = sm.createAccount();
    }

    @Test
    void testName() {
        assertEquals(name, tokenScore.call("name"));
    }

    @Test
    void testSymbol() {
        assertEquals(symbol, tokenScore.call("symbol"));
    }

    @Test
    void admin() {
        assertEquals(owner.getAddress(), tokenScore.call("getAdmin"));
    }

    @Test
    void testDecimals() {
        assertEquals(decimals, tokenScore.call("decimals"));
    }

    @Test
    void freeDailyTxLimit() {
        assertEquals(BigInteger.valueOf(50), tokenScore.call("freeDailyTxLimit"));
    }

    @Test()
    void addRemoveIssuers() {
        //not by owner
        Executable addIssuerNotByAdmin = () -> tokenScore.invoke(Alice, "addIssuer", Alice.getAddress());
        String expectedErrorMessage = "Only admin can add issuer";
        expectErrorMessage(addIssuerNotByAdmin, expectedErrorMessage);

        //add issuers by owner
        tokenScore.invoke(owner, "addIssuer", Alice.getAddress());
        tokenScore.invoke(owner, "addIssuer", Bob.getAddress());

        //verify the allowance
        assertEquals(BigInteger.ZERO,tokenScore.call("issuerAllowance",Alice.getAddress()));
        assertEquals(BigInteger.ZERO,tokenScore.call("issuerAllowance",Bob.getAddress()));

        //get issuers
        Address[] issuers = (Address[]) tokenScore.call("getIssuers");
        assertEquals(2, issuers.length);
        assertEquals(Alice.getAddress(), issuers[0]);
        assertEquals(Bob.getAddress(), issuers[1]);

        //add already issued issuers
        Executable alreadyissued = () -> tokenScore.invoke(owner, "addIssuer", Alice.getAddress());
        expectedErrorMessage = Alice.getAddress() + " is already an issuer";
        expectErrorMessage(alreadyissued, expectedErrorMessage);

        //remove issuers by not admin
        Executable removeIssuerByNonAdmin = () -> tokenScore.invoke(Alice, "removeIssuer", Alice.getAddress());
        expectedErrorMessage = "Only admin can remove issuer";
        expectErrorMessage(removeIssuerByNonAdmin, expectedErrorMessage);

        //remove issuer Alice
        tokenScore.invoke(owner, "removeIssuer", Alice.getAddress());

        //try to remove Alice again
        Executable removeNotAnIssuer = () -> tokenScore.invoke(owner, "removeIssuer", Alice.getAddress());
        expectedErrorMessage = Alice.getAddress() + " not an issuer";
        expectErrorMessage(removeNotAnIssuer, expectedErrorMessage);

        issuers = (Address[]) tokenScore.call("getIssuers");
        assertEquals(1, issuers.length);
        assertEquals(Bob.getAddress(), issuers[0]);
    }

    @Test
    void add_more_than_two_issuers(){
        tokenScore.invoke(owner, "addIssuer", Alice.getAddress());
        tokenScore.invoke(owner, "addIssuer", Bob.getAddress());
        Executable thirdIssuer = () -> tokenScore.invoke(owner, "addIssuer", C.getAddress());
        String expectedErrorMessage = "Cannot have more than " + nIssuers + " issuers";
        expectErrorMessage(thirdIssuer, expectedErrorMessage);
    }

    @Test
    void changeAdmin() {
        //not by admin
        Executable changeAdminNotByAdmin = () -> tokenScore.invoke(Alice, "transferAdminRight", Alice.getAddress());
        String expectedErrorMessage = "Only admin can transfer their admin right";
        expectErrorMessage(changeAdminNotByAdmin, expectedErrorMessage);

        //change admin and get admin
        tokenScore.invoke(owner, "transferAdminRight", Alice.getAddress());
        assertEquals(Alice.getAddress(), tokenScore.call("getAdmin"));

        //check whether previous admin lost admin rights
        Executable addIssuerNotByAdmin = () -> tokenScore.invoke(owner, "addIssuer", Alice.getAddress());
        expectedErrorMessage = "Only admin can add issuer";
        expectErrorMessage(addIssuerNotByAdmin, expectedErrorMessage);

    }

    @Test
    void changeFreeDailyTxLimit() {
        //not by admin
        Executable NotByAdmin = () -> tokenScore.invoke(Alice, "changeFreeDailyTxLimit", BigInteger.TWO);
        String expectedErrorMessage = "Only admin can change free daily transaction limit";
        expectErrorMessage(NotByAdmin, expectedErrorMessage);

        //change limit to zero
        Executable zeroLimit = () -> tokenScore.invoke(Alice,"changeFreeDailyTxLimit",BigInteger.ONE.negate());
        expectedErrorMessage = "Free daily transaction limit cannot be under 0.";
        expectErrorMessage(zeroLimit,expectedErrorMessage);

        //change to 2
        tokenScore.invoke(owner,"changeFreeDailyTxLimit",BigInteger.TWO);
        assertEquals(BigInteger.TWO, tokenScore.call("freeDailyTxLimit"));
    }

    @Test
    void approve_negative_amount() {
        //add issuer
        tokenScore.invoke(owner,"addIssuer",Alice.getAddress());

        //approve negative amount
        tokenScore.invoke(owner,"approve",Alice.getAddress(),BigInteger.valueOf(10).negate());

        //try to mint
        Executable allowanceExceed = () -> tokenScore.invoke(Alice, "mint", BigInteger.valueOf(10));
        String expectedErrorMessage = "Allowance amount to mint exceed";
        expectErrorMessage(allowanceExceed, expectedErrorMessage);
    }

    @Test
    void mint_test_by_not_nonIssuers() {
        BigInteger value = BigInteger.TEN.pow(decimals.intValue());
        Executable mintNotByIssuerCall = () -> tokenScore.invoke(Alice, "mintTo", owner.getAddress(), value);
        String expectedErrorMessage = "Only issuers can mint";
        expectErrorMessage(mintNotByIssuerCall, expectedErrorMessage);
    }

    @Test
    void mint_test_to_zero_Address() {
        tokenScore.invoke(owner, "addIssuer", owner.getAddress());
        BigInteger value = BigInteger.TEN.pow(decimals.intValue());
        Executable mintToZero = () -> tokenScore.invoke(owner, "mintTo", EOA_ZERO, value);
        String expectedErrorMessage = "Cannot mint to zero address";
        expectErrorMessage(mintToZero, expectedErrorMessage);
    }

    @Test
    void mint_with_allowance_amount_exceed() {
        BigInteger value = BigInteger.TEN.pow(decimals.intValue());
        //add issuer
        tokenScore.invoke(owner, "addIssuer", owner.getAddress());
        //add allowance
        tokenScore.invoke(owner, "approve", owner.getAddress(), value);

        Executable allowanceExceed = () -> tokenScore.invoke(owner, "mint", value.add(BigInteger.ONE));
        String expectedErrorMessage = "Allowance amount to mint exceed";
        expectErrorMessage(allowanceExceed, expectedErrorMessage);
    }

    @Test
    void mint_in_paused_state() {
        BigInteger value = BigInteger.TEN.pow(decimals.intValue());
        //add issuer
        tokenScore.invoke(owner, "addIssuer", owner.getAddress());
        //pause
        tokenScore.invoke(owner, "togglePause");

        Executable pausedMint = () -> tokenScore.invoke(owner, "mint", value.add(BigInteger.ONE));
        String expectedErrorMessage = "Cannot mint when paused";
        expectErrorMessage(pausedMint, expectedErrorMessage);

    }

    @Test
    void mint_flow() {
        BigInteger value = BigInteger.TEN.pow(decimals.intValue());
        //check if whitelisted
        assertEquals(false, tokenScore.call("isWhitelisted", owner.getAddress()));
        //add issuer
        tokenScore.invoke(owner, "addIssuer", owner.getAddress());
        //add allowance
        tokenScore.invoke(owner, "approve", owner.getAddress(), value);
        //get Issuer Allowance
        assertEquals(value, tokenScore.call("issuerAllowance", owner.getAddress()));
        //mint
        tokenScore.invoke(owner, "mint", value);
        //check balance
        assertEquals(value, tokenScore.call("balanceOf", owner.getAddress()));
        //check totalSupply
        assertEquals(value, tokenScore.call("totalSupply"));
        //check if whitelisted
        assertEquals(true, tokenScore.call("isWhitelisted", owner.getAddress()));
    }

    @Test
    void burn_test_with_zero_amount() {
        Executable burn = () -> tokenScore.invoke(owner, "burn", BigInteger.ZERO);
        String expectedErrorMessage = "Amount to burn should be greater than zero";
        expectErrorMessage(burn, expectedErrorMessage);

        //burn negative amount
        burn = () -> tokenScore.invoke(owner, "burn", BigInteger.ONE.negate());
        expectedErrorMessage = "Amount to burn should be greater than zero";
        expectErrorMessage(burn, expectedErrorMessage);
    }

    @Test
    void burn_with_insufficient_amount() {
        mint_flow();
        BigInteger value = BigInteger.TEN.pow(decimals.intValue());

        Executable insufficientBalance = () -> tokenScore.invoke(owner, "burn", value.add(BigInteger.ONE));
        String expectedErrorMessage = "Insufficient balance to burn";
        expectErrorMessage(insufficientBalance, expectedErrorMessage);
    }

    @Test
    void burn_in_paused_state() {
        BigInteger value = BigInteger.TEN.pow(decimals.intValue());
        mint_flow();
        //pause
        tokenScore.invoke(owner, "togglePause");

        Executable pausedMint = () -> tokenScore.invoke(owner, "burn", value);
        String expectedErrorMessage = "Cannot burn when paused";
        expectErrorMessage(pausedMint, expectedErrorMessage);

    }

    @Test
    void burn_flow() {
        BigInteger value = BigInteger.TEN.pow(decimals.intValue());
        mint_flow();
        //burn
        tokenScore.invoke(owner, "burn", value);
        //check balance
        assertEquals(BigInteger.ZERO, tokenScore.call("balanceOf", owner.getAddress()));
        //check totalSupply
        assertEquals(BigInteger.ZERO, tokenScore.call("totalSupply"));
    }

    @Test
    void transfer_test_to_zero_Address() {
        mint_flow();
        BigInteger value = BigInteger.TEN.pow(decimals.intValue());
        Executable transferToZero = () -> tokenScore.invoke(owner, "transfer", EOA_ZERO, value,
                "transfer".getBytes());
        String expectedErrorMessage = "Cannot transfer to zero address";
        expectErrorMessage(transferToZero, expectedErrorMessage);
    }

    @Test
    void transfer_test_zero_or_less() {
        mint_flow();
        Executable transferZero = () -> tokenScore.invoke(owner, "transfer", Alice.getAddress(), BigInteger.ZERO,
                "transfer".getBytes());
        String expectedErrorMessage = "Cannot transfer zero or less";
        expectErrorMessage(transferZero, expectedErrorMessage);

        Executable transferNegative = () -> tokenScore.invoke(owner, "transfer", Alice.getAddress(),
                BigInteger.ONE.negate(), "transfer".getBytes());
        expectErrorMessage(transferNegative, expectedErrorMessage);
    }

    @Test
    void transfer_insufficientBalance() {
        mint_flow();
        BigInteger value = BigInteger.TEN.pow(decimals.intValue());
        Executable transferInsufficientBalance = () -> tokenScore.invoke(owner, "transfer", Alice.getAddress(),
                value.add(BigInteger.ONE), "transfer".getBytes());
        String expectedErrorMessage = "Insufficient Balance";
        expectErrorMessage(transferInsufficientBalance, expectedErrorMessage);
    }

    @Test
    void transfer_when_paused() {
        mint_flow();
        //pause
        tokenScore.invoke(owner, "togglePause");
        BigInteger value = BigInteger.TEN.pow(decimals.intValue());
        Executable transferInPause = () -> tokenScore.invoke(owner, "transfer", Alice.getAddress(), value,
                "transfer".getBytes());
        String expectedErrorMessage = "Cannot transfer when paused";
        expectErrorMessage(transferInPause, expectedErrorMessage);
    }

    @Test
    void transfer_flow() {
        mint_flow();
        BigInteger value = BigInteger.TEN.pow(decimals.intValue());

        //balance check
        assertEquals(value, tokenScore.call("balanceOf", owner.getAddress()));
        assertEquals(BigInteger.ZERO, tokenScore.call("balanceOf", Alice.getAddress()));

        BigInteger ownerBalBeforeTransfer = (BigInteger) tokenScore.call("balanceOf", owner.getAddress());
        BigInteger AliceBalBeforeTransfer = (BigInteger) tokenScore.call("balanceOf", Alice.getAddress());

        BigInteger transferValue =BigInteger.valueOf(5).pow(decimals.intValue());
        //transfer
        tokenScore.invoke(owner, "transfer", Alice.getAddress(), transferValue, "transfer".getBytes());

        BigInteger ownerBalAfterTransfer = (BigInteger) tokenScore.call("balanceOf", owner.getAddress());
        BigInteger AliceBalAfterTransfer = (BigInteger) tokenScore.call("balanceOf", Alice.getAddress());

        //balance after transfer
        assertEquals(ownerBalBeforeTransfer.subtract(transferValue),ownerBalAfterTransfer);
        assertEquals(AliceBalBeforeTransfer.add(transferValue),AliceBalAfterTransfer);

        // transfer self
        tokenScore.invoke(Alice, "transfer", Alice.getAddress(), transferValue, "self transfer".getBytes());
        BigInteger AliceBalAfterSelfTransfer = (BigInteger) tokenScore.call("balanceOf", Alice.getAddress());

        assertEquals(AliceBalAfterTransfer,AliceBalAfterSelfTransfer);
        assertEquals(value, tokenScore.call("totalSupply"));
    }

    private void expectErrorMessage(Executable contractCall, String errorMessage) {
        AssertionError e = Assertions.assertThrows(AssertionError.class, contractCall);
        assertEquals(errorMessage, e.getMessage());
    }

}