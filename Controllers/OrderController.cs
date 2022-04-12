#nullable disable
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using TestApi.Models;

namespace testApi.Controllers
{
    [Route("api/[controller]")]
    [ApiController]
    public class OrderController : ControllerBase
    {
        private readonly ExchangeContext _context;

        public OrderController(ExchangeContext context)
        {
            _context = context;
        }

        // GET: api/Order
        [HttpGet]
        public async Task<ActionResult<IEnumerable<StandingOrder>>> GetStandingOrder()
        {
            return await _context.StandingOrder.ToListAsync();
        }

        // GET: api/Order/5
        [HttpGet("{id}")]
        public async Task<ActionResult<StandingOrder>> GetStandingOrder(int id)
        {
            var standingOrder = await _context.StandingOrder.FindAsync(id);

            if (standingOrder == null)
            {
                return NotFound();
            }

            return standingOrder;
        }

        // PUT: api/Order/5
        // To protect from overposting attacks, see https://go.microsoft.com/fwlink/?linkid=2123754
        [HttpPut("{id}")]
        public async Task<IActionResult> PutStandingOrder(int id, StandingOrder standingOrder)
        {
            if (id != standingOrder.Id)
            {
                return BadRequest();
            }

            _context.Entry(standingOrder).State = EntityState.Modified;

            try
            {
                await _context.SaveChangesAsync();
            }
            catch (DbUpdateConcurrencyException)
            {
                if (!StandingOrderExists(id))
                {
                    return NotFound();
                }
                else
                {
                    throw;
                }
            }

            return NoContent();
        }

        // POST: api/Order
        // To protect from overposting attacks, see https://go.microsoft.com/fwlink/?linkid=2123754
        [HttpPost("/standing")]
        public async Task<ActionResult<StandingOrder>> PostStandingOrder(StandingOrder standingOrder)
        {
            var user = await _context.User.FindAsync("1");
            if(user is null) return NotFound();
            var orders = await _context.StandingOrder.Select(x=> x).ToListAsync();
            var filteredOrders = orders.Where(it => (standingOrder.type == OrderType.Buy) ? it.limit <= standingOrder.limit : it.limit > standingOrder.limit).OrderBy(it => it.limit);
            var quantity = standingOrder.quantity;
            Tuple<Decimal, Decimal> t = (standingOrder.type==OrderType.Buy) 
                ? executeOrder(filteredOrders.Where(it=> it.type == OrderType.Sell).ToList(), user, quantity, OrderType.Buy, Currency.USD, false)
                : executeOrder(filteredOrders.Where(it=> it.type == OrderType.Buy).ToList(), user, quantity*100000000, OrderType.Sell, Currency.BITCOIN, false);
            var newStandingOrder = new StandingOrder();
            newStandingOrder.quantity = quantity;
            newStandingOrder.type = standingOrder.type;
            newStandingOrder.limit = standingOrder.limit;
            newStandingOrder.user = user;
            newStandingOrder.fill(t.Item2);

            if(newStandingOrder.quantity > 0){
                _context.StandingOrder.Add(newStandingOrder);
            }
            await _context.SaveChangesAsync();
            return CreatedAtAction("GetStandingOrder", new { id = newStandingOrder.Id }, newStandingOrder);
        }
        [HttpPost("/standing")]
        public async Task<ActionResult<StandingOrder>> PostMarketOrder(MarketOrderRequest request)
        {
            var user = await _context.User.FindAsync("1");
            var filteredOrders = _context.StandingOrder.Select(x => x).ToList().Where( it=>  it.type != request.type).OrderBy(it => it.limit);
            Tuple<Decimal, Decimal> t = (request.type==OrderType.Buy) 
            ? executeOrder(filteredOrders.Where(it=> it.type == OrderType.Sell).ToList(), user, request.quantity, OrderType.Buy, Currency.USD, true)
            : executeOrder(filteredOrders.Where(it=> it.type == OrderType.Buy).ToList(), user, request.quantity*100000000, OrderType.Sell, Currency.BITCOIN, true);

            await _context.SaveChangesAsync();
            return Created("uri?!",new MarketOrderResponse(t.Item2, t.Item2/t.Item1));

        }
        // DELETE: api/Order/5
        [HttpDelete("/standing/{id}")]
        public async Task<IActionResult> DeleteStandingOrder(int id)
        {


            return NoContent();
            var user = await _context.User.FindAsync("1");
            var order = await _context.StandingOrder.FindAsync(id);
            if(order is null) return NotFound();
            if(user.Id == order.user.Id) {
                _context.StandingOrder.Remove(order);
                if (order.type == OrderType.Buy) {
                    user.ChangeBalance(Currency.USD, order.quantity);
                } else {
                    user.ChangeBalance(Currency.BITCOIN, order.quantity);
                }
                await _context.SaveChangesAsync();
            } else{
                throw new ArgumentException("forbidden operation");
            }
            return Ok(id);

        }

        private bool StandingOrderExists(int id)
        
        {
            return _context.StandingOrder.Any(e => e.Id == id);
        }

        private Tuple<Decimal, Decimal> executeOrder(List<StandingOrder> filteredOrderBook, User user, Decimal quantity, OrderType type, Currency spendingCurrency, Boolean isMarket){
            user.ChangeBalance(spendingCurrency, -quantity);
            Decimal spent = 0;
            Decimal bought = 0;
            foreach (var o in filteredOrderBook){
                var userToSave = _context.User.Find(o.user.Id);
                if(userToSave is null){
                    throw new ArgumentException("User not found ");
                }
                if(spent + o.quantity <= quantity){
                    if (type == OrderType.Sell) {
                        spent += 100000000*o.quantity/o.limit;
                        userToSave.ChangeBalance(spendingCurrency, 100000000*o.quantity/o.limit);
                        bought += o.quantity;
                    } else {
                        spent += o.quantity;
                        userToSave.ChangeBalance(spendingCurrency, o.quantity);
                        bought += 100000000/(o.limit/o.quantity);
                    }
                    _context.SaveChanges();
                    o.fill(o.quantity);
                    if (o.quantity == 0L) 
                        _context.StandingOrder.Remove(o); 
                    else 
                        _context.StandingOrder.Add(o);
                }else{
                    if (type == OrderType.Sell){
                        bought += (quantity-spent);
                        userToSave.ChangeBalance(spendingCurrency, (quantity - spent));

                    } else{
                        bought += 100000000/(o.limit/(quantity-spent));
                        userToSave.ChangeBalance(spendingCurrency, (quantity - spent));

                    }
                    _context.SaveChanges();
                    o.fill(quantity - spent);
                    if (o.quantity == 0L) 
                        _context.StandingOrder.Remove(o); 
                    else 
                        _context.StandingOrder.Add(o);
                    spent = quantity;
                    break;
                }
            }
            if(isMarket) user.ChangeBalance(spendingCurrency, quantity-spent);
            // otazka je, ci tu nebude problem s user premennou - netreba si volat aj get z databazy???
            Console.Write($"bought {spendingCurrency.Pair()}: {bought} spent {spendingCurrency}: {spent}");
            user.ChangeBalance(spendingCurrency.Pair(), bought);
            _context.SaveChanges();
            return Tuple.Create(bought, spent);
        }
    }
}
