package data.stockdata;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import data.datapoints.DataPoint;
import data.datapoints.StockDataPoint;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by scottreese on 6/10/19.
 *
 * A container which holds historical data for a particular asset. This is what is handed back after requesting
 * historical data from the online database.
 */
public class HistoricalStockData implements StockData {
    private String ticker;
    private List<DataPoint> history;

    /**
     * Converts the HTTP response from the online database API into a HistoricalStockData instance.
     *
     * @param httpResponse The response from the database API.
     */
    HistoricalStockData(HttpResponse<JsonNode> httpResponse) {
        history = new ArrayList<>();

        JSONObject result = httpResponse.getBody().getObject()
                .getJSONObject("chart")
                .getJSONArray("result")
                .getJSONObject(0);
        JSONArray historicalOpenData = result
                .getJSONObject("indicators")
                .getJSONArray("quote")
                .getJSONObject(0)
                .getJSONArray("open");
        JSONArray historicalCloseData = result
                .getJSONObject("indicators")
                .getJSONArray("adjclose")
                .getJSONObject(0)
                .getJSONArray("adjclose");
        JSONArray historicalHighData = result
                .getJSONObject("indicators")
                .getJSONArray("quote")
                .getJSONObject(0)
                .getJSONArray("high");
        JSONArray historicalLowData = result
                .getJSONObject("indicators")
                .getJSONArray("quote")
                .getJSONObject(0)
                .getJSONArray("low");
        JSONArray timestamps = result.getJSONArray("timestamp");

        ticker = result.getJSONObject("meta").getString("symbol");

        for (int i = 0; i < timestamps.length(); i++) {
            LocalDate date = Instant.ofEpochMilli(timestamps.getLong(i) * 1000).atZone(ZoneId.systemDefault()).toLocalDate();
            Double openPrice = historicalOpenData.getDouble(i);
            Double closePrice = historicalCloseData.getDouble(i);
            Double high = historicalHighData.getDouble(i);
            Double low = historicalLowData.getDouble(i);

            StockDataPoint dataPoint = new StockDataPoint(date, openPrice, closePrice, high, low);

            history.add(dataPoint);
        }
    }

    // getter methods for class member variables

    public String getTicker() {
        return ticker;
    }

    public List<DataPoint> getHistory() {
        return history;
    }
}
