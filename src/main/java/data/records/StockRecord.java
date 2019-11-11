package data.records;

import data.datapoints.DataPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by scottreese on 6/10/19.
 *
 * A record type which stores all relevant data corresponding to a particular asset.
 */
public class StockRecord implements Record {
    private static int NUM_ENTRIES = 2520; // number of trading days in 10 years

    private String ticker;
    private List<DataPoint> history;

    /**
     * Initializes class member variables.
     *
     * @param ticker The ticker symbol of the asset associated with this record.
     */
    public StockRecord(String ticker) {
        this.ticker = ticker;
        this.history = new ArrayList<>();
    }

    // getter methods for class member variables

    @Override
    public RecordType getType() {
        return RecordType.STOCK_DATA_RECORD;
    }

    public String getTicker() {
        return ticker;
    }

    public List<DataPoint> getHistory() {
        return history;
    }

    /**
     * Adds a new segment of historical data points to the existing history for the asset.
     *
     * @param newHistory The segment of historical data points to be added.
     */
    public void addHistory(List<DataPoint> newHistory) {
        if (history.size() > 0) {
            DataPoint lastDataPoint = history.get(history.size() - 1);
            DataPoint firstDataPoint = newHistory.get(0);

            if (lastDataPoint.getDate().equals(firstDataPoint.getDate())) {
                history.remove(lastDataPoint);
            }
        }

        history.addAll(newHistory);
        resizeHistory();
    }

    /**
     * Updates an already existing segment of the historical asset data.
     *
     * @param updatedHistory The segment of data points which holds the new updated data.
     * @param index The starting index of the segment in the existing list of historical data points that needs to be updated.
     */
    public void updateHistory(List<DataPoint> updatedHistory, int index) {
        for (DataPoint dataPoint : updatedHistory) {
            history.set(index, dataPoint);
            index++;
        }
    }

    /**
     * Resizes the list of historical data points to contain at most 10 years worth of data. Any data points dated
     * prior to 10 years from the current date are removed from the list.
     */
    private void resizeHistory() {
        if (history.size() > NUM_ENTRIES) {
            List<DataPoint> resizedHistory = new ArrayList<>();

            for (int i = history.size() - NUM_ENTRIES; i < history.size(); i++) {
                resizedHistory.add(history.get(i));
            }

            history = resizedHistory;
        }
    }
}
