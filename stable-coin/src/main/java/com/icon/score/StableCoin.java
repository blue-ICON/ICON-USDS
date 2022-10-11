package com.icon.score;

import score.Address;
import score.Context;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;
import java.util.List;

import static score.Context.require;

public class StableCoin extends AbstractStableCoin {

    /**
     *Variable Initialization
     *
     * @param name The name of the token.
     * @param symbol The symbol of the token.
     * @param decimals The number of decimals. Set to 18 by default.
     * @param admin The admin for the token.
     * @param nIssuers Maximum number of issuers.
     */
    public StableCoin(String name, String symbol, BigInteger decimals, Address admin, @Optional int nIssuers) {
        super();

        if (name==null){
            if (nIssuers==0){
                nIssuers =2;
            }
            require(name.length()>0,"Invalid Token Name");
            require(symbol.length() > 0, "Invalid Token Symbol Name");
            require(decimals.compareTo(BigInteger.ZERO) > 0, "Decimals cannot be less than 0");
            require(nIssuers > 0, "1 or more issuers required");

            this.name = name;
            this.symbol = symbol;
            this.decimals.set(decimals);
            this.admin.set(admin);
            this.nIssuers.set(nIssuers);
            this.totalSupply.set(BigInteger.ZERO);
            this._paused.set(false);
            this.freeDailyTLimit.set(BigInteger.valueOf(50));
        }
    }

    /**
     *
     * @return name of the token
     */
    @External(readonly=true)
    public String name(){
        return name;
    }

    /**
     *
     * @return  symbol of the token
     */
    @External(readonly=true)
    public String symbol(){
        return symbol;
    }

    /**
     *
     * @return number of decimals
     */
    @External(readonly=true)
    public BigInteger decimals(){
        return decimals.get();
    }

    /**
     *
     * @param _owner The account whose balance is to be checked.
     * @return Amount of tokens owned by the `_owner` with the given address.
     */
    @External(readonly=true)
    public BigInteger balanceOf(Address _owner){
        return _balances.get(_owner);
    }

    /**
     *
     * @return the wallet address of admin.
     */
    @External(readonly=true)
    public Address getAdmin(){
        return admin.get();
    }

    /**
     *
     * @return  list of all the issuers.
     */
    @External(readonly=true)
    public List<Address> getIssuers(){
        List<Address> temp = null;
        for (int i = 0; i < issuers.size(); i++) {
            temp.add(issuers.get(i));
        }
        return temp;
    }

    /**
     *
     * @return if the score is paused
     */
    @External(readonly=true)
    public boolean isPaused(){
        return _paused.get();
    }

    /**
     *
     * @param _issuer The wallet address of issuer
     * @return amount of tokens that `_issuer` can mint at this point in time.
     */
    @External(readonly=true)
    public BigInteger issuerAllowance(Address _issuer) {
        return _allowances.get(_issuer);
    }

    /**
     *
     * @return daily free transaction limit
     */
    @External(readonly=true)
    public BigInteger freeDailyTxLimit() {

        return freeDailyTLimit.get();
    }

    /**
     *
     * @param _owner The account at which remaining free transaction is to be queried
     * @return number of free transactions left for `_owner`
     */
    @External(readonly=true)
    public BigInteger remainingFreeTxThisTerm(Address _owner) {

        if (!_whitelist.at(_owner).get("free_tx_start_height").equals(BigInteger.ZERO))
        {
            if (_whitelist.at(_owner).get("free_tx_start_height").add(TERM_LENGTH).compareTo(BigInteger.valueOf
                    (Context.getBlockHeight()))<0){
                return freeDailyTLimit.get();
            }else {
                return freeDailyTLimit.get().add(_whitelist.at(_owner).get("free_tx_count_since_start"));
            }
        }
        return BigInteger.ZERO;
    }

    /**
     *
     * @param _owner The account to check if it is whitelisted
     * @return if wallet address is whitelisted.
     */
    @External(readonly=true)
    public boolean isWhitelisted(Address _owner) {
        return !_whitelist.at(_owner).get("free_tx_start_height").equals(BigInteger.ZERO);
    }


    /**
     * Transfers certain amount of tokens from sender to the receiver.
     * @param _to The account to which the token is to be transferred.
     * @param _value The no. of tokens to be transferred.
     * @param _data Any information or message
     */
    @External
    public void transfer(Address _to, BigInteger _value, @Optional byte[] _data){

        setFeeSharingPercentage();

        if (_data == null) {
            _data = new byte[0];
        }
        _transfer(Context.getCaller(),_to,_value,_data);
    }

    @External
    /**
     * Changes daily free transactions limit for whitelisted users
     * Only admin can call this method
     * @param newLimit
     */
    public void changeFreeDailyTxLimit(BigInteger newLimit){

        require(newLimit.compareTo(BigInteger.ZERO) >= 0, "Free daily transaction limit cannot be under 0.");
        onlyAdmin("Only admin can change free daily transaction limit");

        freeDailyTLimit.set(newLimit);
    }

    /**
     * Add issuers. Issuers can mint and burn tokens.
     * Only admin can call this method.
     * @param issuer The wallet address of issuer to be added
     */
    @External
    public void addIssuer(Address issuer){
        require(!isIssuer(issuer), issuer+" is already an issuer");
        onlyAdmin("Only admin can add issuer");
        require(issuers.size()<nIssuers.get(),"Cannot have more than "+nIssuers.get()+" issuers");
        issuers.add(issuer);
    }

    /**
     * Remove issuer from the list of issuers.
     * Sets the allowance of `_issuer` to zero.
     * Only admin can call this method.
     * @param issuer The wallet of address of issuer to remove
     */
    @External
    public void removeIssuer(Address issuer){
        onlyAdmin("Only admin can remove issuer");
        require(isIssuer(issuer), issuer+" not an issuer");

        Address top = issuers.pop();

        if (top!=issuer){
            for (int i = 0; i < issuers.size(); i++) {
                if (issuers.get(i)==issuer){
                    issuers.set(i,top);
                }
            }
        }
        _allowances.set(issuer,BigInteger.ZERO);
    }


    /**
     * Allow `_issuer` to mint `_value` tokens.
     * @param issuer The issuer to approve to.
     * @param value The amount to approve to issuer to mint.
     */
    @External
    public void approve(Address issuer, BigInteger value){
        onlyAdmin("Only admin can approve amount to issuer");
        require(isIssuer(issuer), "Only issuers can be approved");
        _allowances.set(issuer,value);
        Approval(Context.getCaller(), issuer, value);
    }

    /**
     * Transfer the admin rights to another `_newAdmin` address
     * Only admin can call this method.
     * @param newAdmin New wallet address that will now have admin rights
     */
    @External
    public void transferAdminRight(Address newAdmin) {

        onlyAdmin("Only admin can transfer their admin right");
        admin.set(newAdmin);
    }

    /**
     * Toggles pause status of the score.
     * Only admin can call this method.
     */
    @External
    public void togglePause(){
        onlyAdmin("Only admin can toggle pause");
        _paused.set(!isPaused());
    }

    /**
     * Creates `_value` number of tokens, and assigns to caller account.
     * Increases the balance of that account and total supply.
     * Only issuers can call ths method.
     * @param value Number of tokens to be created at the account.
     */
    @External
    public void mint(BigInteger value){
        _mint(Context.getCaller(),value);
    }

    /**
     * Creates `_value` number of tokens, and assigns to `_to`.
     * Increases the balance of that account and total supply.
     * Only issuers can call ths method.
     * @param to The account at which token is to be created.
     * @param value Number of tokens to be minted
     */
    @External
    public void mintTo(Address to,BigInteger value){
        _mint(to,value);
    }

    /**
     * Destroys `_value` number of tokens from the caller account.
     * Decreases the balance of that account and total supply.
     * @param value Number of tokens to be destroyed.
     */
    @External
    public void burn(BigInteger value){
        _burn(Context.getCaller(),value);
    }
}
