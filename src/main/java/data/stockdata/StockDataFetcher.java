package data.stockdata;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import error.PAException;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by scottreese on 6/3/19.
 *
 * Responsible for fetching requested data from the online database API.
 */

public class StockDataFetcher {
    private static final String URL = "https://apidojo-yahoo-finance-v1.p.rapidapi.com/stock/";
    private static final String X_RAPIDAPI_HOST = "apidojo-yahoo-finance-v1.p.rapidapi.com";
    private static final String X_RAPIDAPI_KEY = "...INSERT RAPID API KEY HERE...";
    private static final String REGION = "US";
    private static final String LANGUAGE = "en";
    private static final String DIV_EVENT = "div";
    private static final String INTERVAL = "1d";

    // private enum used internally to specify the type of data requested (more types to be added in the future)
    private enum StockDataType {
        HISTORICAL
    }

    /**
     * Makes request to online database API for historical data associated with a particular asset.
     *
     * @param tickers A list of ticker symbols whose historical data is requested.
     * @param from The starting date for the requested historical data.
     * @param to The end date for the requested historical data.
     * @param callback The callback to be used once the data is received or if an error occurs.
     */
    public void fetchHistoricalStockData(List<String> tickers, Date from, Date to, StockDataCallback callback) {
        StockDataDeliverer stockDataDeliverer = new StockDataDeliverer(tickers.size(), StockDataType.HISTORICAL, callback);

        for (String ticker : tickers) {
            String fullURL = URL + "get-histories"
                    + "?region=" + REGION
                    + "&lang=" + LANGUAGE
                    + "&symbol=" + ticker
                    + "&from=" + from.getTime() / 1000
                    + "&to=" + to.getTime() / 1000
                    + "&events=" + DIV_EVENT
                    + "&interval=" + INTERVAL;

            Unirest.get(fullURL)
                    .header("X-RapidAPI-Host", X_RAPIDAPI_HOST)
                    .header("X-RapidAPI-Key", X_RAPIDAPI_KEY)
                    .asJsonAsync(stockDataDeliverer);
        }
    }

    /**
     * The callback class used internally in this class for the purpose of synchronizing multiple threads. Each of
     * these threads would be responsible for making a separate call to the API for data associated with a particular
     * asset. Once all threads have received their respective data, the data is placed into a list which is then handed
     * to the callback originally passed in as an argument for any of the fetch... public methods.
     */
    private class StockDataDeliverer implements Callback<JsonNode> {
        private int numRequests;
        private StockDataType type;
        private StockDataCallback callback;
        private List<StockData> stockData;
        private boolean errorOccurred;

        /**
         * Initializes class member variables.
         *
         * @param numRequests The number of requests needed to be made, each for a specific asset.
         * @param type The type of data requested (e.g. historical).
         * @param callback The callback to be used once all requested data has been received.
         */
        StockDataDeliverer(int numRequests, StockDataType type, StockDataCallback callback) {
            this.numRequests = numRequests;
            this.type = type;
            this.callback = callback;
            this.stockData = new ArrayList<>();
            this.errorOccurred = false;
        }

        /**
         * Called when an error occurs during a request.
         *
         * @param e The specific exception that occurred.
         */
        public void failed(UnirestException e) {
            callback.failed(new PAException(e));
        }

        /**
         * Called when the request is cancelled.
         */
        public void cancelled() {
            callback.failed(new PAException(PAException.Type.REQUEST_CANCELLED, "Request to fetch data was cancelled"));
        }

        /**
         * Called when the requested data has been received.
         *
         * @param httpResponse The HTTP response from the online database API.
         */
        public void completed(HttpResponse<JsonNode> httpResponse) {
            checkData(httpResponse);
        }

        /**
         * The function which synchronizes multiple threads when they receive their respective data, assuming the
         * stock data fetcher was given multiple asset tickers for which to fetch data. Also checks for errors.
         *
         * @param httpResponse The HTTP response from the online database API.
         */
        private synchronized void checkData(HttpResponse<JsonNode> httpResponse) {
            if (!errorOccurred) {
                PAException error;

                error = checkNotFoundError(httpResponse);

                if (error != null) {
                    errorOccurred = true;
                    callback.failed(error);
                    return;
                }

                try {
                    StockData data = null;

                    switch(type) {
                        case HISTORICAL:
                            data = new HistoricalStockData(httpResponse);
                    }

                    stockData.add(data);

                    if (stockData.size() == numRequests) {
                        callback.completed(stockData);
                    }
                } catch (Exception ex) {
                    if (ex instanceof JSONException) {
                        error = new PAException(PAException.Type.WEEKEND_NO_DATA, ex.getMessage());
                    } else {
                        error = new PAException(ex);
                    }

                    errorOccurred = true;
                    callback.failed(error);
                }
            }
        }

        /**
         * Checks if the API response is empty as a result of the asset whose data is requested does not exist or could
         * not be found.
         *
         * @param httpResponse The API response.
         * @return An exception indicating that the asset was not found or does not exist; null otherwise.
         */
        private PAException checkNotFoundError(HttpResponse<JsonNode> httpResponse) {
            JSONObject chart = httpResponse.getBody().getObject().getJSONObject("chart");
            boolean errorOccurred = !chart.isNull("error");

            if (errorOccurred) {
                if (chart.getJSONObject("error").getString("code").equals("Not Found")) {
                    return new PAException(PAException.Type.NOT_FOUND, chart.getJSONObject("error").getString("description"));
                }
            }

            return null;
        }
    }
}
