package com.icon.score;

import score.Address;
import score.Context;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static score.Context.require;

public class StableCoin extends AbstractStableCoin {

    /**
     * Variable Initialization
     *
     * @param _name     The name of the token.
     * @param _symbol   The symbol of the token.
     * @param _decimals The number of decimals. Set to 18 by default.
     * @param _admin    The admin for the token.
     * @param _nIssuers Maximum number of issuers.
     */
    public StableCoin(String _name, String _symbol, BigInteger _decimals, Address _admin, @Optional int _nIssuers) {
        super(_name, _symbol, _decimals, _admin, _nIssuers);
    }

    /**
     * @return name of the token
     */
    @External(readonly = true)
    public String name() {
        return name.get();
    }

    /**
     * @return symbol of the token
     */
    @External(readonly = true)
    public String symbol() {
        return symbol.get();
    }

    /**
     * @return number of decimals
     */
    @External(readonly = true)
    public BigInteger decimals() {
        return decimals.get();
    }

    @External(readonly = true)
    public BigInteger totalSupply() {
        return this.totalSupply.getOrDefault(BigInteger.ZERO);
    }

    /**
     * @param _owner The account whose balance is to be checked.
     * @return Amount of tokens owned by the `_owner` with the given address.
     */
    @External(readonly = true)
    public BigInteger balanceOf(Address _owner) {
        return _balances.getOrDefault(_owner, BigInteger.ZERO);
    }

    /**
     * @return the wallet address of admin.
     */
    @External(readonly = true)
    public Address getAdmin() {
        return admin.get();
    }

    /**
     * @return list of all the issuers.
     */
    @External(readonly = true)
    public List<Address> getIssuers() {
        List<Address> issuersList = new ArrayList<>();
        for (int i = 0; i < issuers.size(); i++) {
            issuersList.add(issuers.get(i));
        }
        return issuersList;
    }

    /**
     * @return if the score is paused
     */
    @External(readonly = true)
    public boolean isPaused() {
        return _paused.get();
    }

    /**
     * @param _issuer The wallet address of issuer
     * @return amount of tokens that `_issuer` can mint at this point in time.
     */
    @External(readonly = true)
    public BigInteger issuerAllowance(Address _issuer) {
        return _allowances.getOrDefault(_issuer, BigInteger.ZERO);
    }

    /**
     * @return daily free transaction limit
     */
    @External(readonly = true)
    public BigInteger freeDailyTxLimit() {

        return freeDailyTLimit.get();
    }

    /**
     * @param _owner The account at which remaining free transaction is to be queried
     * @return number of free transactions left for `_owner`
     */
    @External(readonly = true)
    public BigInteger remainingFreeTxThisTerm(Address _owner) {

        if (_whitelist.at(_owner).get("free_tx_start_height")!=null) {
            if (_whitelist.at(_owner).get("free_tx_start_height").add(TERM_LENGTH).compareTo(BigInteger.valueOf
                    (Context.getBlockHeight())) < 0) {
                return freeDailyTLimit.get();
            } else {
                return freeDailyTLimit.get().add(_whitelist.at(_owner).get("free_tx_count_since_start"));
            }
        }
        return BigInteger.ZERO;
    }

    /**
     * @param _owner The account to check if it is whitelisted
     * @return if wallet address is whitelisted.
     */
    @External(readonly = true)
    public boolean isWhitelisted(Address _owner) {
        return _whitelist.at(_owner).get("free_tx_start_height")!=null;
    }


    /**
     * Transfers certain amount of tokens from sender to the receiver.
     *
     * @param _to    The account to which the token is to be transferred.
     * @param _value The no. of tokens to be transferred.
     * @param _data  Any information or message
     */
    @External
    public void transfer(Address _to, BigInteger _value, @Optional byte[] _data) {

        setFeeSharingPercentage();

        if (_data == null) {
            _data = new byte[0];
        }
        _transfer(Context.getCaller(), _to, _value, _data);
    }

    @External
    /**
     * Changes daily free transactions limit for whitelisted users
     * Only admin can call this method
     * @param _new_limit
     */
    public void changeFreeDailyTxLimit(BigInteger _new_limit) {

        require(_new_limit.compareTo(BigInteger.ZERO) >= 0, "Free daily transaction limit cannot be under 0.");
        onlyAdmin("Only admin can change free daily transaction limit");

        freeDailyTLimit.set(_new_limit);
    }

    /**
     * Add issuers. Issuers can mint and burn tokens.
     * Only admin can call this method.
     *
     * @param _issuer The wallet address of issuer to be added
     */
    @External
    public void addIssuer(Address _issuer) {
        require(!isIssuer(_issuer), _issuer + " is already an issuer");
        onlyAdmin("Only admin can add issuer");
        require(issuers.size() < nIssuers.get(), "Cannot have more than " + nIssuers.get() + " issuers");
        issuers.add(_issuer);
    }

    /**
     * Remove issuer from the list of issuers.
     * Sets the allowance of `_issuer` to zero.
     * Only admin can call this method.
     *
     * @param _issuer The wallet of address of issuer to remove
     */
    @External
    public void removeIssuer(Address _issuer) {
        onlyAdmin("Only admin can remove issuer");
        require(isIssuer(_issuer), _issuer + " not an issuer");

        Address top = issuers.pop();

        if (top != _issuer) {
            for (int i = 0; i < issuers.size(); i++) {
                if (issuers.get(i) == _issuer) {
                    issuers.set(i, top);
                }
            }
        }
        _allowances.set(_issuer, BigInteger.ZERO);
    }


    /**
     * Allow `_issuer` to mint `_value` tokens.
     *
     * @param _issuer The issuer to approve to.
     * @param _value  The amount to approve to issuer to mint.
     */
    @External
    public void approve(Address _issuer, BigInteger _value) {
        onlyAdmin("Only admin can approve amount to issuer");
        require(isIssuer(_issuer), "Only issuers can be approved");
        _allowances.set(_issuer, _value);
        Approval(Context.getCaller(), _issuer, _value);
    }

    /**
     * Transfer the admin rights to another `_newAdmin` address
     * Only admin can call this method.
     *
     * @param _newAdmin New wallet address that will now have admin rights
     */
    @External
    public void transferAdminRight(Address _newAdmin) {

        onlyAdmin("Only admin can transfer their admin right");
        admin.set(_newAdmin);
    }

    /**
     * Toggles pause status of the score.
     * Only admin can call this method.
     */
    @External
    public void togglePause() {
        onlyAdmin("Only admin can toggle pause");
        _paused.set(!isPaused());
    }

    /**
     * Creates `_value` number of tokens, and assigns to caller account.
     * Increases the balance of that account and total supply.
     * Only issuers can call ths method.
     *
     * @param _value Number of tokens to be created at the account.
     */
    @External
    public void mint(BigInteger _value) {
        _mint(Context.getCaller(), _value);
    }

    /**
     * Creates `_value` number of tokens, and assigns to `_to`.
     * Increases the balance of that account and total supply.
     * Only issuers can call ths method.
     *
     * @param _to    The account at which token is to be created.
     * @param _value Number of tokens to be minted
     */
    @External
    public void mintTo(Address _to, BigInteger _value) {
        _mint(_to, _value);
    }

    /**
     * Destroys `_value` number of tokens from the caller account.
     * Decreases the balance of that account and total supply.
     *
     * @param _value Number of tokens to be destroyed.
     */
    @External
    public void burn(BigInteger _value) {
        _burn(Context.getCaller(), _value);
    }
}
