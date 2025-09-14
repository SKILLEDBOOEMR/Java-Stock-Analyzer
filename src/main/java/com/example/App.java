package com.example;
import java.io.IOException;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;


public class App {
    public static void main(String[] args) throws IOException {
        String reset = "\u001B[0m";
        String red = "\u001B[31m";
        String green = "\u001B[32m";
        String blue = "\u001B[36m";

        Scanner scanner = new Scanner(System.in);
        System.out.print("Which stock would you like to analyze? (Enter the stock symbol, e.g., AAPL, MSFT, TSLA): ");
        String stock_symbol = scanner.next();
        stock_symbol = stock_symbol.toUpperCase();
        scanner.close();
        System.out.println(blue + "Loading..." + reset);


        Element priceElement = null; 
        try {
            Document Doc = Jsoup.connect("https://finance.yahoo.com/quote/" + stock_symbol + "/").get();
            System.out.println(Doc.title());

            // Select the stock price element by CSS class
            priceElement = Doc.selectFirst("span[data-testid=qsp-price]");

            String priceText = priceElement.text();
            double pricedouble = Double.parseDouble(priceText);
            System.out.println(pricedouble);

        } catch(Exception e) {
            System.out.println(red + "Failed" + reset);
        }
        if (priceElement != null) {
            System.out.println(stock_symbol +" Price: " + priceElement.text());
        } else {
            System.out.println("Price not found!");}
    }
    static boolean calculate_expenses(double value) {
        boolean expensive_cheap_state = false;
        return expensive_cheap_state;
    }
}


