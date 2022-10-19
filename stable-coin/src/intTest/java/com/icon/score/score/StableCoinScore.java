
package com.icon.score.score;

import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.jsonrpc.RpcItem;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.test.ResultTimeoutException;
import foundation.icon.test.TransactionFailureException;
import foundation.icon.test.TransactionHandler;
import foundation.icon.test.score.Score;
import score.annotation.Optional;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static foundation.icon.test.Env.LOG;

public class StableCoinScore extends Score {
    public StableCoinScore(Score other) {
        super(other);
    }

    public static StableCoinScore mustDeploy(TransactionHandler txHandler, Wallet owner)
            throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "StableCoin");
        RpcObject params = new RpcObject.Builder()
                .put("_name", new RpcValue("Stable Token"))
                .put("_symbol", new RpcValue("STO"))
                .put("_decimals", new RpcValue("18"))
                .put("_admin", new RpcValue(owner.getAddress()))
                .put("_nIssuers", new RpcValue("2"))
                .build();
        Score score = txHandler.deploy(owner, getFilePath("stable-coin"), params);
        LOG.info("scoreAddr = " + score.getAddress());
        LOG.info("owner = " + owner.getAddress());
        LOG.infoExiting();
        return new StableCoinScore(score);
    }

    public String name() throws IOException {
        return call("name",null).asString();
    }

    public String symbol() throws IOException {
        return call("symbol",null).asString();
    }

    public Address admin() throws IOException {
        return call("getAdmin",null).asAddress();
    }

    public BigInteger decimals() throws IOException {
        return call("decimals",null).asInteger();
    }

    public BigInteger freeDailyTxLimit() throws IOException {
        return call("freeDailyTxLimit",null).asInteger();
    }

    public BigInteger remainingFreeTxThisTerm(Address _owner) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_owner", new RpcValue(_owner))
                .build();
        return call("remainingFreeTxThisTerm",params).asInteger();
    }
    public Boolean isWhitelisted(Address _owner) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_owner", new RpcValue(_owner))
                .build();
        return call("isWhitelisted",params).asBoolean();
    }

    public List<RpcItem> getIssuers() throws IOException {
        RpcItem issuers = call("getIssuers",null);
        return issuers.asArray().asList();
    }

    public Bytes transfer(Wallet wallet, Address _to, BigInteger _value, @Optional byte[] _data) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_to", new RpcValue(_to))
                .put("_value", new RpcValue(_value))
                .put("_data", new RpcValue(_data))
                .build();
        return invoke(wallet, "transfer", params);
    }

    public Bytes changeFreeDailyTxLimit(Wallet wallet, BigInteger _new_limit) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_new_limit", new RpcValue(_new_limit))
                .build();
        return invoke(wallet, "changeFreeDailyTxLimit", params);
    }

    public Bytes addIssuer(Wallet wallet, Address _issuer) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_issuer", new RpcValue(_issuer))
                .build();
        return invoke(wallet, "addIssuer", params);
    }

    public Bytes removeIssuer(Wallet wallet, Address _issuer) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_issuer", new RpcValue(_issuer))
                .build();
        return invoke(wallet, "removeIssuer", params);
    }

    public Bytes approve(Wallet wallet, Address _issuer, BigInteger _value) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_issuer", new RpcValue(_issuer))
                .put("_value", new RpcValue(_value))
                .build();
        return invoke(wallet, "approve", params);
    }

    public Bytes transferAdminRight(Wallet wallet, Address _newAdmin) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_newAdmin", new RpcValue(_newAdmin))
                .build();
        return invoke(wallet, "transferAdminRight", params);
    }

    public Bytes togglePause(Wallet wallet) throws IOException {
        return invoke(wallet, "togglePause", null);
    }

    public Bytes mint(Wallet wallet, BigInteger _value) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_value", new RpcValue(_value))
                .build();
        return invoke(wallet, "mint", params);
    }

    public Bytes mintTo(Wallet wallet, Address _to, BigInteger _value) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_to", new RpcValue(_to))
                .put("_value", new RpcValue(_value))
                .build();
        return invoke(wallet, "mintTo", params);
    }

    public Bytes burn(Wallet wallet, BigInteger _value) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_value", new RpcValue(_value))
                .build();
        return invoke(wallet, "burn", params);
    }

    public BigInteger balanceOf(Address owner) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_owner", new RpcValue(owner))
                .build();
        return call("balanceOf",params).asInteger();
    }

    public BigInteger totalSupply() throws IOException {
        return call("totalSupply",null).asInteger();
    }

    public void approvalLog(TransactionResult result, Address from, Address to , BigInteger value) throws IOException{
        TransactionResult.EventLog eventLog = findEventLog(result,getAddress(),
                "Approval(Address,Address,int)");
        if (eventLog !=null){
            Address _from = eventLog.getIndexed().get(1).asAddress();
            Address _to = eventLog.getIndexed().get(2).asAddress();
            BigInteger _value = eventLog.getData().get(0).asInteger();
            if (from.equals(_from) && to.equals(_to) && value.equals(_value)){
                return;
            }
        }
        throw new IOException("Failed to get approved.");
    }

    public void mintLog(TransactionResult result, Address to , BigInteger value)throws IOException{
        TransactionResult.EventLog eventLog = findEventLog(result,getAddress(),
                "Mint(Address,int)");
        if (eventLog !=null){
            Address _to = eventLog.getIndexed().get(1).asAddress();
            BigInteger _value = eventLog.getData().get(0).asInteger();
            if (to.equals(_to) && value.equals(_value)){
                return;
            }
        }
        throw new IOException("Failed to mint tokens.");
    }
    public void whiteListLog(TransactionResult result, Address to, byte[] data)throws IOException{
        TransactionResult.EventLog eventLog = findEventLog(result,getAddress(),
                "WhitelistWallet(Address,bytes)");
        if (eventLog !=null){
            Address _to = eventLog.getIndexed().get(1).asAddress();
            byte[] _bytes = eventLog.getIndexed().get(2).asByteArray();
            if (to.equals(_to) && Arrays.equals(data, _bytes)){
                return;
            }
        }
        throw new IOException("Failed to whitelist wallet.");
    }

    public void transferLog(TransactionResult result,Address from, Address to, BigInteger value, byte[] data)throws IOException{
        TransactionResult.EventLog eventLog = findEventLog(result,getAddress(),
                "Transfer(Address,Address,int,bytes)");
        if (eventLog !=null){
            Address _from = eventLog.getIndexed().get(1).asAddress();
            Address _to = eventLog.getIndexed().get(2).asAddress();
            BigInteger _value = eventLog.getIndexed().get(3).asInteger();
            byte[] _bytes = eventLog.getData().get(0).asByteArray();

            if (from.equals(_from) && to.equals(_to) && value.equals(_value) && Arrays.equals(data, _bytes) ){
                return;
            }
        }
        throw new IOException("Failed to transfer tokens.");
    }

    public void burnLog(TransactionResult result,Address from,BigInteger value)throws IOException{
        TransactionResult.EventLog eventLog = findEventLog(result,getAddress(),
                "Burn(Address,int)");
        if (eventLog !=null){
            Address _from = eventLog.getIndexed().get(1).asAddress();
            BigInteger _value = eventLog.getData().get(0).asInteger();

            if (from.equals(_from) && value.equals(_value) ){
                return;
            }
        }
        throw new IOException("Failed to burn tokens.");
    }

    public void depositForFeeSharing(){

    }
}

