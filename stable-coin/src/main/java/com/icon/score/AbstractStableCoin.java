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

import static score.Context.getBlockHeight;
import static score.Context.require;

public abstract class AbstractStableCoin {
    protected final String TAG = "StableCoin";
    protected final Integer TERM_LENGTH = 43120;
    protected final Address EOA_ZERO = Address.fromString("hx" + '0' * 40);

    protected String name;
    protected String symbol;
    protected final VarDB<BigInteger> decimals = Context.newVarDB("decimals", BigInteger.class);
    protected final  VarDB<Address> admin = Context.newVarDB("admin", Address.class);
    protected final  VarDB<Integer> nIssuers = Context.newVarDB("nIssuers", Integer.class);
    protected final ArrayDB<Address> issuers = Context.newArrayDB("issuers", Address.class);
    protected final  VarDB<BigInteger> totalSupply = Context.newVarDB("totalSupply", BigInteger.class);
    protected final  VarDB<Boolean> _paused = Context.newVarDB("paused", Boolean.class);
    protected final  VarDB<BigInteger> freeDailyTLimit = Context.newVarDB("freeDailyTLimit", BigInteger.class);
    protected final DictDB<Address, BigInteger> _balances = Context.newDictDB("_balances", BigInteger.class);
    protected final DictDB<Address, BigInteger> _allowances = Context.newDictDB("_allowances", BigInteger.class);
    protected final DictDB<Address, Boolean> isIssuer = Context.newDictDB("isIssuer", Boolean.class);
    protected final BranchDB<Address,DictDB<String,Integer>> _whitelist = Context.newBranchDB("_whitelist", Integer.class);


    @EventLog(indexed = 3)
    public void Transfer(Address _from,  Address _to, BigInteger _value, byte[] _data) {}

    @EventLog(indexed = 1)
    public void Mint(Address _to, BigInteger _value) {}

    @EventLog(indexed = 1)
    public void Burn(Address _from, BigInteger _value) {}

    @EventLog(indexed = 2)
    public void Approval(Address _from,  Address _to, BigInteger _value) {}

    @EventLog(indexed = 2)
    public void WhitelistWallet(Address _to, byte[] _data) {}

    protected void _transfer(Address _from, Address _to, BigInteger _value, byte[] _data) {
    /*
        Transfers certain amount of tokens from `_from` to `_to`.
        This is an internal function.
        :param _from: The account from which the token is to be transferred.
        :param _to: The account to which the token is to be transferred.
        :param _value: The no. of tokens to be transferred.
        :param _data: Any information or message
     */
        require(_to!=EOA_ZERO,"Cannot transfer to zero address");
        require(_value.compareTo(BigInteger.ZERO)> 0, "Cannot transfer zero or less");
        require(_balances.get(_from).compareTo(_value)>=0, "Insufficient Balance");
        require(!_paused.get(), "Cannot transfer when paused");

        _balances.set(_from,_balances.get(_from).subtract(_value));
        _balances.set(_to,_balances.get(_to).add(_value));

        String data = new String(_data);

        if (data == null){
            data = "None";
        }

//        if _to.is_contract
//
//        //If the recipient is SCORE, then calls `tokenFallback` to hand over control.
//        recipient_score = create_interface_score(_to, TokenFallbackInterface)
//        recipient_score.tokenFallback(_from, _value, _data)

        //Emits an event log `Transfer`
        Transfer(_from, _to, _value, _data);
    }

    protected void _mint(Address _to, BigInteger _value) {
        require(_to!=EOA_ZERO,"Cannot mint to zero address");
        require(_value.compareTo(BigInteger.ZERO)> 0, "Amount to mint should be greater than zero");
        require(!_paused.get(), "Cannot burn when paused");
        Context.require(isIssuer.get(Context.getCaller()),"Only issuers can mint");

        BigInteger value = _allowances.get(Context.getCaller()).subtract(_value);
        _allowances.set(Context.getCaller(),value);
        require(_allowances.get(Context.getCaller()).compareTo(BigInteger.ZERO) >= 0,
                "Allowance amount to mint exceed");

        _whitelistWallet(_to, "whitelist on mint".getBytes());

        totalSupply.set(totalSupply.get().add(_value));
        _balances.set(_to,_balances.get(_to).add(_value));

        Transfer(EOA_ZERO, _to, _value, "mint".getBytes());
        Mint(_to, _value);
    }

    protected void _burn(Address _from, BigInteger _value){
        require(_from!=EOA_ZERO,"Cannot burn from zero address");
        require(_value.compareTo(BigInteger.ZERO)> 0, "Amount to burn should be greater than zero");
        require(_balances.get(_from).compareTo(_value)>=0, "Insufficient balance to burn");
        require(!_paused.get(), "Cannot burn when paused");

        totalSupply.set(totalSupply.get().subtract(_value));
        _balances.set(_from,_balances.get(_from).subtract(_value));

        Transfer(_from, EOA_ZERO, _value, "burn".getBytes());
        Burn(_from, _value);
    }

    protected void _whitelistWallet(Address _to, byte[] _data){
        require(_to!=EOA_ZERO,"Can not whitelist zero wallet address");

        if (_whitelist.at(_to).get("free_tx_start_height") == null){
            _whitelist.at(_to).set("free_tx_start_height", (int) getBlockHeight());
            _whitelist.at(_to).set("free_tx_start_height", 1);
        }
    }
    public <K> K call(Class<K> kClass, Address contract, String method, Object... params) {
        return Context.call(kClass, contract, method, params);
    }

}
