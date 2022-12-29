package com.icon.score.unit.test;

import com.icon.score.StableCoin;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.function.Executable;

import java.math.BigInteger;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.ServiceManager;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import score.Address;
import score.Context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


public class StableCoinUnitTest extends TestBase {

    public static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount();
    private Score tokenScore;
    private static final String name = "StableToken";
    private static final String symbol = "STO";
    private static final BigInteger decimals = BigInteger.valueOf(18);
    private static final BigInteger nIssuers = BigInteger.valueOf(2);
    protected final Address EOA_ZERO = new Address(new byte[21]);
    private static Account Alice, Bob, Cathy;
    StableCoin scoreSpy;
    public static final Account scoreAccount = Account.newScoreAccount(101);

    static MockedStatic<Context> contextMock;

    @BeforeAll
    protected static void init() {
        contextMock = Mockito.mockStatic(Context.class, Mockito.CALLS_REAL_METHODS);
    }

    @BeforeEach
    public void setup() throws Exception {
        tokenScore = sm.deploy(owner, StableCoin.class, name, symbol, decimals, owner.getAddress(), nIssuers);
        Alice = sm.createAccount();
        Bob = sm.createAccount();
        Cathy = sm.createAccount();

        StableCoin instance = (StableCoin) tokenScore.getInstance();
        scoreSpy = spy(instance);
        tokenScore.setInstance(scoreSpy);
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
        assertEquals(BigInteger.ZERO, tokenScore.call("issuerAllowance", Alice.getAddress()));
        assertEquals(BigInteger.ZERO, tokenScore.call("issuerAllowance", Bob.getAddress()));

        //get issuers
        Address[] issuers = (Address[]) tokenScore.call("getIssuers");
        assertEquals(2, issuers.length);
        assertEquals(Alice.getAddress(), issuers[0]);
        assertEquals(Bob.getAddress(), issuers[1]);

        tokenScore.invoke(owner,"approve",Alice.getAddress(),BigInteger.ONE);

        //add already issued issuers
        Executable alreadyIssued = () -> tokenScore.invoke(owner, "addIssuer", Alice.getAddress());
        expectedErrorMessage = Alice.getAddress() + " is already an issuer";
        expectErrorMessage(alreadyIssued, expectedErrorMessage);

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
    void add_more_than_two_issuers() {
        tokenScore.invoke(owner, "addIssuer", Alice.getAddress());
        tokenScore.invoke(owner, "addIssuer", Bob.getAddress());
        Executable thirdIssuer = () -> tokenScore.invoke(owner, "addIssuer", Cathy.getAddress());
        String expectedErrorMessage = "Cannot have more than " + nIssuers + " issuers";
        expectErrorMessage(thirdIssuer, expectedErrorMessage);
    }

    @Test
    void changeAdmin() {
        //not by admin
        Executable changeAdminNotByAdmin = () -> tokenScore.invoke(Alice, "transferAdminRight", Alice.getAddress());
        String expectedErrorMessage = "Only admin can transfer their admin right";
        expectErrorMessage(changeAdminNotByAdmin, expectedErrorMessage);

        // change Admin to zeroAddress
        Executable zeroAddressAdmin = () -> tokenScore.invoke(owner, "transferAdminRight", EOA_ZERO);
        expectedErrorMessage = "Cannot set zero address as admin";
        expectErrorMessage(zeroAddressAdmin, expectedErrorMessage);

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
        Executable zeroLimit = () -> tokenScore.invoke(Alice, "changeFreeDailyTxLimit", BigInteger.ONE.negate());
        expectedErrorMessage = "Free daily transaction limit cannot be under 0.";
        expectErrorMessage(zeroLimit, expectedErrorMessage);

        //change to 2
        tokenScore.invoke(owner, "changeFreeDailyTxLimit", BigInteger.TWO);
        assertEquals(BigInteger.TWO, tokenScore.call("freeDailyTxLimit"));
    }

    @Test
    void approve_negative_amount() {
        //add issuer
        tokenScore.invoke(owner, "addIssuer", Alice.getAddress());

        //approve negative amount
        tokenScore.invoke(owner, "approve", Alice.getAddress(), BigInteger.valueOf(10).negate());

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

        BigInteger balance_before_mint = (BigInteger) tokenScore.call("balanceOf", owner.getAddress());
        BigInteger total_supply_before_mint = (BigInteger) tokenScore.call("totalSupply");

        //mint
        tokenScore.invoke(owner, "mint", value);

        //check balance
        BigInteger balance_after_mint = (BigInteger) tokenScore.call("balanceOf", owner.getAddress());
        assertEquals(balance_before_mint.add(value), balance_after_mint);

        //check totalSupply
        BigInteger total_supply_after_mint = (BigInteger) tokenScore.call("totalSupply");
        assertEquals(total_supply_before_mint.add(value), total_supply_after_mint);

        //check if whitelisted
        assertEquals(true, tokenScore.call("isWhitelisted", owner.getAddress()));

        // event logs verification
        verify(scoreSpy).Approval(owner.getAddress(), owner.getAddress(), value);
        verify(scoreSpy).Transfer(EOA_ZERO, owner.getAddress(), value, "mint".getBytes());
        verify(scoreSpy).Mint(owner.getAddress(), value);
        verify(scoreSpy).WhitelistWallet(owner.getAddress(), "whitelist on mint".getBytes());
    }

    @Test
    void mint_by_contract() {
        BigInteger value = BigInteger.TEN.pow(decimals.intValue());

        tokenScore.invoke(owner, "addIssuer", scoreAccount.getAddress());
        tokenScore.invoke(owner, "approve", scoreAccount.getAddress(), value);

        BigInteger balance_before = (BigInteger) tokenScore.call("balanceOf", scoreAccount.getAddress());
        BigInteger total_supply_before = (BigInteger) tokenScore.call("totalSupply");

        tokenScore.invoke(scoreAccount, "mint", value);
        System.out.println(tokenScore.call("balanceOf", scoreAccount.getAddress()));

        BigInteger balance_after = (BigInteger) tokenScore.call("balanceOf", scoreAccount.getAddress());
        BigInteger total_supply_after = (BigInteger) tokenScore.call("totalSupply");

        assertEquals(balance_before.add(value), balance_after);
        assertEquals(total_supply_before.add(value), total_supply_after);

        Executable allowanceMintExceed = () -> tokenScore.invoke(scoreAccount, "mint", value);
        String expectedErrorMessage = "Allowance amount to mint exceed";

        expectErrorMessage(allowanceMintExceed, expectedErrorMessage);
    }

    @Test
    void whitelisted_account_mints_again() {
        BigInteger value = BigInteger.TEN.pow(decimals.intValue());

        // owner mints
        mint_flow();

        tokenScore.invoke(owner, "approve", owner.getAddress(), value);

        //check if whitelisted
        assertEquals(true, tokenScore.call("isWhitelisted", owner.getAddress()));

        BigInteger balance_before_mint = (BigInteger) tokenScore.call("balanceOf", owner.getAddress());
        BigInteger total_supply_before_mint = (BigInteger) tokenScore.call("totalSupply");

        // owner mints again
        tokenScore.invoke(owner, "mint", value);

        //check balance
        BigInteger balance_after_mint = (BigInteger) tokenScore.call("balanceOf", owner.getAddress());
        assertEquals(balance_before_mint.add(value), balance_after_mint);

        //check totalSupply
        BigInteger total_supply_after_mint = (BigInteger) tokenScore.call("totalSupply");
        assertEquals(total_supply_before_mint.add(value), total_supply_after_mint);

        verify(scoreSpy, times(2)).Approval(owner.getAddress(), owner.getAddress(), value);
        verify(scoreSpy, times(2)).Transfer(EOA_ZERO, owner.getAddress(), value, "mint".getBytes());
        verify(scoreSpy, times(2)).Mint(owner.getAddress(), value);

        // account is whitelisted only once
        verify(scoreSpy).WhitelistWallet(owner.getAddress(), "whitelist on mint".getBytes());

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

        BigInteger balance_before_burn = (BigInteger) tokenScore.call("balanceOf", owner.getAddress());
        BigInteger totalSupply_before_burn = (BigInteger) tokenScore.call("totalSupply");

        //burn
        tokenScore.invoke(owner, "burn", value);

        // check balance
        BigInteger balance_after_burn = (BigInteger) tokenScore.call("balanceOf", owner.getAddress());
        assertEquals(balance_before_burn.subtract(value), balance_after_burn);

        //check totalSupply
        BigInteger totalSupply_after_burn = (BigInteger) tokenScore.call("totalSupply");
        assertEquals(totalSupply_before_burn.subtract(value), totalSupply_after_burn);

        verify(scoreSpy).Transfer(owner.getAddress(), EOA_ZERO, value, "burn".getBytes());
        verify(scoreSpy).Burn(owner.getAddress(), value);
    }

    @Test
    void burn_flow_not_by_owner() {
        BigInteger burn_value = BigInteger.valueOf(5).pow(decimals.intValue());

        mint_flow();
        tokenScore.invoke(owner, "transfer", Alice.getAddress(), burn_value, "transfer".getBytes());

        BigInteger balance_before_burn = (BigInteger) tokenScore.call("balanceOf", Alice.getAddress());
        BigInteger totalSupply_before_burn = (BigInteger) tokenScore.call("totalSupply");

        assertEquals(balance_before_burn, burn_value);

        tokenScore.invoke(Alice, "burn", burn_value);
        BigInteger balance_after_burn = (BigInteger) tokenScore.call("balanceOf", Alice.getAddress());
        BigInteger totalSupply_after_burn = (BigInteger) tokenScore.call("totalSupply");

        assertEquals(balance_before_burn.subtract(burn_value), balance_after_burn);
        assertEquals(totalSupply_before_burn.subtract(burn_value), totalSupply_after_burn);

        // event log verification
        verify(scoreSpy).Transfer(Alice.getAddress(), EOA_ZERO, burn_value, "burn".getBytes());
        verify(scoreSpy).Burn(Alice.getAddress(), burn_value);

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
    void transfer_to_contract() {
        mint_flow();
        BigInteger value = BigInteger.TEN.pow(decimals.intValue());
        contextMock.when(
                        contractCall(scoreAccount.getAddress(), owner.getAddress(), value, "transfer".getBytes())).
                thenReturn(BigInteger.ZERO);
        tokenScore.invoke(owner, "transfer", scoreAccount.getAddress(), value, "transfer".getBytes());
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

        BigInteger transferValue = BigInteger.valueOf(5).pow(decimals.intValue());
        //transfer
        tokenScore.invoke(owner, "transfer", Alice.getAddress(), transferValue, "transfer".getBytes());

        //check whether Alice is whitelisted or not
        assertEquals(false, tokenScore.call("isWhitelisted", Alice.getAddress()));

        BigInteger ownerBalAfterTransfer = (BigInteger) tokenScore.call("balanceOf", owner.getAddress());
        BigInteger AliceBalAfterTransfer = (BigInteger) tokenScore.call("balanceOf", Alice.getAddress());

        //balance after transfer
        assertEquals(ownerBalBeforeTransfer.subtract(transferValue), ownerBalAfterTransfer);
        assertEquals(AliceBalBeforeTransfer.add(transferValue), AliceBalAfterTransfer);

        // transfer self
        tokenScore.invoke(Alice, "transfer", Alice.getAddress(), transferValue, "self transfer".getBytes());
        BigInteger AliceBalAfterSelfTransfer = (BigInteger) tokenScore.call("balanceOf", Alice.getAddress());

        assertEquals(AliceBalAfterTransfer, AliceBalAfterSelfTransfer);
        assertEquals(value, tokenScore.call("totalSupply"));

        verify(scoreSpy).Transfer(owner.getAddress(), Alice.getAddress(), transferValue, "transfer".getBytes());
        verify(scoreSpy).Transfer(Alice.getAddress(), Alice.getAddress(), transferValue, "self transfer".getBytes());
    }

    @Test
    void check_free_transactions() {

        BigInteger value = BigInteger.TEN.pow(decimals.intValue());
        tokenScore.invoke(owner, "changeFreeDailyTxLimit", BigInteger.valueOf(3));

        // free transaction before whitelisted
        BigInteger free_tx_owner = (BigInteger) tokenScore.call("remainingFreeTxThisTerm", Alice.getAddress());
        assertEquals(BigInteger.ZERO, free_tx_owner);

        // Alice mints
        tokenScore.invoke(owner, "addIssuer", Alice.getAddress());
        tokenScore.invoke(owner, "approve", Alice.getAddress(), value);
        tokenScore.invoke(Alice, "mint", value);

        free_tx_owner = (BigInteger) tokenScore.call("remainingFreeTxThisTerm", Alice.getAddress());
        assertEquals(BigInteger.TWO, free_tx_owner);

        // Alice does 2 more transactions
        tokenScore.invoke(Alice, "transfer", owner.getAddress(), BigInteger.ONE, new byte[0]);
        free_tx_owner = (BigInteger) tokenScore.call("remainingFreeTxThisTerm", Alice.getAddress());
        assertEquals(BigInteger.ONE, free_tx_owner);

        tokenScore.invoke(Alice, "transfer", owner.getAddress(), BigInteger.ONE, new byte[0]);
        free_tx_owner = (BigInteger) tokenScore.call("remainingFreeTxThisTerm", Alice.getAddress());
        assertEquals(BigInteger.ZERO, free_tx_owner);
    }

    private void expectErrorMessage(Executable contractCall, String errorMessage) {
        AssertionError e = Assertions.assertThrows(AssertionError.class, contractCall);
        assertEquals(errorMessage, e.getMessage());
    }

    public MockedStatic.Verification contractCall(Address to, Address from, BigInteger value, byte[] data) {
        return () -> Context.call(to, "tokenFallback", from, value, data);
    }

}