
public enum Currency{
    BITCOIN,
    USD
}

static class CurrencyMethods{
    public static Currency Pair(this Currency currency){
        switch(currency){
            case Currency.BITCOIN:
                return Currency.USD;
            case Currency.USD:
                return Currency.BITCOIN;
            default:
                throw new ArgumentException();
        }
    }
}