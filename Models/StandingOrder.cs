namespace TestApi.Models
{
    public class StandingOrder{
        // public StandingOrder(decimal quantity, OrderType type, decimal limit, User user){
        //     this.quantity = quantity;
        //     this.type = type;
        //     this.limit = limit;
        //     this.user = user;
        // }
        public int Id { get; set; }

        public decimal quantity { get; set; } = 0;
        public OrderType type { get; set; } = OrderType.Buy;
        public decimal limit { get; set; } = 0;
        public User user = new User();


        public void fill(Decimal volume){
            this.quantity -= volume;
        }
    }
}