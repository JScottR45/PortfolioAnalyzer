package data.datapoints;

import java.time.LocalDate;

/**
 * Created by scottreese on 7/23/19.
 *
 * Represents a data point for a specific date for an individual asset. See DataPoint.java for more detailed documentation.
 */
public class StockDataPoint extends DataPoint {
    private double marketHighValue;
    private double marketLowValue;
    private double numShares;

    public StockDataPoint(LocalDate date, double marketOpenValue, double marketCloseValue, double marketHighValue,
                          double marketLowValue, double moneyInvested, double numShares) {
        super(date, marketOpenValue, marketCloseValue, moneyInvested);

        this.numShares = numShares;
        this.marketHighValue = marketHighValue;
        this.marketLowValue = marketLowValue;
    }

    public StockDataPoint(LocalDate date, double marketOpenValue, double marketCloseValue, double marketHighValue, double marketLowValue) {
        super(date, marketOpenValue, marketCloseValue, 0);

        this.numShares = 0;
        this.marketHighValue = marketHighValue;
        this.marketLowValue = marketLowValue;
    }

    // getter methods for class member variables

    public double getMarketHighValue() {
        return marketHighValue;
    }

    public double getMarketLowValue() {
        return marketLowValue;
    }

    public double getNumShares() {
        return numShares;
    }

    // used for debugging purposes
    @Override
    public String toString() {
        return date + ": (Open: " + getMarketOpenValue() + ", Close: " + getMarketCloseValue() + ", No. Shares: " + getNumShares() + ")";
    }
}
