/*
 * Copyright 2020 ICONLOOP Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.icon.score.cases;

import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.test.Env;
import foundation.icon.test.ResultTimeoutException;
import foundation.icon.test.TestBase;
import foundation.icon.test.TransactionFailureException;
import foundation.icon.test.TransactionHandler;
import com.icon.score.score.StableCoinScore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.math.BigInteger;

import java.util.HashMap;
import java.util.Map;

import static foundation.icon.test.Env.LOG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StableCoinIntTest extends TestBase {
    private static final boolean DEBUG = true;
    private static final Address ZERO_ADDRESS = new Address("hx0000000000000000000000000000000000000000");
    private static TransactionHandler txHandler;
    private static KeyWallet[] wallets;
    private static KeyWallet ownerWallet, caller;
    private static StableCoinScore tokenScore;
    private static BigInteger value;

    private Map<String, Boolean> status = new HashMap<>();

    @BeforeAll
     static void setup() throws Exception {
        Env.Chain chain = Env.getDefaultChain();
        IconService iconService = new IconService(new HttpProvider(chain.getEndpointURL(3)));
        txHandler = new TransactionHandler(iconService, chain);

        // init wallets
        wallets = new KeyWallet[2];
        BigInteger amount = ICX.multiply(BigInteger.valueOf(50));
        for (int i = 0; i < wallets.length; i++) {
            wallets[i] = KeyWallet.create();
            txHandler.transfer(wallets[i].getAddress(), amount);
        }
        for (KeyWallet wallet : wallets) {
            ensureIcxBalance(txHandler, wallet.getAddress(), BigInteger.ZERO, amount);
        }
        ownerWallet = wallets[0];
        caller = wallets[1];
        tokenScore = StableCoinScore.mustDeploy(txHandler, ownerWallet);
        value = BigInteger.TEN.pow(tokenScore.decimals().intValue());
    }

    @AfterAll
    static void shutdown() throws Exception {
        for (KeyWallet wallet : wallets) {
            txHandler.refundAll(wallet);
        }
    }

    @Order(1)
    @Test
    public void initialCheck() throws IOException {
        LOG.infoEntering("initial check");
        assertEquals(BigInteger.ZERO, tokenScore.balanceOf(ownerWallet.getAddress()));
        assertEquals(BigInteger.ZERO, tokenScore.balanceOf(caller.getAddress()));
        assertEquals(BigInteger.ZERO, tokenScore.totalSupply());
        assertEquals("Stable Token", tokenScore.name());
        assertEquals("STO", tokenScore.symbol());
        assertEquals(ownerWallet.getAddress(), tokenScore.admin());
        assertEquals(BigInteger.valueOf(18), tokenScore.decimals());
        assertEquals(BigInteger.valueOf(50), tokenScore.freeDailyTxLimit());
    }

    @Order(2)
    @Test
    public void add_same_issuer_twice() throws IOException, ResultTimeoutException {

        if(!status.getOrDefault("add_and_approve_owner",false)){
            add_and_approve(tokenScore,value);
        }

        assertEquals(ownerWallet.getAddress().toString(),tokenScore.getIssuers().get(0).asString());
        assertEquals(1,tokenScore.getIssuers().size());

        Bytes add = tokenScore.addIssuer(ownerWallet, ownerWallet.getAddress());
        assertFailure(txHandler.getResult(add));
    }

    @Order(3)
    @Test
    public void remove_issuer_and_nonexistent_issuer() throws IOException, ResultTimeoutException, TransactionFailureException {
        if(!status.getOrDefault("add_and_approve_owner",false)){
            add_and_approve(tokenScore,value);
        }

        LOG.infoEntering("admin removes owner as issuer");
        Bytes remove = tokenScore.removeIssuer(ownerWallet, ownerWallet.getAddress());
        assertSuccess(txHandler.getResult(remove));

        LOG.infoEntering("admin try remove owner as issuer again");
        remove = tokenScore.removeIssuer(ownerWallet, ownerWallet.getAddress());
        assertFailure(txHandler.getResult(remove));

        status.put("add_and_approve_owner",false);
    }

    @Order(4)
    @Test
    public void transfer_admin_right_check_access() throws IOException, ResultTimeoutException, TransactionFailureException {

        LOG.infoEntering("transfer admin right");
        Bytes adminRight = tokenScore.transferAdminRight(ownerWallet, caller.getAddress());
        assertSuccess(txHandler.getResult(adminRight));

        LOG.infoEntering("owner add owner as issuer");
        Bytes add = tokenScore.addIssuer(ownerWallet, ownerWallet.getAddress());
        assertFailure(txHandler.getResult(add));

        LOG.infoEntering("caller add owner as issuer");
        add = tokenScore.addIssuer(caller, ownerWallet.getAddress());
        assertSuccess(txHandler.getResult(add));

        LOG.infoEntering("caller remove owner as issuer");
        Bytes remove = tokenScore.removeIssuer(caller, ownerWallet.getAddress());
        assertSuccess(txHandler.getResult(remove));

        LOG.infoEntering("transfer admin right back to owner");
        adminRight = tokenScore.transferAdminRight(caller, ownerWallet.getAddress());
        assertSuccess(txHandler.getResult(adminRight));

        status.put("add_and_approve_owner",false);
    }

    @Order(5)
    @Test
    public void check_transactions_when_paused() throws IOException, ResultTimeoutException, TransactionFailureException {

        if(!status.getOrDefault("add_and_approve_owner",false)){
            add_and_approve(tokenScore,value);
        }

        LOG.infoEntering("admin pause the contract");
        Bytes togglePause = tokenScore.togglePause(ownerWallet);
        assertSuccess(txHandler.getResult(togglePause));

        LOG.infoEntering("mint fails when paused");
        Bytes mint = tokenScore.mint(ownerWallet, value);
        assertFailure(txHandler.getResult(mint));

        LOG.infoEntering("burn fails when paused");
        Bytes burn = tokenScore.burn(caller, value.divide(BigInteger.TWO));
        assertFailure(txHandler.getResult(burn));

        LOG.infoEntering("transfer fails when paused");
        Bytes transfer = tokenScore.transfer(ownerWallet, caller.getAddress(), value.divide(BigInteger.TWO), "transfer".getBytes());
        assertFailure(txHandler.getResult(transfer));

        LOG.infoEntering("admin unpause the contract");
        togglePause = tokenScore.togglePause(ownerWallet);
        assertSuccess(txHandler.getResult(togglePause));
    }

    @Order(6)
    @Test
    public void check_transaction_minting_more_than_allowance() throws IOException, ResultTimeoutException, TransactionFailureException {

        if(!status.getOrDefault("add_and_approve_owner",false)){
            add_and_approve(tokenScore,value);
        }

        LOG.infoEntering("mint fails when minting value is more than allowance");
        Bytes mint = tokenScore.mint(ownerWallet, value.add(BigInteger.TWO));
        assertFailure(txHandler.getResult(mint));

    }

    @Order(7)
    @Test
    public void mint_to_zero_address() throws IOException, ResultTimeoutException, TransactionFailureException {

        if(!status.getOrDefault("add_and_approve_owner",false)){
            add_and_approve(tokenScore,value);
        }

        LOG.infoEntering("mint fails when minting to zero address");
        Bytes mint = tokenScore.mintTo(ownerWallet, ZERO_ADDRESS, value);
        assertFailure(txHandler.getResult(mint));
    }

    @Order(8)
    @Test
    public void mint_by_non_issuers() throws IOException, ResultTimeoutException, TransactionFailureException {
        if(!status.getOrDefault("add_and_approve_owner",false)){
            add_and_approve(tokenScore,value);
        }

        LOG.infoEntering("mint fails when done by non Issuer");
        Bytes mint = tokenScore.mint(caller, value);
        assertFailure(txHandler.getResult(mint));
    }

    @Order(9)
    @Test
    public void mint_zero_or_less() throws IOException, ResultTimeoutException, TransactionFailureException {
        if(!status.getOrDefault("add_and_approve_owner",false)){
            add_and_approve(tokenScore,value);
        }

        LOG.infoEntering("mint fails when minting zero");
        Bytes mint = tokenScore.mintTo(ownerWallet, caller.getAddress(), BigInteger.ZERO);
        assertFailure(txHandler.getResult(mint));

        LOG.infoEntering("mint fails when minting negative");
        mint = tokenScore.mintTo(ownerWallet, caller.getAddress(), BigInteger.ONE.negate());
        assertFailure(txHandler.getResult(mint));
    }

    @Order(10)
    @Test
    public void transfer_zero_or_less() throws IOException, ResultTimeoutException, TransactionFailureException {

        if(!status.getOrDefault("add_and_approve_owner",false)){
            add_and_approve(tokenScore,value);
        }

        if(!status.getOrDefault("mint",false)){
            mint();
        }
        LOG.infoEntering("transfer fails when value = zero ");
        Bytes transfer = tokenScore.transfer(ownerWallet, caller.getAddress(), BigInteger.ZERO, "transfer".getBytes());
        assertFailure(txHandler.getResult(transfer));

        LOG.infoEntering("transfer fails when value = negative ");
        transfer = tokenScore.transfer(ownerWallet, caller.getAddress(), value.negate(), "transfer".getBytes());
        assertFailure(txHandler.getResult(transfer));
    }

    @Order(11)
    @Test
    public void transfer_to_zero_address() throws IOException, ResultTimeoutException, TransactionFailureException {
        if(!status.getOrDefault("add_and_approve_owner",false)){
            add_and_approve(tokenScore,value);
        }
        if(!status.getOrDefault("mint",false)){
            mint();
        }
        BigInteger value = BigInteger.TEN.pow(tokenScore.decimals().intValue());

        LOG.infoEntering("transfer fails when to zero address");
        Bytes transfer = tokenScore.transfer(ownerWallet, ZERO_ADDRESS, value.divide(BigInteger.TWO), "transfer".getBytes());
        assertFailure(txHandler.getResult(transfer));
    }

    @Order(12)
    @Test
    public void check_transaction_transfer_more_than_balance() throws IOException, ResultTimeoutException, TransactionFailureException {

        if(!status.getOrDefault("add_and_approve_owner",false)){
            add_and_approve(tokenScore,value);
        }
        if(!status.getOrDefault("mint",false)){
            mint();
        }
        LOG.infoEntering("transfer more than balance - fails");
        Bytes transfer = tokenScore.transfer(ownerWallet, caller.getAddress(), value.add(BigInteger.TWO), "transfer".getBytes());
        assertFailure(txHandler.getResult(transfer));
    }

    @Order(13)
    @Test
    public void transfer_to_self() throws IOException, ResultTimeoutException {

        if(!status.getOrDefault("add_and_approve_owner",false)){
            add_and_approve(tokenScore,value);
        }
        if(!status.getOrDefault("mint",false)){
            mint();
        }

        LOG.infoEntering("transfer self");
        Bytes transfer = tokenScore.transfer(ownerWallet, ownerWallet.getAddress(), value.divide(BigInteger.TWO), "transfer".getBytes());
        assertSuccess(txHandler.getResult(transfer));
        assertEquals(value,tokenScore.balanceOf(ownerWallet.getAddress()));
        assertEquals(value,tokenScore.totalSupply());
    }

    @Order(14)
    @Test
    public void check_transaction_burning_more_than_balance() throws IOException, ResultTimeoutException, TransactionFailureException {

        if(!status.getOrDefault("add_and_approve_owner",false)){
            add_and_approve(tokenScore,value);
        }
        if(!status.getOrDefault("mint",false)){
            mint();
        }
        LOG.infoEntering("burn fails when value is more than balance");
        Bytes burn = tokenScore.burn(ownerWallet, value.add(BigInteger.TWO));
        assertFailure(txHandler.getResult(burn));
    }

    @Order(15)
    @Test
    public void testStableTokenContractFlow() throws Exception {
        if(!status.getOrDefault("add_and_approve_owner",false)){
            add_and_approve(tokenScore,value);
        }else {
            burn();
        }

        // 1. Add Issuers caller
        LOG.infoEntering("admin add owner as issuer");
        Bytes add = tokenScore.addIssuer(ownerWallet, caller.getAddress());
        assertSuccess(txHandler.getResult(add));

        // 2. Approve Issuers an amount
        LOG.infoEntering("admin approve owner to mint value amount");
        Bytes approve = tokenScore.approve(ownerWallet, caller.getAddress(), value);
        TransactionResult txResult = txHandler.getResult(approve);
        assertSuccess(txResult);

        // verify event log
        tokenScore.approvalLog(txResult,ownerWallet.getAddress(),caller.getAddress(),value);

        // 3. mint some tokens
        LOG.infoEntering("mint amount by caller");
        Bytes mint = tokenScore.mint(caller, value);
        txResult = txHandler.getResult(mint);
        assertSuccess(txResult);
        assertEquals(value, tokenScore.balanceOf(caller.getAddress()));

        // verify event logs
        tokenScore.mintLog(txResult,caller.getAddress(),value);
        tokenScore.whiteListLog(txResult,caller.getAddress(),"whitelist on mint".getBytes());
        tokenScore.transferLog(txResult,ZERO_ADDRESS,caller.getAddress(),value,"mint".getBytes());

        // 4. mint
        LOG.infoEntering("admin approve owner to mint value amount");
        approve = tokenScore.approve(ownerWallet, ownerWallet.getAddress(), value);
        txResult = txHandler.getResult(approve);
        assertSuccess(txResult);

        // verify event log
        tokenScore.approvalLog(txResult,ownerWallet.getAddress(),ownerWallet.getAddress(),value);

        LOG.infoEntering("mint by owner");
        Bytes mintTo = tokenScore.mint(ownerWallet, value);
        txResult = txHandler.getResult(mintTo);
        assertSuccess(txResult);
        assertEquals(value, tokenScore.balanceOf(ownerWallet.getAddress()));

        // verify event logs
        tokenScore.mintLog(txResult,ownerWallet.getAddress(),value);
        tokenScore.transferLog(txResult,ZERO_ADDRESS,ownerWallet.getAddress(),value,"mint".getBytes());

        LOG.infoEntering("owner address is whitelisted");
        assertEquals(true, tokenScore.isWhitelisted(ownerWallet.getAddress()));

        // 4. burn half tokens of caller
        LOG.infoEntering("burn from caller");
        Bytes burn = tokenScore.burn(caller, value.divide(BigInteger.TWO));
        txResult = txHandler.getResult(burn);
        assertSuccess(txResult);
        assertEquals((value.divide(BigInteger.TWO)), tokenScore.balanceOf(caller.getAddress()));

        //verify event logs
        tokenScore.burnLog(txResult,caller.getAddress(),value.divide(BigInteger.TWO));
        tokenScore.transferLog(txResult,caller.getAddress(),ZERO_ADDRESS,value.divide(BigInteger.TWO),"burn".getBytes());

        // 5. transfer half tokens from owner to caller
        LOG.infoEntering("transfer from owner to caller");
        Bytes transfer = tokenScore.transfer(ownerWallet, caller.getAddress(), value.divide(BigInteger.TWO),
                "transfer".getBytes());
        txResult = txHandler.getResult(transfer);
        assertSuccess(txResult);
        assertEquals(value, tokenScore.balanceOf(caller.getAddress()));

        //verify event logs
        tokenScore.transferLog(txResult,ownerWallet.getAddress(),caller.getAddress(),value.divide(BigInteger.TWO),
                "transfer".getBytes());

    }

    @Test
    @Order(16)
    public void check_fee_sharing() throws Exception {
        if(!status.getOrDefault("add_and_approve_owner",false)){
            add_and_approve(tokenScore,value);
        }
        if(!status.getOrDefault("mint",false)){
            mint();
        }

        BigInteger depositAmount = ICX.multiply(BigInteger.valueOf(5000));

        Bytes loadIcx = txHandler.transfer(ownerWallet.getAddress(), depositAmount);
        assertSuccess(txHandler.getResult(loadIcx));

        // deposit 5000 ICX to Score
        Bytes depositICX = txHandler.depositICX(ownerWallet,tokenScore.getAddress(),depositAmount,null);
        assertSuccess(txHandler.getResult(depositICX));

        assertTrue(tokenScore.isWhitelisted(ownerWallet.getAddress()));

        BigInteger balance_owner_before = txHandler.getBalance(ownerWallet.getAddress());
        Bytes transfer = tokenScore.transfer(ownerWallet,caller.getAddress(),BigInteger.TEN,"transfer".getBytes());
        assertSuccess(txHandler.getResult(transfer));

        BigInteger balance_owner_after = txHandler.getBalance(ownerWallet.getAddress());
        assertEquals(balance_owner_before,balance_owner_after);
    }

    private void add_and_approve(StableCoinScore tokenScore, BigInteger value) throws IOException, ResultTimeoutException {
        LOG.infoEntering("admin add owner as issuer");
        Bytes add = tokenScore.addIssuer(ownerWallet, ownerWallet.getAddress());
        assertSuccess(txHandler.getResult(add));

        LOG.infoEntering("admin approve owner to mint value amount");
        Bytes approve = tokenScore.approve(ownerWallet, ownerWallet.getAddress(), value);
        assertSuccess(txHandler.getResult(approve));

        status.put("add_and_approve_owner",true);
    }

    private void burn() throws IOException, ResultTimeoutException {
        Bytes burn = tokenScore.burn(ownerWallet, tokenScore.balanceOf(ownerWallet.getAddress()));
        assertSuccess(txHandler.getResult(burn));
    }

    private void mint() throws IOException, ResultTimeoutException {
        Bytes mint = tokenScore.mint(ownerWallet, value);
        assertSuccess(txHandler.getResult(mint));
        status.put("mint",true);
    }

}
