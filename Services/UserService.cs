using Microsoft.EntityFrameworkCore;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace testApi.Services
{
    public interface IUserService
    {
        Task<User> Authenticate(string username, string password);
        Task<IEnumerable<User>> GetAll();
    }

    public class UserService : IUserService
    {
        private readonly ExchangeContext _context;

        public UserService(ExchangeContext context)
        {
            _context = context;
        }

        public async Task<User> Authenticate(string username, string password)
        {
            // wrapped in "await Task.Run" to mimic fetching user from a db
            var user = await _context.User.SingleOrDefaultAsync(x => x.Username == username && x.Password == password);
            // return null if user not found
            if (user == null)
                return null;

            // authentication successful so return user details
            return user;
        }

        public async Task<IEnumerable<User>> GetAll()
        {   
            // wrapped in "await Task.Run" to mimic fetching users from a db
            return await _context.User.ToListAsync();
        }
    }
}