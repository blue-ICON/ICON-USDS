package com.icon.score;

import score.Address;
import score.ArrayDB;
import score.BranchDB;
import score.Context;

import java.math.BigInteger;

import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

import static score.Context.getBlockHeight;
import static score.Context.require;

public abstract class AbstractStableCoin implements IRC2Base {
    protected final String TAG = "StableCoin";
    protected final BigInteger TERM_LENGTH = BigInteger.valueOf(43120);
    protected final Address EOA_ZERO = Address.fromString("hx0000000000000000000000000000000000000000");

    protected final VarDB<String> name = Context.newVarDB("name", String.class);
    protected final VarDB<String> symbol = Context.newVarDB("symbol", String.class);
    protected final VarDB<BigInteger> decimals = Context.newVarDB("decimals", BigInteger.class);
    protected final VarDB<Address> admin = Context.newVarDB("admin", Address.class);
    protected final VarDB<Integer> nIssuers = Context.newVarDB("nIssuers", Integer.class);
    protected final ArrayDB<Address> issuers = Context.newArrayDB("issuers", Address.class);
    protected final VarDB<BigInteger> totalSupply = Context.newVarDB("totalSupply", BigInteger.class);
    protected final VarDB<Boolean> _paused = Context.newVarDB("paused", Boolean.class);
    protected final VarDB<BigInteger> freeDailyTLimit = Context.newVarDB("freeDailyTLimit", BigInteger.class);
    protected final DictDB<Address, BigInteger> _balances = Context.newDictDB("_balances", BigInteger.class);
    protected final DictDB<Address, BigInteger> _allowances = Context.newDictDB("_allowances", BigInteger.class);
    protected final BranchDB<Address, DictDB<String, BigInteger>> _whitelist = Context.newBranchDB("_whitelist", BigInteger.class);

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

    public AbstractStableCoin(String _name, String _symbol, BigInteger _decimals, Address _admin, @Optional int _nIssuers) {

        if (name.get()==null){
            if (_nIssuers == 0) {
                _nIssuers = 2;
            }
            require(_name.length() > 0, "Invalid Token Name");
            require(_symbol.length() > 0, "Invalid Token Symbol Name");
            require(_decimals.compareTo(BigInteger.ZERO) > 0, "Decimals cannot be less than 0");
            require(_nIssuers > 0, "1 or more issuers required");

            this.name.set(_name);
            this.symbol.set(_symbol);
            this.decimals.set(_decimals);
            this.admin.set(_admin);
            this.nIssuers.set(_nIssuers);
            this.totalSupply.set(BigInteger.ZERO);
            this._paused.set(false);
            this.freeDailyTLimit.set(BigInteger.valueOf(50));
        }

    }

    protected void onlyAdmin(String msg) {
        require(Context.getCaller() == admin.get(), msg);
    }

    protected boolean isIssuer(Address issuer) {
        for (int i = 0; i < issuers.size(); i++) {
            if (issuer == issuers.get(i)) {
                return true;
            }
        }
        return false;
    }

//    protected void setFeeSharingPercentage(){
//        if (!_whitelist.at(Context.getCaller()).get("free_tx_start_height").equals(BigInteger.ZERO)){
//            _whitelist.at(Context.getCaller()).set("free_tx_start_height",BigInteger.valueOf(Context.getBlockHeight()));
//            _whitelist.at(Context.getCaller()).set("free_tx_count_since_start",BigInteger.ONE);
//            Context.setFeeSharingProportion(100);
//
//        } else if (_whitelist.at(Context.getCaller()).get("free_tx_count_since_start").add(BigInteger.ONE).compareTo
//                (freeDailyTLimit.get())<=0) {
//            BigInteger newVal = _whitelist.at(Context.getCaller()).get("free_tx_count_since_start").add(BigInteger.ONE);
//            _whitelist.at(Context.getCaller()).set("free_tx_count_since_start",newVal);
//            Context.setFeeSharingProportion(100);
//        }
//    }

    protected void setFeeSharingPercentage() {
        Address user = Context.getCaller();
        BigInteger currentBlockHeight = BigInteger.valueOf(getBlockHeight());
        DictDB<String, BigInteger> userFeeSharing = _whitelist.at(user);
        if (userFeeSharing.get(START_HEIGHT) == null) {
            userFeeSharing.set(START_HEIGHT, currentBlockHeight);
        }
        if (userFeeSharing.get(START_HEIGHT).add(TERM_LENGTH).compareTo(currentBlockHeight) > 0) {
            BigInteger count = userFeeSharing.getOrDefault(TXN_COUNT, BigInteger.ZERO);
            if (count.compareTo(freeDailyTLimit.get()) < 0) {
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

        require(!_to.equals(EOA_ZERO), "Cannot transfer to zero address");
        require(_value.compareTo(BigInteger.ZERO) > 0, "Cannot transfer zero or less");
        require(_balances.getOrDefault(_from,BigInteger.ZERO).compareTo(_value) >= 0, "Insufficient Balance");
        require(!_paused.get(), "Cannot transfer when paused");

        _balances.set(_from, _balances.getOrDefault(_from,BigInteger.ZERO).subtract(_value));
        _balances.set(_to, _balances.getOrDefault(_to,BigInteger.ZERO).add(_value));

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
        require(!_to.equals(EOA_ZERO), "Cannot mint to zero address");
        require(_value.compareTo(BigInteger.ZERO) > 0, "Amount to mint should be greater than zero");
        require(isIssuer(Context.getCaller()), "Only issuers can mint");
        require(!_paused.get(), "Cannot mint when paused");

        BigInteger value = _allowances.getOrDefault(Context.getCaller(),BigInteger.ZERO).subtract(_value);
        _allowances.set(Context.getCaller(), value);
        require(_allowances.getOrDefault(Context.getCaller(),BigInteger.ZERO).compareTo(BigInteger.ZERO) >= 0,
                "Allowance amount to mint exceed");

        _whitelistWallet(_to, "whitelist on mint".getBytes());

        totalSupply.set(totalSupply.get().add(_value));
        _balances.set(_to, _balances.getOrDefault(_to,BigInteger.ZERO).add(_value));

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
        require(_balances.getOrDefault(_from,BigInteger.ZERO).compareTo(_value) >= 0,
                "Insufficient balance to burn");
        require(!_paused.get(), "Cannot burn when paused");

        totalSupply.set(totalSupply.getOrDefault(BigInteger.ZERO).subtract(_value));
        _balances.set(_from, _balances.getOrDefault(_from,BigInteger.ZERO).subtract(_value));

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
        require(_to != EOA_ZERO, "Can not whitelist zero wallet address");

        if (_whitelist.at(_to).get("free_tx_start_height") == null) {
            _whitelist.at(_to).set("free_tx_start_height", BigInteger.valueOf(getBlockHeight()));
            _whitelist.at(_to).set("free_tx_count_since_start", BigInteger.ONE);

            WhitelistWallet(_to, _data);
        }
    }

}
