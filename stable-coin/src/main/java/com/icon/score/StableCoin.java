package com.icon.score;

import score.Address;
import score.Context;
import score.annotation.External;
import java.math.BigInteger;
import java.util.List;

import static score.Context.require;

public class StableCoin extends AbstractStableCoin {

    public StableCoin(String name, String symbol, BigInteger decimals, Address admin, int nIssuers) {
        super();



        this.name = name;
        this.symbol = symbol;
        this.decimals.set(decimals);
        this.admin.set(admin);
        this.nIssuers.set(nIssuers);
        this.totalSupply.set(BigInteger.ZERO);
        this._paused.set(false);
        this.freeDailyTLimit.set(BigInteger.valueOf(50));
    }

    @External(readonly=true)
    public String name(){
    /*
        Returns the name of the token
     */
        return name;
    }

    @External(readonly=true)
    public String symbol(){
    /*
        Returns the symbol of the token
     */
        return symbol;
    }

    @External(readonly=true)
    public BigInteger decimals(){
    /*
        Returns the number of decimals
        For example, if the decimals = 2, a balance of 25 tokens
        should be displayed to the user as (25 * 10 ** 2)
     */
        return decimals.get();
    }

    @External(readonly=true)
    public BigInteger balanceOf(Address _owner){
    /*
         Returns the amount of tokens owned by the account
        :param _owner: The account whose balance is to be checked.
        :return Amount of tokens owned by the `_owner` with the given address.
     */
        return _balances.get(_owner);
    }

    @External(readonly=true)
    public Address getAdmin(){
    /*
       Returns the wallet address of admin.
     */
        return Context.getCaller();
    }
    @External(readonly=true)
    public List<Address> getIssuers(){
    /*
        Returns the list of all the issuers.
     */
        return (List<Address>) issuers;
    }
    @External(readonly=true)
    public boolean isPaused(){
    /*
        Returns if the score is paused
     */
        return _paused.get();
    }
    @External(readonly=true)
    public BigInteger issuerAllowance(Address _issuer) {
    /*
        Returns amount of tokens that `_issuer` can mint at this point in time.
        :param _issuer: The wallet address of issuer
     */
        return _allowances.get(_issuer);
    }

    @External(readonly=true)
    public BigInteger freeDailyTxLimit() {
    /*
         Returns daily free transaction limit
     */
        return freeDailyTLimit.get();
    }

    @External(readonly=true)
    public BigInteger remainingFreeTxThisTerm(Address _owner) {
    /*
         Returns number of free transactions left for `_owner`
        :param _owner: The account at which remaining free transaction is to be queried
     */
        return BigInteger.ONE;
    }

    @External(readonly=true)
    public boolean isWhitelisted(Address _owner) {
    /*
         Returns if wallet address is whitelisted.
        :param _owner: The account to check if it is whitelisted
     */
        return _whitelist.at(_owner).get("free_tx_start_height")!= 0;
    }


    @External
    public void transfer(Address _to, BigInteger _value, byte[] _data){
    /*
        Transfers certain amount of tokens from sender to the receiver.
        :param _to: The account to which the token is to be transferred.
        :param _value: The no. of tokens to be transferred.
        :param _data: Any information or message
     */

        _transfer(Context.getCaller(),_to,_value,_data);
    }

    @External
    public void changeFreeDailyTxLimit(BigInteger newLimit){
    /*
        Add issuers. Issuers can mint and burn tokens.
        Only admin can call this method.
        :param _issuer: The wallet address of issuer to be added
     */
        require(newLimit.compareTo(BigInteger.ZERO) >= 0, "Free daily transaction limit cannot be under 0.");
        require(Context.getCaller() == Context.getOwner(),"Only admin can add issuer");

        freeDailyTLimit.set(newLimit);
    }

    @External
    public void addIssuer(Address issuer){
    /*
        Add issuers. Issuers can mint and burn tokens.
        Only admin can call this method.
        :param _issuer: The wallet address of issuer to be added
     */
        require(Context.getCaller() == Context.getOwner(),"Only admin can add issuer");
        isIssuer.set(issuer,true);
    }

    @External
    public void removeIssuer(Address issuer){
    /*
        Remove issuer from the list of issuers.
        Sets the allowance of `_issuer` to zero.
        Only admin can call this method.
        :param _issuer: The wallet of address of issuer to remove

     */

        require(Context.getCaller() == Context.getOwner(),"Only admin can remove issuer");
        isIssuer.set(issuer,false);
    }

//    require(_issuer in self._issuers, "{_issuer} not an issuer");
//
//    top = self._issuers.pop()
//            if top != _issuer:
//            for i in range(len(self._issuers)):
//            if self._issuers[i] == _issuer:
//    self._issuers[i] = top
//    self._allowances[_issuer] = 0

    @External
    public void approve(Address issuer, BigInteger value){
    /*
        Allow `_issuer` to mint `_value` tokens.
        :param _issuer: The issuer to approve to.
        :param _value: The amount to approve to issuer to mint.
     */
        require(Context.getCaller() == Context.getOwner(),"Only admin can approve amount to issuer");
//        require(issuer in getIssuers(), "Only issuers can be approved")
        _allowances.set(issuer,value);
        Approval(Context.getCaller(), issuer, value);
    }

    @External
    public void transferAdminRight(Address newAdmin) {
    /*
        Transfer the admin rights to another `_newAdmin` address
        Only admin can call this method.
        :param _newAdmin: New wallet address that will now have admin rights
     */
        require(Context.getCaller() == Context.getOwner(),"Only admin can transfer their admin right");
        admin.set(newAdmin);
    }

    @External
    public void togglePause(){
    /*
        Toggles pause status of the score.
        Only admin can call this method.
     */
        require(Context.getCaller() == Context.getOwner(),"Only admin can toggle pause");
        _paused.set(!isPaused());
    }

    @External
    public void mint(BigInteger value){
    /*
        Creates `_value` number of tokens, and assigns to caller account.
        Increases the balance of that account and total supply.
        Only issuers can call ths method.
        :param _value: Number of tokens to be created at the account.
     */
        _mint(Context.getCaller(),value);
    }

    @External
    public void mintTo(Address to,BigInteger value){
    /*
        Creates `_value` number of tokens, and assigns to `_to`.
        Increases the balance of that account and total supply.
        Only issuers can call ths method.
        :param _to: The account at which token is to be created.
        :param _value: Number of tokens to be created at the account.upply.
        :param _value: Number of tokens to be destroyed.
     */
        _mint(to,value);
    }

    @External
    public void burn(BigInteger value){
    /*
        Destroys `_value` number of tokens from the caller account.
        Decreases the balance of that account and total supply.
        :param _value: Number of tokens to be destroyed.
     */
        _burn(Context.getCaller(),value);
    }



}
