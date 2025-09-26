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
        System.out.print("Stock Ticker: ");
        String stock_ticker = scanner.nextLine().toUpperCase();
        scanner.close();

        try{
            Document news_rss = Jsoup.connect("https://news.google.com/rss/search?q=" + "News+Stock+" + stock_ticker).get();
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
        pe_analysis(stock_ticker);

    }
    static void pe_analysis(String stock_ticker) throws IOException {
        String RESET = "\u001B[0m";
        String RED = "\u001B[31m";
        String GREEN = "\u001B[32m";
        String CYAN = "\u001B[36m";
        String YELLOW = "\u001B[33m";

        System.out.println(YELLOW + "Analyzing and Comparing P/E..." + RESET);

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://quote.cnbc.com/quote-html-webservice/restQuote/symbolType/symbol?symbols="+ stock_ticker +"&requestMethod=itv&noform=1&partnerId=2&fund=1&exthrs=1&output=json&events=1")).build();
            HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            // Step 1: Get the FormattedQuoteResult object
            JsonObject formattedQuoteResult = json.getAsJsonObject("FormattedQuoteResult");

            // Step 2: Get the FormattedQuote array
            JsonArray formattedQuoteArray = formattedQuoteResult.getAsJsonArray("FormattedQuote");

            // Step 3: Get the first object inside the array
            JsonObject firstQuote = formattedQuoteArray.get(0).getAsJsonObject();

            String price = firstQuote.get("last").getAsString();
            String exchange = firstQuote.get("exchange").getAsString();
            System.out.println(price);

            JsonArray selected_stock_data = Checking_individual_status("",exchange + ":" + stock_ticker);
            System.out.println(selected_stock_data);
            JsonArray results = null;
            // Fetching market_cap sizes
            for (double i = 0.1 ; i <= 1 ; i += 0.1) {
                double selected_stock_marketcap = selected_stock_data.get(4).getAsDouble();
                double dif = (selected_stock_marketcap * i);
                String filters2 = """
                [
                {
                "left": "sector",
                "operation": "equal",
                "right": %s
                },

                {
                "left": "industry",
                "operation": "equal",
                "right": %s
                },

                {
                "left": "earnings_per_share_diluted_yoy_growth_ttm",
                "operation": "greater",
                "right": 0
                },

                {
                "left":"is_primary",
                "operation":"equal",
                "right":true
                },

                {
                "left":"market_cap_basic",
                "operation":"in_range",
                "right":[%f,%f]
                }
                ]
                """.formatted(
                    selected_stock_data.get(0),
                    selected_stock_data.get(1),
                    selected_stock_marketcap - dif,
                    selected_stock_marketcap + dif);
                    
                System.out.println(filters2);
                results = Checking_individual_status(filters2, "");
                if (results.size() > 10) break;
            }
            System.out.println(results);

        } catch (Exception e) {
        }
    }

    static JsonArray Checking_individual_status(String filters,String Special_filter) throws Exception{
        String jsonBody;
        if (filters.equals("")) {
            jsonBody = """
        {
          "filter": [],
          "symbols": { "tickers": [\"%s\"], "query": { "types": [] } },
          "columns": ["sector", "industry","name","close","market_cap_basic"],
          "sort": { "sortBy": "market_cap_basic", "sortOrder": "desc" },
          "options": { "lang": "en" },
          "range": [0, 1]
        }
        """.formatted(Special_filter);

        }
        else {
            jsonBody = """
        {
          "filter": %s,
          "symbols": { "tickers": [], "query": { "types": [] } },
          "columns": ["name","close", "price_earnings_ttm","earnings_per_share_diluted_yoy_growth_ttm","earnings_per_share_diluted_ttm","price_sales_current","total_revenue_yoy_growth_ttm","gross_profit_ttm" ],
          "sort": { "sortBy": "market_cap_basic", "sortOrder": "desc" },
          "options": { "lang": "en" },
          "range": [0, 100]
        }
        """.formatted(filters);
        }
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://scanner.tradingview.com/global/scan?label-product=screener-stock")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
        HttpClient client = HttpClient.newHttpClient();
        JsonObject json = null;
        HttpResponse<String> response = null;

        for (int i = 0; i < 5; i++) { // Retry up to 5 times
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
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

