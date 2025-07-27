package com.baodo.stocktracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StockTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockTrackerApplication.class, args);
        System.out.println("StockTrackerApplication started successfully!");
    }

}
