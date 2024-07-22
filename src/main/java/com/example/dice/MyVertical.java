package com.example.dice;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

import java.util.Arrays;

public class MyVertical extends AbstractVerticle {

    private JDBCClient jdbc;

    @Override
    public void start(Promise<Void> startPromise) {
        jdbc = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", "jdbc:mysql://localhost:3306/EmployeeDB")
                .put("driver_class", "com.mysql.cj.jdbc.Driver")
                .put("user", "root")
                .put("password", "password")
        );

        Router router = Router.router(vertx);
        router.post("/employee").handler(this::addEmployee);
        router.get("/employee/:id").handler(this::getEmployee);

        vertx.createHttpServer().requestHandler(router).listen(8888, http -> {
            if (http.succeeded()) {
                startPromise.complete();
                System.out.println("HTTP server started on port 8888");
            } else {
                startPromise.fail(http.cause());
            }
        });
    }

    private void addEmployee(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        String empId = body.getString("EmpId");
        String empName = body.getString("EmpName");
        String empSalary = body.getString("EmpSalary");

        try {
            if (isPrime(Integer.parseInt(empSalary)) && isAnagram(empId, empName)) {
                jdbc.updateWithParams("INSERT INTO Employees (EmpId, EmpName, EmpSalary) VALUES (?, ?, ?)",
                        new JsonArray().add(empId).add(empName).add(empSalary), res -> {
                            if (res.succeeded()) {
                                context.response().setStatusCode(201).end("Employee added and conditions met");
                            } else {
                                context.response().setStatusCode(500).end(res.cause().getMessage());
                            }
                        });
            } else {
                throw new CustomValidationException("Conditions not met");
            }
        } catch (CustomValidationException e) {
            context.response().setStatusCode(400).end(e.getMessage());
        }
    }

    private void getEmployee(RoutingContext context) {
        String empId = context.pathParam("id");
        jdbc.queryWithParams("SELECT * FROM Employees WHERE EmpId = ?", new JsonArray().add(empId), res -> {
            if (res.succeeded()) {
                if (res.result().getNumRows() > 0) {
                    context.response().end(res.result().getRows().get(0).encode());
                } else {
                    context.response().end("Employee not found");
                }
            } else {
                context.response().end(res.cause().getMessage());
            }
        });
    }

    private boolean isPrime(int number) {
        if (number <= 1) return false;
        for (int i = 2; i <= Math.sqrt(number); i++) {
            if (number % i == 0) return false;
        }
        return true;
    }

    private boolean isAnagram(String str1, String str2) {
        if (str1.length() != str2.length()) {
            return false;
        }
        char[] a1 = str1.toCharArray();
        char[] a2 = str2.toCharArray();
        Arrays.sort(a1);
        Arrays.sort(a2);
        return Arrays.equals(a1, a2);
    }

    private static class CustomValidationException extends Exception {
        public CustomValidationException(String message) {
            super(message);
        }
    }

    public static void main(String[] args) {

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MyVertical());
    }
}
