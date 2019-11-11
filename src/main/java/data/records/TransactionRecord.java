package data.records;

import java.time.LocalDate;

/**
 * Created by scottreese on 6/14/19.
 *
 * A record type which stores all relevant data corresponding to a particular transaction.
 */
public class TransactionRecord implements Record {
    private LocalDate date;
    private String ticker;
    private double numShares;
    private double price;
    private boolean isBuy;

    /**
     * Initializes class member variables.
     *
     * @param date The date on which this transaction occurred.
     * @param ticker The ticker symbol associated with the asset that was bought or sold for this transaction.
     * @param numShares The number of shares of the asset which were bought or sold.
     * @param price The price at which the asset was purchased or sold.
     * @param isBuy True if this transaction is a "Buy"; false if it is a "Sell".
     */
    public TransactionRecord(LocalDate date, String ticker, double numShares, double price, boolean isBuy) {
        this.date = date;
        this.ticker = ticker;
        this.numShares = numShares;
        this.price = price;
        this.isBuy = isBuy;
    }

    // getter methods for class member variables

    @Override
    public RecordType getType() {
        return RecordType.TRANSACTION_RECORD;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getTicker() {
        return ticker;
    }

    public double getNumShares() {
        return numShares;
    }

    public double getPrice() {
        return price;
    }

    public boolean isBuy() {
        return isBuy;
    }
}
