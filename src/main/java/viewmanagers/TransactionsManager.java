package viewmanagers;

import controllers.PortfolioOverviewController;
import data.datapoints.DataPoint;
import data.datapoints.PortfolioDataPoint;
import data.datapoints.StockDataPoint;
import data.records.PortfolioRecord;
import data.records.RecordType;
import data.records.StockRecord;
import data.records.TransactionRecord;
import data.stockdata.HistoricalStockData;
import data.stockdata.StockData;
import data.stockdata.StockDataCallback;
import data.stockdata.StockDataFetcher;
import error.PAException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import utils.Utils;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by scottreese on 6/17/19.
 *
 * The manager responsible for controlling and updating the transactions table.
 */
public class TransactionsManager {
    private PortfolioOverviewController overviewController;
    private TableView<TableRow> transactionsTable;

    /**
     * Initializes class member variables and does some visual formatting for the table.
     *
     * @param overviewController The controller of the main screen.
     * @param transactionsTable The transactions table used to display the history of all transactions made in the past.
     */
    public TransactionsManager(PortfolioOverviewController overviewController, TableView<TableRow> transactionsTable) {
        this.overviewController = overviewController;
        this.transactionsTable = transactionsTable;

        transactionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<TableRow, String> date = new TableColumn<>("Date");
        date.setCellValueFactory(new PropertyValueFactory<>("date"));
        date.setSortable(false);
        date.impl_setReorderable(false);

        TableColumn<TableRow, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(new PropertyValueFactory<>("type"));
        type.setSortable(false);
        type.impl_setReorderable(false);

        TableColumn<TableRow, String> ticker = new TableColumn<>("Ticker");
        ticker.setCellValueFactory(new PropertyValueFactory<>("ticker"));
        ticker.setSortable(false);
        ticker.impl_setReorderable(false);

        TableColumn<TableRow, String> amount = new TableColumn<>("Amount");
        amount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amount.setSortable(false);
        amount.impl_setReorderable(false);

        TableColumn<TableRow, String> numShares = new TableColumn<>("Shares");
        numShares.setCellValueFactory(new PropertyValueFactory<>("numShares"));
        numShares.setSortable(false);
        numShares.impl_setReorderable(false);

        TableColumn<TableRow, Button> undo = new TableColumn<>();
        undo.setCellValueFactory(new PropertyValueFactory<>("undoButton"));
        undo.setSortable(false);
        undo.impl_setReorderable(false);

        transactionsTable.getColumns().setAll(date, type, ticker, amount, numShares, undo);
    }

    /**
     * Initializes the transactions table with all transactions made in the past.
     *
     * @param transactionRecords A list of transaction records associated with all previously made transactions.
     */
    public void initialize(List<TransactionRecord> transactionRecords) {
        updateTransactionsTable(false, transactionRecords);
    }

    /**
     * Updates the transactions table when a transaction has been made.
     *
     * @param transactionRecords The updated list of transaction records.
     */
    public void update(List<TransactionRecord> transactionRecords) {
        updateTransactionsTable(true, transactionRecords);
    }

    /**
     * Begins the transaction procedure. Fetches asset data if the transaction is for an asset not currently allocated
     * in the portfolio, then updates the portfolio. Otherwise, the portfolio is immediately updated to reflect the
     * transaction.
     *
     * @param portRecord The portfolio record to be updated.
     * @param stockDataRecords A map of all records of assets currently allocated in the portfolio.
     * @param transactionRecords A list of transaction records for all previously made transactions.
     * @param transactionRecord The transaction record which contains all relevant data for this transaction.
     * @param isUndo True if this transaction is meant to undo a previous one; false otherwise.
     */
    public void initiateTransaction(PortfolioRecord portRecord, Map<String, StockRecord> stockDataRecords,
                                    List<TransactionRecord> transactionRecords, TransactionRecord transactionRecord, boolean isUndo) {
        Thread thread = new Thread(() -> {
            String ticker = transactionRecord.getTicker();

            if (!stockDataRecords.containsKey(ticker)) {
                fetchNewStockData(portRecord, stockDataRecords, transactionRecords, transactionRecord);
            } else {
                PAException ex = updateHistories(portRecord, stockDataRecords, transactionRecords, transactionRecord, isUndo);
                notifyOverviewController(transactionRecord, ex);
            }
        });

        thread.start();
    }

    /**
     * The function which actually updates the transactions table to reflect the current state of the portfolio's
     * transactional history.
     *
     * @param isUpdate True if this transactions table manager is completing an update; false if it is initializing.
     * @param transactionRecords A list of transaction records for all previously made transactions.
     */
    private void updateTransactionsTable(boolean isUpdate, List<TransactionRecord> transactionRecords) {
        ObservableList<TableRow> tableRows = FXCollections.observableArrayList();

        for (TransactionRecord transactionRecord : transactionRecords) {
            tableRows.add(new TableRow(transactionRecord));
        }

        transactionsTable.setItems(tableRows);
        overviewController.transactionsManagerFinished(isUpdate);
    }

    /**
     * Fetches at most 10 years worth of historical data for the asset involved in the transaction if the asset is not
     * currently allocated in the portfolio.
     *
     * @param portRecord The portfolio record to be updated.
     * @param stockDataRecords A map of all records of assets currently allocated in the portfolio.
     * @param transactionRecords A list of transaction records for all previously made transactions.
     * @param transactionRecord The transaction record which contains all relevant data for this transaction.
     */
    private void fetchNewStockData(PortfolioRecord portRecord, Map<String, StockRecord> stockDataRecords,
                                   List<TransactionRecord> transactionRecords, TransactionRecord transactionRecord) {
        StockDataFetcher stockDataFetcher = new StockDataFetcher();
        HistoricalStockDataCallback callback = new HistoricalStockDataCallback(portRecord, stockDataRecords, transactionRecords, transactionRecord);
        List<String> ticker = new ArrayList<>();

        ticker.add(transactionRecord.getTicker());
        stockDataFetcher.fetchHistoricalStockData(ticker, new Date(0), new Date(), callback);
    }

    /**
     * Notifies the main screen controller that the transaction has been completed and the portfolio has been updated
     * accordingly.
     *
     * @param transactionRecord The transaction record representing the transaction just committed.
     * @param ex An exception which may have occurred during the transaction.
     */
    private void notifyOverviewController(TransactionRecord transactionRecord, PAException ex) {
        Platform.runLater(() -> overviewController.transactionCommitted(transactionRecord, ex));
    }

    /**
     * The function which actually updates the historical data for both the portfolio as a whole and the individual
     * asset involved in the transaction.
     *
     * @param portRecord The portfolio record to be updated.
     * @param stockDataRecords A map of all records of assets currently allocated in the portfolio.
     * @param transactionRecords A list of transaction records for all previously made transactions.
     * @param transactionRecord The transaction record representing the transaction just committed.
     * @param isUndo True if the histories are being updated to undo a previous transaction; false otherwise.
     */
    private PAException updateHistories(PortfolioRecord portRecord, Map<String, StockRecord> stockDataRecords,
                                        List<TransactionRecord> transactionRecords, TransactionRecord transactionRecord, boolean isUndo) {
        StockRecord stockRecord = stockDataRecords.get(transactionRecord.getTicker());
        List<DataPoint> updatedPortfolioHistory = new ArrayList<>();
        List<DataPoint> updatedStockHistory = new ArrayList<>();
        List<DataPoint> currPortfolioHistory = portRecord.getHistory();
        List<DataPoint> stockDataPoints = getStockDataPoints(stockRecord, transactionRecord);

        if (stockDataPoints == null) {
            return new PAException(PAException.Type.INVALID_TRANSACTION_DATE, "Transaction date either on weekend, holiday, or in future.");
        }

        adjustPortfolioAllocations(portRecord, stockDataRecords, transactionRecord);

        boolean isBuy = transactionRecord.isBuy();
        int portHistoryIndex = currPortfolioHistory.size() - stockDataPoints.size();
        int stockHistoryIndex = stockRecord.getHistory().size() - stockDataPoints.size();
        int j = 0;

        for (int i = portHistoryIndex; i < currPortfolioHistory.size(); i++) {
            StockDataPoint stockDataPoint = (StockDataPoint) stockDataPoints.get(j);
            PortfolioDataPoint newPortfolioDataPoint;
            StockDataPoint newStockDataPoint;

            double portOpenValueDelta = stockDataPoint.getMarketOpenValue() * transactionRecord.getNumShares();
            double portCloseValueDelta = stockDataPoint.getMarketCloseValue() * transactionRecord.getNumShares();
            double moneyInvestedDelta = transactionRecord.getType() == RecordType.DIVIDEND_RECORD ? transactionRecord.getPrice()
                    : transactionRecord.getPrice() * transactionRecord.getNumShares();

            double updatedPortOpenValue;
            double updatedPortCloseValue;
            double updatedPortMoneyInvested;
            double updatedStockMoneyInvested;
            double updatedStockNumShares;

            if (i >= 0) {
                DataPoint currPortDataPoint = currPortfolioHistory.get(i);
                double portOpenValue = currPortDataPoint.getMarketOpenValue();
                double portCloseValue = currPortDataPoint.getMarketCloseValue();
                double portMoneyInvested = currPortDataPoint.getMoneyInvested();

                updatedPortOpenValue = isBuy ? portOpenValue + portOpenValueDelta : portOpenValue - portOpenValueDelta;
                updatedPortCloseValue = isBuy ? portCloseValue + portCloseValueDelta : portCloseValue - portCloseValueDelta;
                updatedPortMoneyInvested = isBuy ? portMoneyInvested + moneyInvestedDelta : portMoneyInvested - moneyInvestedDelta;

                newPortfolioDataPoint = new PortfolioDataPoint(stockDataPoint.getDate(), updatedPortOpenValue, updatedPortCloseValue, updatedPortMoneyInvested);
            } else {
                updatedPortOpenValue = isBuy ? portOpenValueDelta : -portOpenValueDelta;
                updatedPortCloseValue = isBuy ? portCloseValueDelta : -portCloseValueDelta;
                updatedPortMoneyInvested = isBuy ? moneyInvestedDelta : -moneyInvestedDelta;

                newPortfolioDataPoint = new PortfolioDataPoint(stockDataPoint.getDate(), updatedPortOpenValue, updatedPortCloseValue, updatedPortMoneyInvested);
            }

            double stockMoneyInvested = stockDataPoint.getMoneyInvested();
            double stockNumShares = stockDataPoint.getNumShares();

            updatedStockMoneyInvested = isBuy ? stockMoneyInvested + moneyInvestedDelta : stockMoneyInvested - moneyInvestedDelta;
            updatedStockNumShares = isBuy ? stockNumShares + transactionRecord.getNumShares() : stockNumShares - transactionRecord.getNumShares();
            newStockDataPoint = new StockDataPoint(stockDataPoint.getDate(), stockDataPoint.getMarketOpenValue(),
                    stockDataPoint.getMarketCloseValue(), stockDataPoint.getMarketHighValue(), stockDataPoint.getMarketLowValue(),
                    updatedStockMoneyInvested, updatedStockNumShares);

            updatedPortfolioHistory.add(newPortfolioDataPoint);
            updatedStockHistory.add(newStockDataPoint);
            j++;
        }

        portRecord.updateHistory(updatedPortfolioHistory, portHistoryIndex);
        stockRecord.updateHistory(updatedStockHistory, stockHistoryIndex);

        if (!isUndo) {
            transactionRecords.add(transactionRecord);
            transactionRecords.sort(new TransactionRecordComparator());
        } else {
            TransactionRecord earliestRecord = transactionRecords.size() == 0 ? null : transactionRecords.get(transactionRecords.size() - 1);
            portRecord.truncateHistory(earliestRecord == null ? null : earliestRecord.getDate());
        }

        return null;
    }

    /**
     * Adjusts the portfolio's allocation after a transaction to reflect the new state of allocations in the portfolio.
     *
     * @param portRecord The portfolio record whose allocations need to be adjusted.
     * @param stockDataRecords The stock data records.
     * @param transactionRecord The transaction record representing the transaction.
     */
    private void adjustPortfolioAllocations(PortfolioRecord portRecord, Map<String, StockRecord> stockDataRecords,
                                            TransactionRecord transactionRecord) {
        String ticker = transactionRecord.getTicker();

        if (transactionRecord.isBuy()) {
            portRecord.addAllocation(transactionRecord);
        } else {
            portRecord.removeAllocation(transactionRecord);
        }

        if (!portRecord.getAllocations().containsKey(ticker)) {
            stockDataRecords.remove(ticker);
        }
    }

    /**
     * Retrieves the relevant segment of historical data that needs to be updated for the individual asset involved in
     * the transaction.
     *
     * @param stockRecord The record for the asset whose historical data needs to be updated.
     * @param transactionRecord The transaction record representing the transaction just committed.
     *
     * @return The segment of historical data that needs to be updated for the asset.
     */
    private List<DataPoint> getStockDataPoints(StockRecord stockRecord, TransactionRecord transactionRecord) {
        List<DataPoint> stockHistory = stockRecord.getHistory();
        List<DataPoint> dataPoints = new ArrayList<>();

        for (int i = stockHistory.size() - 1; i >= 0; i--) {
            DataPoint dataPoint = stockHistory.get(i);
            dataPoints.add(dataPoint);

            if (dataPoint.getDate().compareTo(transactionRecord.getDate()) == 0) {
                Collections.reverse(dataPoints);
                return dataPoints;
            }
        }

        return null;
    }

    /**
     * Kicks off the transaction undo process.
     *
     * @param transactionRecord The transaction record representing the transaction which needs to be undone.
     */
    private void undoTransaction(TransactionRecord transactionRecord) {
        ObservableList<TableRow> tableRows = transactionsTable.getItems();

        for (int i = tableRows.size() - 1; i >= 0; i--) {
            if (transactionRecord.equals(tableRows.get(i).getTransactionRecord())) {
                tableRows.remove(i);

                break;
            }
        }

        overviewController.undoTransaction(transactionRecord);
    }

    /**
     * Represents a single row of the transactions table.
     */
    public class TableRow {
        private TransactionRecord transactionRecord;
        private Button undoButton;

        /**
         * Initializes class member variable.
         *
         * @param transactionRecord The transaction record that represents a specific transaction.
         */
        TableRow(TransactionRecord transactionRecord) {
            this.transactionRecord = transactionRecord;
            this.undoButton = new Button("Undo");

            undoButton.getStylesheets().add(getClass().getResource("../css/undo_button.css").toExternalForm());
            undoButton.setOnMouseClicked((MouseEvent event) -> undoTransaction(transactionRecord));
        }

        // getter methods for class member variables (stored within the transaction record)

        public String getDate() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yy");

            return transactionRecord.getDate().format(formatter);
        }

        public String getType() {
            return transactionRecord.getType() == RecordType.DIVIDEND_RECORD ? "Div. Reinv."
                    : transactionRecord.isBuy() ? "Buy" : "Sell";
        }

        public String getTicker() {
            return transactionRecord.getTicker();
        }

        public String getAmount() {
            return Utils.formatDollars(transactionRecord.getPrice());
        }

        public String getNumShares() {
            return Utils.roundDecimal(transactionRecord.getNumShares(), 4);
        }

        public Button getUndoButton() {
            return undoButton;
        }

        TransactionRecord getTransactionRecord() {
            return transactionRecord;
        }
    }

    /**
     * Comparator class used to compare transaction records in order to sort them by date.
     */
    private class TransactionRecordComparator implements Comparator<TransactionRecord> {

        @Override
        public int compare(TransactionRecord o1, TransactionRecord o2) {
            if (o1.getDate().compareTo(o2.getDate()) < 0) {
                return 1;
            } else if (o1.getDate().compareTo(o2.getDate()) > 0) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    /**
     * Callback class used to receive historical data for the asset involved in the transaction if it is not currently
     * allocated in the portfolio.
     */
    private class HistoricalStockDataCallback implements StockDataCallback {
        private PortfolioRecord portRecord;
        private Map<String, StockRecord> stockDataRecords;
        private List<TransactionRecord> transactionRecords;
        private TransactionRecord transactionRecord;

        /**
         * Initializes class member variables.
         *
         * @param portRecord The portfolio record.
         * @param stockDataRecords A map of all records of assets currently allocated in the portfolio.
         * @param transactionRecords A list of transaction records for all previously made transactions.
         * @param transactionRecord The transaction record containing all relevant data for the transaction just made.
         */
        HistoricalStockDataCallback(PortfolioRecord portRecord, Map<String, StockRecord> stockDataRecords,
                                    List<TransactionRecord> transactionRecords, TransactionRecord transactionRecord) {
            this.portRecord = portRecord;
            this.stockDataRecords = stockDataRecords;
            this.transactionRecords = transactionRecords;
            this.transactionRecord = transactionRecord;
        }

        @Override
        public void failed(PAException ex) {
            notifyOverviewController(null, ex);
        }

        @Override
        public void completed(List<StockData> data) {
            String ticker = transactionRecord.getTicker();
            HistoricalStockData historicalStockData = (HistoricalStockData) data.get(0);
            StockRecord stockRecord = new StockRecord(ticker);

            stockRecord.addHistory(historicalStockData.getHistory());
            stockDataRecords.put(ticker, stockRecord);

            PAException ex = updateHistories(portRecord, stockDataRecords, transactionRecords, transactionRecord, false);

            if (ex != null) {
                stockDataRecords.remove(ticker);
            }

            notifyOverviewController(transactionRecord, ex);
        }
    }
}
