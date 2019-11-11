package data.stockdata;

import error.PAException;

import java.util.List;

/**
 * Created by scottreese on 6/10/19.
 *
 * Interface to be implemented to create callback instances for receiving data from the online database API.
 */
public interface StockDataCallback {

    /**
     * Called when receiving data from the online database API fails.
     *
     * @param ex The specific exception that occurred during the retrieval of data from the API.
     */
    void failed(PAException ex);

    /**
     * Called when receiving data from the online database API is successful.
     *
     * @param data A list of the data containers holding the data requested from the API.
     */
    void completed(List<StockData> data);
}
