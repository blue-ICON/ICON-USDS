package com.icon.score;

import score.Address;
import score.ArrayDB;
import score.BranchDB;
import score.Context;

import java.math.BigInteger;

import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;

import static score.Context.getBlockHeight;
import static score.Context.require;

public abstract class AbstractStableCoin implements IRC2Base {
    protected final String TAG = "StableCoin";
    protected final BigInteger TERM_LENGTH = BigInteger.valueOf(43120);
    protected final Address EOA_ZERO = new Address(new byte[21]);

    protected final VarDB<String> name = Context.newVarDB("_name", String.class);
    protected final VarDB<String> symbol = Context.newVarDB("_symbol", String.class);
    protected final VarDB<BigInteger> decimals = Context.newVarDB("decimals", BigInteger.class);

    protected final VarDB<Address> admin = Context.newVarDB("admin", Address.class);
    protected final VarDB<BigInteger> nIssuers = Context.newVarDB("number_of_issuers", BigInteger.class);
    protected final ArrayDB<Address> issuers = Context.newArrayDB("issuers", Address.class);

    protected final VarDB<BigInteger> totalSupply = Context.newVarDB("total_supply", BigInteger.class);
    protected final DictDB<Address, BigInteger> _balances = Context.newDictDB("balances", BigInteger.class);
    protected final DictDB<Address, BigInteger> _allowances = Context.newDictDB("allowances", BigInteger.class);
    protected final VarDB<Boolean> _paused = Context.newVarDB("paused", Boolean.class);

    protected final BranchDB<Address, DictDB<String, BigInteger>> _whitelist = Context.newBranchDB("whitelist", BigInteger.class);
    protected final VarDB<BigInteger> freeDailyTxLimit = Context.newVarDB("free_daily_tx_limit", BigInteger.class);

    public static final String START_HEIGHT = "free_tx_start_height";
    public static final String TXN_COUNT = "free_tx_count_since_start";


    @EventLog(indexed = 3)
    public void Transfer(Address _from, Address _to, BigInteger _value, byte[] _data) {
    }

    @EventLog(indexed = 1)
    public void Mint(Address _to, BigInteger _value) {
    }

    @EventLog(indexed = 1)
    public void Burn(Address _from, BigInteger _value) {
    }

    @EventLog(indexed = 2)
    public void Approval(Address _from, Address _to, BigInteger _value) {
    }

    @EventLog(indexed = 2)
    public void WhitelistWallet(Address _to, byte[] _data) {
    }

    public AbstractStableCoin() {}

    protected void onlyAdmin(String msg) {
        require(Context.getCaller().equals(admin.get()), msg);
    }

    protected boolean isIssuer(Address issuer) {
        int size = issuers.size();
        for (int i = 0; i < size; i++) {
            if (issuer.equals(issuers.get(i))) {
                return true;
            }
        }
        return false;
    }


    protected void setFeeSharingPercentage() {
        Address user = Context.getCaller();
        BigInteger currentBlockHeight = BigInteger.valueOf(getBlockHeight());
        DictDB<String, BigInteger> userFeeSharing = _whitelist.at(user);
        if (userFeeSharing.get(START_HEIGHT) == null) {
            userFeeSharing.set(START_HEIGHT, currentBlockHeight);
        }
        if (userFeeSharing.get(START_HEIGHT).add(TERM_LENGTH).compareTo(currentBlockHeight) > 0) {
            BigInteger count = userFeeSharing.getOrDefault(TXN_COUNT, BigInteger.ZERO);
            if (count.compareTo(freeDailyTxLimit.get()) < 0) {
                userFeeSharing.set(TXN_COUNT, count.add(BigInteger.ONE));
                Context.setFeeSharingProportion(100);
            }
        } else {
            userFeeSharing.set(START_HEIGHT, currentBlockHeight);
            userFeeSharing.set(TXN_COUNT, BigInteger.ONE);
            Context.setFeeSharingProportion(100);
        }
    }


    /**
     * Transfers certain amount of tokens from `_from` to `_to`.
     * This is an internal function.
     *
     * @param _from  The account from which the token is to be transferred.
     * @param _to    The account to which the token is to be transferred.
     * @param _value The no. of tokens to be transferred.
     * @param _data  Any information or message
     */
    protected void _transfer(Address _from, Address _to, BigInteger _value, byte[] _data) {

        require(_value.compareTo(BigInteger.ZERO) > 0, "Cannot transfer zero or less");
        require(balanceOf(_from).compareTo(_value) >= 0, "Insufficient Balance");
        require(!_to.equals(EOA_ZERO), "Cannot transfer to zero address");
        require(!_paused.get(), "Cannot transfer when paused");

        _balances.set(_from, balanceOf(_from).subtract(_value));
        _balances.set(_to, balanceOf(_to).add(_value));

        if (_data == null) {
            _data = "None".getBytes();
        }

        if (_to.isContract()) {
            Context.call(_to, "tokenFallback", _from, _value, _data);
        }
        //Emits an event log `Transfer`
        Transfer(_from, _to, _value, _data);
    }

    /**
     * Mints `_value` tokens at `_to` address.
     * Internal Function
     *
     * @param _to    The account at which token is to be minted.
     * @param _value Number of tokens to be minted at the account.
     */
    protected void _mint(Address _to, BigInteger _value) {
        Address issuer = Context.getCaller();
        require(!_to.equals(EOA_ZERO), "Cannot mint to zero address");
        require(_value.compareTo(BigInteger.ZERO) > 0, "Amount to mint should be greater than zero");
        require(isIssuer(issuer), "Only issuers can mint");
        require(!_paused.get(), "Cannot mint when paused");

        BigInteger value = _allowances.getOrDefault(issuer, BigInteger.ZERO).subtract(_value);
        _allowances.set(issuer, value);
        require(_allowances.getOrDefault(issuer, BigInteger.ZERO).compareTo(BigInteger.ZERO) >= 0,
                "Allowance amount to mint exceed");

        _whitelistWallet(_to, "whitelist on mint".getBytes());

        totalSupply.set(totalSupply().add(_value));
        _balances.set(_to, _balances.getOrDefault(_to, BigInteger.ZERO).add(_value));

        Transfer(EOA_ZERO, _to, _value, "mint".getBytes());
        Mint(_to, _value);
    }

    /**
     * Burns `_value` amount of tokens from `_from` address.
     * Internal Function
     *
     * @param _from  The account at which token is to be destroyed.
     * @param _value Number of tokens to be destroyed at the `_from`.
     */
    protected void _burn(Address _from, BigInteger _value) {
        require(!_from.equals(EOA_ZERO), "Cannot burn from zero address");
        require(_value.compareTo(BigInteger.ZERO) > 0, "Amount to burn should be greater than zero");
        require(_balances.getOrDefault(_from, BigInteger.ZERO).compareTo(_value) >= 0,
                "Insufficient balance to burn");
        require(!_paused.get(), "Cannot burn when paused");

        totalSupply.set(totalSupply().subtract(_value));
        _balances.set(_from, _balances.getOrDefault(_from, BigInteger.ZERO).subtract(_value));

        Transfer(_from, EOA_ZERO, _value, "burn".getBytes());
        Burn(_from, _value);
    }

    /**
     * Whitelist `_to` address
     *
     * @param _to   Address to whitelist
     * @param _data Data in bytes
     */
    protected void _whitelistWallet(Address _to, byte[] _data) {
        require(!_to.equals(EOA_ZERO) , "Can not whitelist zero wallet address");

        if (_whitelist.at(_to).get(START_HEIGHT) == null) {
            _whitelist.at(_to).set(START_HEIGHT, BigInteger.valueOf(getBlockHeight()));
            _whitelist.at(_to).set(TXN_COUNT, BigInteger.ONE);

            WhitelistWallet(_to, _data);
        }
    }

}
