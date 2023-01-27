package com.example.project2;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Integer.parseInt;

public class MainVerticle extends AbstractVerticle {
  public static void main(String[] args) {
    System.out.println("My First API");
    Vertx vertx=Vertx.vertx();
    vertx.deployVerticle(new MainVerticle());

  }

  void all_detail(SqlClient client,RoutingContext routingContext)
  {
    client
      .query("SELECT * FROM employees e LEFT JOIN department d on e.dept=d.deptName Order by e.id")
      .execute(ar->{
        HttpServerResponse serverResponse = routingContext.response();
        serverResponse.setChunked(true);
        if(ar.succeeded())
        {
          RowSet<Row> rows=ar.result();
          int i=1;
          for(Row row:rows)
          {
            System.out.println((i++)+") "+row.getInteger(0)+"   "+row.getString(1)+"   "+row.getString(2)+"   "+row.getDouble(3)+"   "+row.getInteger(4)+"   "+row.getString(5)+"   "+row.getString(6)+"   "+row.getInteger(7));
          }
        }
      });
  }
  void get_method(SqlClient client,RoutingContext routingContext)
  {
    client
      .query("SELECT * FROM employees")
      .execute(ar -> {
        HttpServerResponse serverResponse = routingContext.response();
        serverResponse.setChunked(true);
        if (ar.succeeded()) {
          RowSet<Row> rows = ar.result();
          int i=1;
          for (Row row : rows) {
            System.out.println((i++)+") User ka ID : " + row.getInteger(0) + " aur name : " + row.getString(1));
          }
        } else {
          serverResponse.end("Failure: " + ar.cause().getMessage());
          serverResponse.setStatusCode(502);
          serverResponse.setStatusMessage("Nothing in Database");
        }
      });
  }

  void get_filter_method(SqlClient client,HttpServerResponse serverResponse,String nm)
  {
    client
      .preparedQuery("SELECT * FROM employees E WHERE E.name=(?) LIMIT 1")
      .execute(Tuple.of(nm),ar -> {
        if (ar.succeeded()) {
          RowSet<Row> rows = ar.result();
          if(rows.size()==0)
          {
            serverResponse.setStatusCode(404);
            serverResponse.setStatusMessage("INVALID REQUEST");
            serverResponse.end("No Record Found");
          }
          for(Row row:rows){
            if(rows.size()==1)
            {
              serverResponse.end("User ID : " + row.getInteger(0) + '\n'+"User name : " + row.getString(1) + '\n'+"User Dept : " + row.getString(2)+ '\n'+"User Salary : " + row.getDouble(3));
            }
          }
        } else {
          System.out.println("Failure: " + ar.cause().getMessage());
        }
      });
  }

  void update_detail(SqlClient client,int uid,HttpServerResponse serverResponse,Employee emp)
  {
//    HttpServerResponse serverResponse = routingContext.response();
//    serverResponse.setChunked(true);
//    final Employee emp = Json.decodeValue(routingContext.getBody(), Employee.class);
//    try{
//      int uid=Integer.parseInt(routingContext.request().getParam("id"));
//      client
//        .preparedQuery("SELECT * FROM employees E WHERE (?)=E.id")
//        .execute(Tuple.of(uid), ar->{
//          RowSet<Row> rec=ar.result();
//          if(rec.size()>=1){
//
//          }
//          else{
//            serverResponse.setStatusCode(404);
//            serverResponse.setStatusMessage("INVALID REQUEST");
//            serverResponse.end("No employee record with ID : "+uid+" found");
//          }
//        });
    try{
      client
        .preparedQuery("UPDATE db01.employees E SET name=(?),dept=(?),salary=(?) WHERE (?)=E.id")
        .execute(Tuple.of(emp.getName(),emp.getDept(),emp.getSalary(),uid), ar1->{
          if(ar1.succeeded()){
            serverResponse.end("Employee with ID : "+uid+" gets UPDATED");
          }
          else{
            System.out.println("Failure : "+ ar1.cause().getMessage());
          }
        });
    }
    catch(Exception e){
      serverResponse.setStatusCode(404);
      serverResponse.setStatusMessage("INVALID REQUEST");
      serverResponse.end("No employee record with ID : "+uid+" found"+'\n'+e);
    }
//    }
//    catch(Exception e){
//      serverResponse.setStatusCode(400);
//      serverResponse.setStatusMessage("Invalid Parameter Request");
//      serverResponse.end("Failure : "+'\n'+e);
//    }
  }

  void delete_emp(int uid,SqlClient client,HttpServerResponse serverResponse)
  {
    client
      .preparedQuery("DELETE FROM db01.employees E WHERE (?)=E.id")
      .execute(Tuple.of(uid),ar->{
        RowSet<Row> rec=ar.result();
        System.out.println(rec.size());
        if (ar.succeeded()) {
          serverResponse.end("Record deleted successfully!!");
        } else {
          serverResponse.setStatusCode(404);
          serverResponse.setStatusMessage("INVALID REQUEST");
          serverResponse.end("No employee record with ID : "+uid+" found");
          System.out.println("Batch failed " + ar.cause());
        }
      });
  }

  void add_emp(Employee emp,SqlClient client,SqlClient client1,RoutingContext routingContext)
  {
    client1
      .preparedQuery("Select * from db01.employees e where (?)=e.id")
      .execute(Tuple.of(emp.getId()), ar1->{
        HttpServerResponse serverResponse = routingContext.response();
        serverResponse.setChunked(true);
        if(ar1.succeeded()){
          RowSet<Row> r1=ar1.result();
          System.out.println(r1.size());
          if(r1.size()==1){
            serverResponse.setStatusCode(403);
            serverResponse.setStatusMessage("Already Exist");
            serverResponse.end("Failed!!!"+'\n'+"ID already exists.");
          }else{
            client
              .preparedQuery("INSERT INTO db01.employees(id,name,dept,salary) VALUES( (?), (?), (?), (?))")
              .execute(Tuple.of(emp.getId(),emp.getName(),emp.getDept(),emp.getSalary()), ar->{
                if (ar.succeeded()) {
                  serverResponse.end("Congratulations Employee added successfully");
                } else {
                  System.out.println("Batch failed " + ar.cause());
                }
              });
          }
        }
        else{
          System.out.println("Batch failed " + ar1.cause());
        }
      });
  }


  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    //creating employee class object list to store all employee
    //List<Employee> employees=new ArrayList<>();

    //connecting mySQL db.
    MySQLConnectOptions connectOptions = new MySQLConnectOptions()
      .setPort(3306)
      .setHost("127.0.0.1")
      .setDatabase("db01")
      .setUser("root")
      .setPassword("Qwerty12345@");

    // Creating pool for interaction with db
    PoolOptions poolOptions = new PoolOptions()
      .setMaxSize(5);
    // Create the client pool
    SqlClient client = MySQLPool.pool(vertx,connectOptions, poolOptions);
    SqlClient client1 = MySQLPool.pool(vertx,connectOptions, poolOptions);
    Router router1= Router.router(vertx);


    client
      .query("SELECT * FROM employees")
      .execute(ar -> {
        if (ar.succeeded()) {
          RowSet<Row> result = ar.result();
          System.out.println("Got " + result.size() + " rows ");
        } else {
          System.out.println("Failure: " + ar.cause().getMessage());
        }
      });


//    router1.connectWithRegex("\"^((http|https)://)[-a-zA-Z0-9@:%._\\\\+~#?&//=]{2,256}\\\\.[a-z]{2,6}\\\\b([-a-zA-Z0-9@:%._\\\\+~#?&//=]*)$\"")
//      .handler(routingContext->{
//        HttpServerResponse serverResponse = routingContext.response();
//        serverResponse.setChunked(true);
//        serverResponse.setStatusCode(400);
//        serverResponse.setStatusMessage("Bad request");
//        serverResponse.end("INVALID URL");
//      });

    //ALL detail
    router1.get("/allDetail")
        .produces("*/json")
          .handler(routingContext->{
            System.out.println("----------Print all details--------");
            all_detail(client,routingContext);
          });

    //GET request
    router1.get("/empDetail/get")
      .produces("*/json")
      .handler(routingContext -> {
          System.out.println("-----------GET CALLED----------");
          get_method(client, routingContext);
//        routingContext
//          .response()
//          .setChunked(true)
//          .end(Json.encodePrettily(employees));
        });

    //GET-FILTER request
    router1.get("/empDetail/get/:name")
      .produces("*/json")
      .handler(routingContext -> {
          System.out.println("------------GET-FILTER CALLED----------");
          HttpServerResponse serverResponse = routingContext.response();
          serverResponse.setChunked(true);
          try {
            String nm = routingContext.request().getParam("name");
            get_filter_method(client, serverResponse, nm);
          }
          catch(Exception e){
            serverResponse.setStatusCode(400);
            serverResponse.setStatusMessage("Invalid Parameter Request");
            serverResponse.end("Failure : "+'\n'+e);
          }
        });

    //PUT method
    router1.put("/empDetail/update/:id")
      .produces("*/json")
      .handler(routingContext -> {
          System.out.println("-------------PUT CALLED--------------");
          HttpServerResponse serverResponse = routingContext.response();
          serverResponse.setChunked(true);
          try{
            int uid = Integer.parseInt(routingContext.request().getParam("id"));
            final Employee empl = Json.decodeValue(routingContext.getBody(), Employee.class);
            update_detail(client,uid,serverResponse,empl);
          }
          catch(Exception e){
            serverResponse.setStatusCode(400);
            serverResponse.setStatusMessage("Invalid Parameter Request");
            serverResponse.end("Failure : "+'\n'+e);
          }


        });

    //DELETE request
    router1.delete("/empDetail/delete/:id")
      .handler(routingContext -> {
          System.out.println("---------------DELETE CALLED------------");
          HttpServerResponse serverResponse = routingContext.response();
          serverResponse.setChunked(true);
          try{
            int uid = Integer.parseInt(routingContext.request().getParam("id"));
            delete_emp(uid, client, serverResponse);
          }
          catch(Exception e){
            serverResponse.setStatusCode(400);
            serverResponse.setStatusMessage("Invalid Parameter Request");
            serverResponse.end("Failure : "+'\n'+e);
          }

        });

    //POST request
    router1.post("/empDetail/add")
      .handler(routingContext -> {
          System.out.println("--------------POST CALLED------------");
          final Employee emp = Json.decodeValue(routingContext.getBody(), Employee.class);
          System.out.println(emp.getName());
          add_emp(emp, client, client1, routingContext);
        });


    //create server
    vertx
      .createHttpServer()
      .requestHandler(router1)
      .listen(8888, http -> {
        if (http.succeeded()) {
          startPromise.complete();
          System.out.println("HTTP server started on port 8888");
        } else {
          startPromise.fail(http.cause());
        }
      });

  }
}
