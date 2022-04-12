public class MarketOrderResponse{
    public MarketOrderResponse(Decimal quantity, Decimal averagePrice){
        this.quantity = quantity;
        this.averagePrice = averagePrice;

    }
    public Decimal quantity;
    public Decimal averagePrice;
}