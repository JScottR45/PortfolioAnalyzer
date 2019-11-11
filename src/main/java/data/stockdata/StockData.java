package data.stockdata;

/**
 * Created by scottreese on 6/10/19.
 *
 * Interface representing a container of data for a particular asset. This container is what is handed back after making
 * an API call to the online database to fetch data for an asset.
 */
public interface StockData {

    // getter function to return the ticker symbol for the asset associated with the data

    String getTicker();
}
