#nullable disable
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using TestApi.Models;

    public class ExchangeContext : DbContext
    {
        public ExchangeContext (DbContextOptions<ExchangeContext> options)
            : base(options)
        {
        }

        public DbSet<User> User => Set<User>();

        public DbSet<StandingOrder> StandingOrder => Set<StandingOrder>();
    }
