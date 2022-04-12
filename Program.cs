using Microsoft.EntityFrameworkCore;
using TestApi.Models;
using Microsoft.Extensions.DependencyInjection;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddDbContext<ExchangeContext>(options =>
    options.UseSqlServer(builder.Configuration.GetConnectionString("ExchangeContext") ?? throw new InvalidOperationException("Connection string 'ExchangeContext' not found.")));
builder.Services.AddControllers();

builder.Services.AddDatabaseDeveloperPageExceptionFilter();
builder.Services.AddDbContext<ExchangeContext>(opt => opt.UseInMemoryDatabase("Exchange"));

var app = builder.Build();
app.MapControllers();
app.Run();