using Microsoft.EntityFrameworkCore;
using TestApi.Models;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.AspNetCore.Authentication;
using testApi.Helpers;
using testApi.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddControllers();
builder.Services.AddDatabaseDeveloperPageExceptionFilter();
builder.Services.AddDbContext<ExchangeContext>(opt => opt.UseInMemoryDatabase("Exchange"));

builder.Services.AddAuthentication("BasicAuthentication")
    .AddScheme<AuthenticationSchemeOptions, BasicAuthenticationHandler>("BasicAuthentication", null); ;
builder.Services.AddScoped<IUserService, UserService>();

var app = builder.Build();

var scopeeee = app.Services.CreateScope();
var context = scopeeee.ServiceProvider.GetRequiredService<ExchangeContext>();
var user = new User { Id = 1, FirstName = "Test", LastName = "User", Username = "test", Password = "test", Satoshis = 4*100000000};
context.Add(user);
context.Add(new StandingOrder { limit = 40000, quantity = 1, type = OrderType.Sell, user = user});
context.SaveChanges();

app.MapControllers();
app.UseAuthentication();
app.UseAuthorization();
app.Run();