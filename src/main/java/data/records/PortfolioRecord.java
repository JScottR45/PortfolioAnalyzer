package data.records;

import data.datapoints.DataPoint;

import java.io.Serializable;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.*;

/**
 * Created by scottreese on 6/9/19.
 *
 * A record type which stores all relevant data corresponding to the portfolio as a whole.
 */
public class PortfolioRecord implements Record {
    private Map<String, Allocation> allocations;
    private List<DataPoint> history;
    private Date lastUpdate;
    private double currPortValue;
    private double currMoneyInvested;
    private boolean isUpdated;

    /**
     * Initializes class member variables.
     */
    public PortfolioRecord() {
        allocations = new HashMap<>();
        history = new ArrayList<>();
        lastUpdate = new Date();
        currPortValue = 0;
        currMoneyInvested = 0;
        isUpdated = false;
    }

    // getter methods for class member variables

    @Override
    public RecordType getType() {
        return RecordType.PORTFOLIO_DATA_RECORD;
    }

    public Map<String, Allocation> getAllocations() {
        return allocations;
    }

    public List<DataPoint> getHistory() {
        return history;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public double getCurrPortValue() {
        return currPortValue;
    }

    public double getCurrMoneyInvested() {
        return currMoneyInvested;
    }

    public boolean isUpdated() {
        return isUpdated;
    }

    /**
     * Adds a new or adjusts an already existing allocation of the portfolio. Occurs when purchasing an asset.
     *
     * @param transactionRecord The transaction record containing all relevant information to adjust the
     * portfolio's allocation for the asset associated with the record.
     */
    public void addAllocation(TransactionRecord transactionRecord) {
        String ticker = transactionRecord.getTicker();
        DecimalFormat df = new DecimalFormat("#.####");
        double numShares = transactionRecord.getNumShares();
        double price = transactionRecord.getPrice();
        double moneyAmount = transactionRecord.getType() == RecordType.DIVIDEND_RECORD ? price : price * numShares;

        df.setRoundingMode(RoundingMode.FLOOR);

        if (allocations.containsKey(ticker)) {
            Allocation allocation = allocations.get(ticker);
            double moneyAmountSum = allocation.getMoneyAmount() + moneyAmount;
            double numSharesSum = allocation.getNumShares() + numShares;
            String roundedNumSharesSum = df.format(numSharesSum);

            if (roundedNumSharesSum.equals("0") || roundedNumSharesSum.equals("-0")) {
                allocations.remove(ticker);
            } else {
                allocation.setMoneyAmount(moneyAmountSum);
                allocation.setNumShares(numSharesSum);

                allocations.put(ticker, allocation);
            }
        } else {
            allocations.put(ticker, new Allocation(moneyAmount, numShares));
        }

        isUpdated = true;
    }

    /**
     * Removes or adjusts an allocation of the portfolio. Occurs when selling an asset. In regards to selling an asset
     * short, an allocation of this asset is actually added to the portfolio but with negative values.
     *
     * @param transactionRecord The transaction record containing all relevant information to adjust the
     * portfolio's allocation for the asset associated with the record.
     */
    public void removeAllocation(TransactionRecord transactionRecord) {
        String ticker = transactionRecord.getTicker();
        DecimalFormat df = new DecimalFormat("#.####");
        double numShares = transactionRecord.getNumShares();
        double price = transactionRecord.getPrice();
        double moneyAmount = transactionRecord.getType() == RecordType.DIVIDEND_RECORD ? price : price * numShares;

        df.setRoundingMode(RoundingMode.FLOOR);

        if (!allocations.containsKey(ticker)) {
            allocations.put(ticker, new Allocation(-moneyAmount, -numShares));
        } else {
            Allocation allocation = allocations.get(ticker);
            double moneyAmountDiff = allocation.getMoneyAmount() - moneyAmount;
            double numSharesDiff = allocation.getNumShares() - numShares;
            String roundedNumSharesDiff = df.format(numSharesDiff);

            if (roundedNumSharesDiff.equals("0") || roundedNumSharesDiff.equals("-0")) {
                allocations.remove(ticker);
            } else {
                allocation.setMoneyAmount(moneyAmountDiff);
                allocation.setNumShares(numSharesDiff);

                allocations.put(ticker, allocation);
            }
        }

        isUpdated = true;
    }

    /**
     * Adds a new segment of historical portfolio data points to the existing history.
     *
     * @param newHistory The segment of historical data points to be added.
     */
    public void addHistory(List<DataPoint> newHistory) {
        if (history.size() > 1) {
            DataPoint lastDataPoint = history.get(history.size() - 1);
            DataPoint firstDataPoint = newHistory.get(0);

            if (lastDataPoint.getDate().equals(firstDataPoint.getDate())) {
                history.remove(history.size() - 1);
            }
        }

        history.addAll(newHistory);
        lastUpdate = new Date();
        isUpdated = true;
        updatePortfolioValueAndMoneyInvested();
    }

    /**
     * Updates an already existing segment of the historical portfolio data.
     *
     * @param updatedHistory The segment of data points which holds the new updated data.
     * @param index The starting index of the segment in the existing list of historical data points that needs to be updated.
     */
    public void updateHistory(List<DataPoint> updatedHistory, int index) {
        if (index < 0) {
            history = updatedHistory;
        } else {
            for (DataPoint dataPoint : updatedHistory) {
                history.set(index, dataPoint);
                index++;
            }
        }

        isUpdated = true;
        updatePortfolioValueAndMoneyInvested();
    }

    /**
     * Removes all data points from historical portfolio data with dates prior to the specified lower bound date.
     *
     * @param lowerBound The date prior to which all data points will be removed.
     */
    public void truncateHistory(LocalDate lowerBound) {
        if (lowerBound == null) {
            allocations = new HashMap<>();
            history = new ArrayList<>();
            lastUpdate = new Date();
            currPortValue = 0;
            currMoneyInvested = 0;
        } else {
            ArrayList<DataPoint> truncatedHistory = new ArrayList<>();

            for (DataPoint dataPoint : history) {
                if (dataPoint.getDate().compareTo(lowerBound) >= 0) {
                    truncatedHistory.add(dataPoint);
                }
            }

            history = truncatedHistory;
        }

        isUpdated = true;
    }

    /**
     * Decides when the portfolio data needs to be updated. Currently set to require an update after 10 minutes.
     *
     * @return True if the portfolio data needs to be updated; false otherwise.
     */
    public boolean updateNeeded() {
        Date currDate = new Date();
        long timeDelta = 10 * 60 * 1000;

        return currDate.getTime() - lastUpdate.getTime() > timeDelta && allocations.size() > 0;
    }

    /**
     * Called to signify that the caller is aware of the fact that the portfolio data was updated and acted on this
     * information accordingly. Only call once everything in the application that needs to be aware of the portfolio's
     * updated state has been notified of the changes.
     */
    public void setCurrent() {
        isUpdated = false;
    }


    /**
     * Called to signify that the portfolio is up to date. This is normally done automatically whenever the portfolio's
     * data is updated. However, in the case of updating errors, this function can be called explicitly to mark the
     * portfolio as current if need be.
     */
    public void setUpdated() {
        lastUpdate = new Date();
        isUpdated = true;
    }

    /**
     * Simple helper function used to update the class member variables for the current portfolio value and current
     * amount of money invested in the portfolio's assets.
     */
    private void updatePortfolioValueAndMoneyInvested() {
        DataPoint dataPoint = history.get(history.size() - 1);

        currPortValue = dataPoint.getMarketCloseValue();
        currMoneyInvested = dataPoint.getMoneyInvested();
    }

    /**
     * Represents an allocation of the portfolio for a specific asset.
     */
    public class Allocation implements Serializable {
        private double moneyAmount;
        private double numShares;

        /**
         * Initializes class member variables.
         *
         * @param moneyAmount The current amount of money invested in the particular asset.
         * @param numShares The number of shares currently owned of the asset.
         */
        Allocation(double moneyAmount, double numShares) {
            this.moneyAmount = moneyAmount;
            this.numShares = numShares;
        }

        // getter methods for class member variables

        public double getMoneyAmount() {
            return moneyAmount;
        }

        public double getNumShares() {
            return numShares;
        }

        // setter methods for class member variables

        void setMoneyAmount(double moneyAmount) {
            this.moneyAmount = moneyAmount;
        }

        void setNumShares(double numShares) {
            this.numShares = numShares;
        }
    }
}
