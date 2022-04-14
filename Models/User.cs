public class User
{
    
    public int Id { get; set; }
    public decimal Satoshis { get; set; } = 0;
    public decimal USDBalance { get; set; } = 0;
    public string FirstName { get; set; }
    public string LastName { get; set; }
    public string Username { get; set; }
    public string Password  { get; set; }


    private void SetBalance(Currency currency, Decimal amount)
    {
        if(currency == Currency.BITCOIN ){
            this.Satoshis = amount;
        }
        else{
            this.USDBalance = amount;
        }
    }
    public void ChangeBalance(Currency currency, Decimal amount)
    {
        if(GetBalance(currency) + amount < 0)
        {
            throw new ArgumentException($"Can't spend more than {GetBalance(currency)} {currency}, expected {amount}");
        }
        this.SetBalance(currency, GetBalance(currency) + amount);
    }
    public Decimal GetBalance(Currency currency)
    {
        if(currency == Currency.BITCOIN){
            return this.Satoshis;
        }else{
            return this.USDBalance;
        }
    }
}

