package com.example;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

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
            long minCap = 0;
            long maxCap = 0;

            for (double i = 0.1 ; i <= 100 ; i *= 2) {
                long dif = (long) (selected_stock_marketcap * i);
                minCap = Math.max(0, selected_stock_marketcap - dif);
                maxCap = selected_stock_marketcap + dif;
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
                if (results.size() >= 11) {
                    int selected_stock_location = 0;
                    for (JsonElement element : results) {
                        JsonObject object = element.getAsJsonObject();
                        JsonArray element_arr = object.get("d").getAsJsonArray();
                        if (safeGetString(element_arr, 0).equals(stock_ticker)) {
                            selected_stock_location++;
                            break;
                        }
                        selected_stock_location++;
                    }
                    int top_pos = selected_stock_location - 1;
                    int bot_pos = selected_stock_location - 1;


                    for (int s = 0 ; s <= results.size() / 2 ; s += 1) {
                        if (top_pos + 1 > results.size() - 1) {
                            bot_pos -= (5-s) * 2;
                            break;
                        }
                        if (bot_pos - 1 < 0) {
                            top_pos += (5-s) * 2;
                            break;
                        }
                        top_pos++;
                        bot_pos--;
                    }
                    System.out.println(bot_pos);
                    System.out.println(  top_pos);
                    JsonArray slice = new JsonArray();
                    for (int b = bot_pos; b <= top_pos && b < results.size(); b++) {
                        slice.add(results.get(b));
                    }
                    results = slice;
                    break;
                }
                else{
                    Thread.sleep(300);
                }
            }

            System.out.printf("%-25s%n", YELLOW + "Filters Applied" + RESET);
            System.out.printf("%-25s %-2s %-12s%n", "Sector", ":", selected_stock_data.get(0).getAsString());
            System.out.printf("%-25s %-2s %-12s%n", "Industry", ":", selected_stock_data.get(1).getAsString());
            System.out.printf("%-25s %-2s %-12s%n", "Market Cap Range", ":", shorten_num_to_readable(minCap) + "-" + shorten_num_to_readable(maxCap));
            if (region_lock.equals("Y")) {
                System.out.printf("%-25s %-2s %-12s%n", "Region", ":", selected_stock_data.get(5).getAsString());
            }
            System.out.println("");

            // Print all results once after loop
            System.out.println(YELLOW + "Relevant Data Fetched !" + RESET);
            System.out.printf("%-12s %-12s %-12s %-12s %-12s %-12s %-12s %-12s %-12s %-20s %-20s %-20s%n","Ticker", "Region", "Price","Market Cap", "P/E TTM", "PEG TTM", "EV / EBIT", "EV / EBITDA", "P/S TTM", "Revenue Growth TTM", "EPS Growth TTM", "Stock Price Growth TTM");
            JsonArray main_stock = null;
            ArrayList<Double> arr_average_market_cap = new ArrayList<>();
            ArrayList<Double> arr_average_peTtm = new ArrayList<>();
            ArrayList<Double> arr_average_pegTtm = new ArrayList<>();
            ArrayList<Double> arr_average_evebit = new ArrayList<>();
            ArrayList<Double> arr_average_evebitda = new ArrayList<>();
            ArrayList<Double> arr_average_psttm = new ArrayList<>();
            ArrayList<Double> arr_average_rgrowthttm = new ArrayList<>();
            ArrayList<Double> arr_average_eps_growth = new ArrayList<>();
            ArrayList<Double> arr_average_stock_growth = new ArrayList<>();

            for (JsonElement element : results) {
                JsonObject object = element.getAsJsonObject();
                JsonArray element_arr = object.get("d").getAsJsonArray();

                String ticker = safeGetString(element_arr, 0);
                String market = safeGetString(element_arr, 1);
                double price = safeGetDouble(element_arr, 2);
                double market_cap = safeGetDouble(element_arr, 3);
                arr_average_market_cap.add(market_cap);

                double peTtm = safeGetDouble(element_arr, 4);
                arr_average_peTtm.add(peTtm);

                double pegTtm = safeGetDouble(element_arr, 5);
                arr_average_pegTtm.add(pegTtm);

                double evebit = safeGetDouble(element_arr, 6);
                arr_average_evebit.add(evebit);

                double evebitda = safeGetDouble(element_arr, 7);
                arr_average_evebitda.add(evebitda);

                double psttm = safeGetDouble(element_arr, 8);
                arr_average_psttm.add(psttm);

                double rgrowthttm = safeGetDouble(element_arr, 9);
                arr_average_rgrowthttm.add(rgrowthttm);
                String rgrowhttms = "";
                if (rgrowthttm > 0) {
                    rgrowhttms = GREEN + "+" + String.format("%.2f", rgrowthttm) + "%" + RESET;
                }else {
                    rgrowhttms = RED + String.format("%.2f", rgrowthttm) + "%" + RESET;
                }

                double eps_growth = safeGetDouble(element_arr, 10);
                arr_average_eps_growth.add(eps_growth);
                String eps_growths = "";
                if (eps_growth > 0) {
                    eps_growths = GREEN + "+" + String.format("%.2f", eps_growth) + "%" + RESET;
                }else {
                    eps_growths = RED + String.format("%.2f", eps_growth) + "%" + RESET;
                }

                double stock_growth = safeGetDouble(element_arr, 11);
                arr_average_stock_growth.add(stock_growth);
                String stock_growths = "";
                if (stock_growth > 0) {
                    stock_growths = GREEN + "+" + String.format("%.2f", stock_growth) + "%" + RESET;
                }else {
                    stock_growths = RED + String.format("%.2f", stock_growth) + "%" + RESET;
                }


                if (ticker.equals(stock_ticker)) {
                    System.out.printf("%-21s %-12s %-12.2f %-12s %-12.2f %-12.2f %-12.2f %-12.2f %-12.2f %-30s %-28s %-30s%n",
                        GREEN + ticker + RESET,
                        market,
                        price,
                        shorten_num_to_readable(market_cap),
                        peTtm,
                        pegTtm,
                        evebit,
                        evebitda,
                        psttm,
                        rgrowhttms,
                        eps_growths,
                        stock_growths

                    );
                    main_stock = element_arr;
                } else {
                    System.out.printf("%-12s %-12s %-12.2f %-12s %-12.2f %-12.2f %-12.2f %-12.2f %-12.2f %-30s %-28s %-30s%n",
                        ticker,
                        market,
                        price,
                        shorten_num_to_readable(market_cap),
                        peTtm,
                        pegTtm,
                        evebit,
                        evebitda,
                        psttm,
                        rgrowhttms,
                        eps_growths,
                        stock_growths
                    );
                }
            }
            // Calculate  Average 
            double average_market_cap = findmedian(arr_average_market_cap);
            double average_peTtm = findmedian(arr_average_peTtm);
            double average_pegTtm = findmedian(arr_average_pegTtm);
            double average_evebit = findmedian(arr_average_evebit);
            double average_evebitda = findmedian(arr_average_evebitda);
            double average_psttm = findmedian(arr_average_psttm);
            double average_rgrowthttm = findmedian(arr_average_rgrowthttm);
            String average_rgrowthttms = "";
                if (average_rgrowthttm > 0) {
                    average_rgrowthttms = GREEN + "+" + String.format("%.2f", average_rgrowthttm) + "%" + RESET;
                }else if (average_rgrowthttm < 0) {
                    average_rgrowthttms = RED + String.format("%.2f", average_rgrowthttm) + "%" + RESET;
                }

            double average_eps_growth = findmedian(arr_average_eps_growth);
            String average_eps_growths = "";
                if (average_eps_growth > 0) {
                    average_eps_growths = GREEN + "+" + String.format("%.2f", average_eps_growth) + "%" + RESET;
                }else if (average_eps_growth < 0) {
                    average_eps_growths = RED + String.format("%.2f", average_eps_growth) + "%" + RESET;
                }
            
            double average_stock_growth = findmedian(arr_average_stock_growth);
            String average_stock_growths = "";
                if (average_stock_growth > 0) {
                    average_stock_growths = GREEN + "+" + String.format("%.2f", average_stock_growth) + "%" + RESET;
                }else if (average_stock_growth < 0) {
                    average_stock_growths = RED + String.format("%.2f", average_stock_growth) + "%" + RESET;
                }


            System.out.println("");
            System.out.println(YELLOW + "Calculated Average (Method : Median & IQR)" + RESET);
            System.out.printf("%-12s %-12s %-12s %-12s %-12s %-12s %-12s %-12s %-12s %-20s %-20s %-20s%n","Ticker", "Region", "Price","Market Cap", "P/E TTM", "PEG TTM", "EV / EBIT", "EV / EBITDA", "P/S TTM", "Revenue Growth TTM", "EPS Growth TTM", "Stock Price Growth TTM");
                        
            String mainstock_rgrowthttms = "";
                if (safeGetDouble(main_stock, 9) > 0) {
                    mainstock_rgrowthttms = GREEN + "+" + String.format("%.2f", safeGetDouble(main_stock, 9)) + "%" + RESET;
                }else if (safeGetDouble(main_stock, 9) < 0) {
                    mainstock_rgrowthttms = RED + String.format("%.2f", safeGetDouble(main_stock, 9)) + "%" + RESET;
                }

            String mainstock_eps_growths = "";
                if (safeGetDouble(main_stock, 10) > 0) {
                    mainstock_eps_growths = GREEN + "+" + String.format("%.2f", safeGetDouble(main_stock, 10)) + "%" + RESET;
                }else if (safeGetDouble(main_stock, 10) < 0) {
                    mainstock_eps_growths = RED + String.format("%.2f", safeGetDouble(main_stock, 10)) + "%" + RESET;
                }
            
            String mainstock_stock_growths = "";
                if (safeGetDouble(main_stock, 11) > 0) {
                    mainstock_stock_growths = GREEN + "+" + String.format("%.2f", safeGetDouble(main_stock, 11)) + "%" + RESET;
                }else if (safeGetDouble(main_stock, 11) < 0) {
                    mainstock_stock_growths = RED + String.format("%.2f", safeGetDouble(main_stock, 11)) + "%" + RESET;
                }
            
            System.out.printf("%-12s %-12s %-12.2f %-12s %-12.2f %-12.2f %-12.2f %-12.2f %-12.2f %-30s %-28s %-30s%n",
                        safeGetString(main_stock, 0),
                        safeGetString(main_stock, 1),
                        safeGetDouble(main_stock, 2),
                        shorten_num_to_readable(safeGetDouble(main_stock, 3)),
                        safeGetDouble(main_stock, 4),
                        safeGetDouble(main_stock, 5),
                        safeGetDouble(main_stock, 6),
                        safeGetDouble(main_stock, 7),
                        safeGetDouble(main_stock, 8),
                        mainstock_rgrowthttms,
                        mainstock_eps_growths,
                        mainstock_stock_growths
                    );
                    
            System.out.printf("%-12s %-12s %-12s %-12s %-12.2f %-12.2f %-12.2f %-12.2f %-12.2f %-30s %-28s %-30s%n",
                        "Average",
                        "-",
                        "-",
                        shorten_num_to_readable(average_market_cap),
                        average_peTtm,
                        average_pegTtm,
                        average_evebit,
                        average_evebitda,
                        average_psttm,
                        average_rgrowthttms,
                        average_eps_growths,
                        average_stock_growths
                    );
            System.out.println("-".repeat(180));
            System.out.printf("%-12s %-12s %-12s %-12s %-12s %-12s %-12s %-12s %-12s %-20s %-20s %-20s%n",
                        stock_ticker + " / Average",
                        "-",
                        "-",
                       String.format("%.2f",(safeGetDouble(main_stock, 3) / average_market_cap ) * 100) + "%",
                       String.format("%.2f",(safeGetDouble(main_stock, 4) / average_peTtm ) * 100) + "%",
                       String.format("%.2f",(safeGetDouble(main_stock, 5) / average_pegTtm ) * 100) + "%",
                       String.format("%.2f",(safeGetDouble(main_stock, 6) / average_evebit ) * 100 ) + "%",
                       String.format("%.2f",(safeGetDouble(main_stock, 7) / average_evebitda ) * 100 ) + "%",
                       String.format("%.2f",(safeGetDouble(main_stock, 8) / average_psttm ) * 100 ) + "%",
                       String.format("%.2f",(safeGetDouble(main_stock, 9) / average_rgrowthttm ) * 100 ) + "%",
                       String.format("%.2f",(safeGetDouble(main_stock, 10) / average_eps_growth ) * 100 ) + "%",
                       String.format("%.2f",(safeGetDouble(main_stock, 11) / average_stock_growth ) * 100 ) + "%"
            );

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
                    "columns": ["name", "market","close", "market_cap_basic", "price_earnings_ttm","price_earnings_growth_ttm","enterprise_value_to_ebit_ttm","enterprise_value_ebitda_ttm","price_sales_current","total_revenue_yoy_growth_ttm", "earnings_per_share_diluted_yoy_growth_ttm", "Perf.Y"],
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
                    "columns": ["name","market","close", "market_cap_basic", "price_earnings_ttm","price_earnings_growth_ttm","enterprise_value_to_ebit_ttm","enterprise_value_ebitda_ttm","price_sales_current","total_revenue_yoy_growth_ttm","earnings_per_share_diluted_yoy_growth_ttm", "Perf.Y" ],
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
    private static String safeGetString(JsonArray array, int index) {
        return (array.size() > index && !array.get(index).isJsonNull()) ? array.get(index).getAsString() : "";
    }
    private static double safeGetDouble(JsonArray array, int index) {
        return (array.size() > index && !array.get(index).isJsonNull()) ? array.get(index).getAsDouble() : 0.0;
    }
    private static String shorten_num_to_readable(double number) {
        if (number > 1_000_000_000_000L) {
            return String.format("%.2f",(double) number / 1_000_000_000_000L) + "T";
        } else if (number > 1_000_000_000L) {
            return String.format("%.2f",(double) number / 1_000_000_000L) + "B";
        } else if (number > 1_000_000L) {
            return String.format("%.2f",(double) number / 1_000_000L) + "M";
        } else if (number > 1_000_000L) {
            return String.format("%.2f",(double) number / 1_000L) + "K";
        }

        return String.valueOf(number);
    }
    private static double findmedian(ArrayList<Double> values) {
        double[] valuez = values.stream().mapToDouble(Double::doubleValue).toArray();
        Arrays.sort(valuez);
        int n = valuez.length;

        double q1 = valuez[n / 4];
        double q3 = valuez[(3 * n) / 4];
        double iqr = q3 - q1;

        double lowerBound = q1 - 1.5 * iqr;
        double upperBound = q3 + 1.5 * iqr;

        double sum = 0.0;
        int count = 0;
        for (double v : valuez) {
            if (v >= lowerBound && v <= upperBound) {
                sum += v;
                count++;
            }
        }
        return (count == 0) ? Double.NaN : sum / count;
    }
    
}

