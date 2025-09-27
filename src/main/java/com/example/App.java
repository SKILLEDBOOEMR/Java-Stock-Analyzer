package com.example;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashSet;
import java.util.Scanner;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class App {
    public static void main(String[] args) throws IOException {
        String RESET = "\u001B[0m";
        String RED = "\u001B[31m";
        String GREEN = "\u001B[32m";
        String CYAN = "\u001B[36m";
        String YELLOW = "\u001B[33m";

        Scanner scanner = new Scanner(System.in);
        System.out.print("Exchange Ticker: ");
        String stock_exchange = scanner.nextLine().toUpperCase();
        System.out.print("Stock Ticker: ");
        String stock_ticker = scanner.nextLine().toUpperCase();
        System.out.print("Region Lock? (y/n): ");
        String region_lock = scanner.nextLine().toUpperCase();
        scanner.close();

        try{
            Document news_rss = Jsoup.connect("https://news.google.com/rss/search?q=" + "News+Stock+" + stock_exchange + ":" + stock_ticker).get();
            Elements news_item = news_rss.select("item");
            int count = 0;

            System.out.println(YELLOW + "---News---" + RESET);
            for (Element item : news_item) {
                if (count >= 20) break;
                String title = item.selectFirst("title").text();
                String link = item.selectFirst("link").text();
                String date = item.selectFirst("pubDate").text();

                count++;
                System.out.println("News " + count);
                System.out.println("Title: " + title);
                System.out.println("Link: " + CYAN + link + RESET);
                System.out.println("Date: " + date);
                System.out.println("");
            }
        }
        catch (Exception e) {
            System.out.println(e);
        } 
        pe_analysis(stock_ticker,stock_exchange, region_lock);

    }
    static void pe_analysis(String stock_ticker, String stock_exchange, String region_lock) throws IOException {
        String RESET = "\u001B[0m";
        String RED = "\u001B[31m";
        String GREEN = "\u001B[32m";
        String CYAN = "\u001B[36m";
        String YELLOW = "\u001B[33m";

        System.out.println(YELLOW + "Analyzing and Comparing P/E..." + RESET);

        try {
            JsonArray selected_stock_data = Checking_individual_status("",stock_exchange + ":" + stock_ticker,"");
            JsonArray results = null;

            // Fetching market_cap sizes
            long selected_stock_marketcap = selected_stock_data.get(4).getAsLong();
            String filters = "";
            for (double i = 0.1 ; i <= 100 ; i *= 2) {
                long dif = (long) (selected_stock_marketcap * i);
                long minCap = Math.max(0, selected_stock_marketcap - dif);
                long maxCap = selected_stock_marketcap + dif;
                filters = """
                [
                {
                "left": "sector",
                "operation": "equal",
                "right": "%s"
                },

                {
                "left": "industry",
                "operation": "equal",
                "right": "%s"
                },

                {
                "left":"is_primary",
                "operation":"equal",
                "right":true
                },

                {
                "left":"market_cap_basic",
                "operation":"in_range",
                "right":[%d,%d]
                }
                ]
                
                """.formatted(
                    selected_stock_data.get(0).getAsString(),
                    selected_stock_data.get(1).getAsString(),
                    minCap,
                    maxCap);
                if (region_lock.equals("Y")) {
                    results = Checking_individual_status(filters, "", selected_stock_data.get(5).getAsString());
                }else{
                    results = Checking_individual_status(filters, "","");
                }
                if (results.size() > 10) break;
                Thread.sleep(300);
            }

            System.out.printf("%-15s %-15s %-15s %-15s%n", "Stock Ticker","Market", "Price", "P/E TTM");
            for (JsonElement element : results) {
                JsonObject object = element.getAsJsonObject();
                JsonArray element_arr = object.get("d").getAsJsonArray();
                if (element_arr.get(0).getAsString().equals(stock_ticker)) {
                    System.out.printf("%-24s %-15s %-15.2f %-15.2f%n",GREEN + element_arr.get(0).getAsString() + RESET,element_arr.get(1).getAsString(),element_arr.get(2).getAsDouble(),element_arr.get(3).getAsDouble());
                }else{
                   System.out.printf("%-15s %-15s %-15.2f %-15.2f%n",element_arr.get(0).getAsString(),element_arr.get(1).getAsString(),element_arr.get(2).getAsDouble(),element_arr.get(3).getAsDouble());
                }
                
            }

        } catch (Exception e) {
        }
    }

    static JsonArray Checking_individual_status(String filters,String Special_filter,String region) throws Exception{
        String jsonBody = "";
        if (filters.equals("")) {
            jsonBody = """
        {
          "symbols": { "tickers": [\"%s\"], "query": { "types": [] } },
          "columns": ["sector", "industry","name","close","market_cap_basic","market"],
          "sort": { "sortBy": "market_cap_basic", "sortOrder": "desc" },
          "price_conversion" : {"to_currency": "usd"},
          "options": { "lang": "en" },
          "range": [0, 1]
        }
        """.formatted(Special_filter);

        }
        else {
            if (region.equals("")) {
                jsonBody = """
                    {
                    "filter": %s,
                    "symbols": { "tickers": [], "query": { "types": [] } },
                    "columns": ["name", "market","close", "price_earnings_ttm","earnings_per_share_diluted_yoy_growth_ttm","earnings_per_share_diluted_ttm","price_sales_current","total_revenue_yoy_growth_ttm","gross_profit_ttm" ],
                    "sort": { "sortBy": "market_cap_basic", "sortOrder": "desc" },
                    "price_conversion" : {"to_currency": "usd"},
                    "options": { "lang": "en" },
                    "range": [0, 100]
                    }
                    """.formatted(filters);
            }
            else {
                jsonBody = """
                    {
                    "filter": %s,
                    "symbols": { "tickers": [], "query": { "types": [] } },
                    "columns": ["name","market","close", "price_earnings_ttm","earnings_per_share_diluted_yoy_growth_ttm","earnings_per_share_diluted_ttm","price_sales_current","total_revenue_yoy_growth_ttm","gross_profit_ttm" ],
                    "sort": { "sortBy": "market_cap_basic", "sortOrder": "desc" },
                    "price_conversion" : {"to_currency": "usd"},
                    "options": { "lang": "en" },
                    "markets": ["%s"],
                    "range": [0, 100]
                    }
                    """.formatted(filters,region);
            }
        }
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://scanner.tradingview.com/global/scan?label-product=screener-stock")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
        HttpClient client = HttpClient.newHttpClient();
        JsonObject json = null;
        HttpResponse<String> response = null;

        for (int i = 0; i < 5; i++) { // Retry up to 5 times
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && !response.body().isEmpty()) {
                json = JsonParser.parseString(response.body()).getAsJsonObject();
                if (json.has("data")) break; // Success
            }

            Thread.sleep(300); // Wait before retrying
        }

        if (json == null || !json.has("data")) {
        System.out.println("Failed to get valid response after retries");
        return new JsonArray();
        }

        JsonArray data = json.getAsJsonArray("data");

        if (filters.equals("")) {
            JsonObject data_array = data.get(0).getAsJsonObject();
            JsonArray darray = data_array.getAsJsonArray("d");
            return darray;
        }
        return data;
    }

    static Set<String> convert_stocks_to_10(JsonArray listofstock) throws Exception {
        System.out.println(listofstock);
        Set<String> uniqueTickers = new LinkedHashSet<>();

        for (JsonElement element : listofstock) {
            System.out.println(element);
            JsonObject object = element.getAsJsonObject();
            JsonArray array = object.get("d").getAsJsonArray();

            if (array.size() > 0) {
                String ticker = array.get(0).getAsString();

                if (ticker.matches("^[A-Z]+$")) {
                        uniqueTickers.add(ticker);
                        System.out.println(array);
                }
            }
        }

        System.out.println(uniqueTickers);
        return uniqueTickers;
    }

}

