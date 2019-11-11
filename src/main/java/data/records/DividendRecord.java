package data.records;

import java.time.LocalDate;

/**
 * Created by scottreese on 10/8/19.
 *
 * A record type which stores all relevant data corresponding to a particular dividend reinvestment transaction.
 */
public class DividendRecord extends TransactionRecord {

    /**
     * Initializes class member variables.
     *
     * @param date The date on which this dividend reinvestment transaction occurred.
     * @param ticker The ticker symbol associated with the asset whose dividend is to be reinvested.
     * @param numShares The number of shares of the asset which were purchased from the dividend reinvestment.
     * @param price The price at which the shares were purchased (also the dividend amount).
     */
    public DividendRecord(LocalDate date, String ticker, double numShares, double price) {
        super(date, ticker, numShares, price, true);
    }

    /**
     * Initializes class member variables.
     *
     * @param date The date on which this dividend reinvestment transaction occurred.
     * @param ticker The ticker symbol associated with the asset whose dividend is to be reinvested.
     * @param numShares The number of shares of the asset which were purchased (or sold for undo) from the dividend reinvestment.
     * @param price The price at which the shares were purchased or sold (also the dividend amount).
     * @param isUndo True if this dividend reinvestment record is undoing a previous one; false otherwise.
     */
    public DividendRecord(LocalDate date, String ticker, double numShares, double price, boolean isUndo) {
        super(date, ticker, numShares, price, !isUndo);
    }

    @Override
    public RecordType getType() {
        return RecordType.DIVIDEND_RECORD;
    }
}
