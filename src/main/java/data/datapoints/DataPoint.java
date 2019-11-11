package data.datapoints;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Created by scottreese on 7/23/19.
 *
 * Represents a data point for a specific date. Meant to be used in a list structure to form a history of data for the
 * portfolio as a whole or for individual assets.
 */
public abstract class DataPoint implements Serializable {
    protected LocalDate date;
    private double marketOpenValue;
    private double marketCloseValue;
    private double moneyInvested;

    /**
     * Initializes class member variables.
     *
     * @param date The specific date associated with this data point.
     * @param marketOpenValue The market open price/value for the portfolio or the individual asset on the specified date.
     * @param marketCloseValue The market close price/value for the portfolio or the individual asset on the specified date.
     * @param moneyInvested The total amount of money invested in the portfolio or the individual asset on the specified date.
     */
    DataPoint(LocalDate date, double marketOpenValue, double marketCloseValue, double moneyInvested) {
        this.date = date;
        this.marketOpenValue = marketOpenValue;
        this.marketCloseValue = marketCloseValue;
        this.moneyInvested = moneyInvested;
    }

    // getter methods for member variables

    public LocalDate getDate() {
        return date;
    }

    public double getMarketOpenValue() {
        return marketOpenValue;
    }

    public double getMarketCloseValue() {
        return marketCloseValue;
    }

    public double getMoneyInvested() {
        return moneyInvested;
    }

    // used for debugging purposes
    @Override
    public String toString() {
        return date + ": (Open: " + getMarketOpenValue() + ", Close: " + getMarketCloseValue() + ")";
    }
}
