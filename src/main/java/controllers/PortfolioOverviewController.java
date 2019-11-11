package controllers;

import data.*;
import data.datapoints.DataPoint;
import data.records.*;
import error.PAException;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import utils.Utils;
import viewmanagers.AllocationsManager;
import viewmanagers.PerformanceManager;
import viewmanagers.StatsManager;
import viewmanagers.TransactionsManager;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * Created by scottreese on 6/6/19.
 *
 * The main controller for the entire application. Controls the main screen. All other components of the application
 * (yet to be implemented) will be launched from this controller.
 */
public class PortfolioOverviewController implements Controller {
    @FXML
    private ProgressIndicator leftProgressIndicator;
    @FXML
    private ProgressIndicator rightProgressIndicator;
    @FXML
    private LineChart<String, Double> performanceGraph;
    @FXML
    private PieChart allocationsChart;
    @FXML
    private TableView<AllocationsManager.UpperTableRow> upperAllocationsTable;
    @FXML
    private TableView<AllocationsManager.LowerTableRow> lowerAllocationsTable;
    @FXML
    private TableView<TransactionsManager.TableRow> transactionsTable;
    @FXML
    private TableView<StatsManager.UpperTableRow> upperStatsTable;
    @FXML
    private TableView<StatsManager.LowerTableRow> lowerStatsTable;
    @FXML
    private TextField tickerInput;
    @FXML
    private TextField amountInput;
    @FXML
    private TextField numSharesInput;
    @FXML
    private DatePicker transactionDatePicker;
    @FXML
    private DatePicker fromDatePicker;
    @FXML
    private DatePicker toDatePicker;
    @FXML
    private CheckBox grossProfitsCheckBox;
    @FXML
    private CheckBox percentReturnCheckBox;
    @FXML
    private Label portfolioValue;
    @FXML
    private Label amountInvested;
    @FXML
    private Label totalProfits;
    @FXML
    private Label totalReturn;
    @FXML
    private Label lastUpdate;
    @FXML
    private Button buyButton;
    @FXML
    private Button sellButton;
    @FXML
    private Button dividendButton;
    @FXML
    private Button refreshButton;

    private DiskDataManager diskDataManager;
    private PortfolioRecord portRecord;
    private Map<String, StockRecord> stockRecords;
    private List<TransactionRecord> transactionRecords;

    private AllocationsManager allocationsManager;
    private PerformanceManager performanceManager;
    private TransactionsManager transactionsManager;
    private StatsManager statsManager;

    private TransactionRecord waitingTransaction;
    private Semaphore semaphore;

    private boolean transactionsMade;
    private boolean performanceManagerInitialized;
    private boolean allocationsManagerInitialized;
    private boolean transactionsManagerInitialized;
    private boolean statsManagerInitialized;
    private boolean invalidTickerInput;
    private boolean invalidAmountInput;
    private boolean invalidNumSharesInput;

    /**
     * Initializes class member variables and kicks off threads to read the portfolio record and transaction records.
     */
    @Override
    public void initialize() {
        leftProgressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        rightProgressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

        diskDataManager = new DiskDataManager();
        stockRecords = new HashMap<>();
        transactionRecords = new ArrayList<>();

        allocationsManager = new AllocationsManager(this, allocationsChart, upperAllocationsTable, lowerAllocationsTable);
        performanceManager = new PerformanceManager(this, performanceGraph);
        transactionsManager = new TransactionsManager(this, transactionsTable);
        statsManager = new StatsManager(this, upperStatsTable, lowerStatsTable);

        waitingTransaction = null;
        semaphore = null;

        transactionsMade = false;
        performanceManagerInitialized = false;
        allocationsManagerInitialized = false;
        transactionsManagerInitialized = false;
        statsManagerInitialized = false;
        invalidTickerInput = false;
        invalidAmountInput = false;
        invalidNumSharesInput = false;

        grossProfitsCheckBox.setSelected(true);
        tickerInput.focusedProperty().addListener(new TickerInputChangeListener());
        amountInput.focusedProperty().addListener(new AmountInputChangeListener());
        numSharesInput.focusedProperty().addListener(new NumSharesChangeListener());
        transactionDatePicker.setOnAction((ActionEvent event) -> {showTransactionDatePickerNormal(); performanceGraph.requestFocus();});

        disableButtons();
        readPortfolioDataRecord();
        readTransactionRecords();
    }

    /**
     * Called before the application exits. Saves all new/updated data to disk, then application terminates.
     */
    public void shutdown() {
        System.out.println("Shutting down...");

        if (transactionsMade) {
            semaphore = new Semaphore(-2, true);

            portRecord.setCurrent();
            diskDataManager.writePortfolioDataRecord(portRecord, new PortfolioDataRecordWriteCallback());
            diskDataManager.writeStockDataRecords(new ArrayList<>(stockRecords.values()), new StockDataRecordWriteCallback());
            diskDataManager.writeTransactionRecords(transactionRecords, new TransactionRecordWriteCallback());

            try {
                semaphore.acquire();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
                System.exit(-1);
            }
        } else if (portRecord.isUpdated()) {
            semaphore = new Semaphore(-1, true);

            portRecord.setCurrent();
            diskDataManager.writePortfolioDataRecord(portRecord, new PortfolioDataRecordWriteCallback());
            diskDataManager.writeStockDataRecords(new ArrayList<>(stockRecords.values()), new StockDataRecordWriteCallback());

            try {
                semaphore.acquire();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
                System.exit(-1);
            }
        }
    }

    /**
     * Called by the transactions manager when a transaction (buy or sell) has been completed and all portfolio
     * data has been updated to reflect the transaction. Kicks off a series of updates to each component of the UI to
     * reflect the new state of the portfolio data.
     *
     * NOTE: Must be on UI thread to call this function.
     *
     * @param transactionRecord The transaction record which contains all the information about the transaction.
     * @param ex An exception which may have occurred during the transaction.
     */
    public void transactionCommitted(TransactionRecord transactionRecord, PAException ex) {
        if (ex != null) {
            switch(ex.getType()) {
                case NOT_FOUND:
                    showTickerInputError();
                    break;
                case INVALID_TRANSACTION_DATE:
                    showTransactionDatePickerError();
                    break;
                case MISC:
                    ex.getMiscException().printStackTrace();
                    System.exit(-1);
            }
        } else {
            System.out.println("Transaction successfully committed.");

            transactionsMade = true;

            allocationsManager.update(portRecord);
            performanceManager.update(portRecord);
            transactionsManager.update(transactionRecords);
            statsManager.update(portRecord, stockRecords.get(transactionRecord.getTicker()));

            if (stockRecords.size() == 1) {
                portRecord.setUpdated();
            }

            clearInputs();
            setOverviewValues();
            showFromDateNormal();
            showToDateNormal();
        }

        hideProgressIndicators();
        enableButtons();
    }

    /**
     * Undoes a transaction. Whichever transaction is desired to be reversed, a transaction with the same parameters but
     * opposite type (buy or sell) is created and executed.
     *
     * @param transactionRecord The transaction record representing the transaction to be undone.
     */
    public void undoTransaction(TransactionRecord transactionRecord) {
        System.out.println("Undoing transaction...");

        boolean isBuy = transactionRecord.isBuy();
        TransactionRecord undoTransactionRecord;

        if (transactionRecord.getType() == RecordType.DIVIDEND_RECORD) {
            undoTransactionRecord = new DividendRecord(transactionRecord.getDate(), transactionRecord.getTicker(),
                    transactionRecord.getNumShares(), transactionRecord.getPrice(), true);
        } else {
            undoTransactionRecord = new TransactionRecord(transactionRecord.getDate(),
                    transactionRecord.getTicker(), transactionRecord.getNumShares(), transactionRecord.getPrice(), !isBuy);
        }

        transactionRecords.remove(transactionRecord);
        transactionsManager.initiateTransaction(portRecord, stockRecords, transactionRecords, undoTransactionRecord, true);
    }

    /**
     * Called by the performance manager when it is finished with its initialization or updating procedures.
     * Kicks off the initialization procedure for the stats manager.
     *
     * NOTE: Must be on UI thread to call this function.
     *
     * @param isUpdate True if the performance manager finished updating; false if it finished initializing.
     * @param fromDate The date starting from which data wants to be seen on the performance graph.
     * @param toDate The date ending at which data wants to be seen on the performance graph.
     */
    public void performanceManagerFinished(boolean isUpdate, LocalDate fromDate, LocalDate toDate) {
        fromDatePicker.setValue(fromDate);
        toDatePicker.setValue(toDate);

        if (isUpdate) {
            System.out.println("Performance manager updated.");
        } else {
            System.out.println("Performance manager initialized.");

            performanceManagerInitialized = true;

            if (waitingTransaction != null) {
                statsManager.initialize(portRecord, stockRecords, false);
            } else {
                statsManager.initialize(portRecord, stockRecords, true);
            }
        }
    }

    /**
     * Called by the allocations manager when it is finished with its initialization or updating procedures.
     * If all other managers have finished as well, then the progress indicators are hidden and buttons become clickable.
     *
     * NOTE: Must be on UI thread to call this function.
     *
     * @param isUpdate True if the allocations manager finished updating; false if it finished initializing.
     */
    public void allocationsManagerFinished(boolean isUpdate) {
        if (isUpdate) {
            System.out.println("Allocations manager updated.");
        } else {
            System.out.println("Allocations manager initialized.");

            allocationsManagerInitialized = true;

            if (allInitialized()) {
                hideProgressIndicators();
                setOverviewValues();
                enableButtons();
            }

            // hacky fix to weird UI bug where table header row shifted slightly when scrolling through table
            Platform.runLater(() -> upperAllocationsTable.refresh());
            Platform.runLater(() -> lowerAllocationsTable.refresh());
        }
    }

    /**
     * Called by the transactions manager when it is finished with its initialization or updating procedures.
     * If all other managers have finished as well, then the progress indicators are hidden and buttons become clickable.
     *
     * NOTE: Must be on UI thread to call this function.
     *
     * @param isUpdate True if the transactions manager finished updating; false if it finished initializing.
     */
    public void transactionsManagerFinished(boolean isUpdate) {
        if (isUpdate) {
            System.out.println("Transactions manager updated.");
        } else {
            System.out.println("Transactions manager initialized.");

            transactionsManagerInitialized = true;

            if (allInitialized()) {
                hideProgressIndicators();
                setOverviewValues();
                enableButtons();
            }

            // hacky fix to weird UI bug where table header row shifted slightly when scrolling through table
            Platform.runLater(() -> transactionsTable.refresh());
        }
    }

    /**
     * Called by the stats manager when it is finished with its initialization or updating procedures. If all
     * other managers have finished as well, then progress indicators are hidden and buttons become clickable.
     *
     * NOTE: Must be on UI thread to call this function.
     *
     * @param isUpdate True if the stats manager finished updating; false if it finished initializing.
     */
    public void statsManagerFinished(boolean isUpdate) {
        if (isUpdate) {
            System.out.println("Stats manager updated.");
        } else {
            System.out.println("Stats manager initialized.");

            statsManagerInitialized = true;

            if (waitingTransaction != null) {
                transactionsManager.initiateTransaction(portRecord, stockRecords, transactionRecords, waitingTransaction, false);
                waitingTransaction = null;
            } else if (allInitialized()) {
                hideProgressIndicators();
                setOverviewValues();
                enableButtons();
            }

            // hacky fix to weird UI bug where table header row shifted slightly when scrolling through table
            Platform.runLater(() -> upperStatsTable.refresh());
            Platform.runLater(() -> lowerStatsTable.refresh());
        }
    }

    /**
     * Called when the user clicks the "Refresh" button. Fetches updated data and refreshes UI.
     */
    @FXML
    private void onRefreshButtonClicked() {
        if (portRecord.getHistory().size() > 0) {
            System.out.println("Refreshing data...");

            disableButtons();
            showProgressIndicators();

            performanceManager.initialize(portRecord, stockRecords, true, true);
        }
    }

    /**
     * Called when the user clicks the "Buy" button. Starts the "Buy" transaction procedure.
     */
    @FXML
    private void onBuyButtonClicked() {
        if (validInputs(true)) {
            disableButtons();

            TransactionRecord transactionRecord = createTransactionRecord(true);

            commitTransaction(transactionRecord);
        }
    }

    /**
     * Called when the user clicks the "Sell" button. Starts the "Sell" transaction procedure.
     */
    @FXML
    private void onSellButtonClicked() {
        if (validInputs(false)) {
            disableButtons();

            TransactionRecord transactionRecord = createTransactionRecord(false);

            commitTransaction(transactionRecord);
        }
    }

    /**
     * Called when the user clicks the "Dividend" button. Adds a dividend reinvestment quantity to the portfolio.
     */
    @FXML
    private void onDividendButtonClicked() {
        if (validInputs(true)) {
            disableButtons();

            DividendRecord dividendRecord = createDividendRecord();

            commitTransaction(dividendRecord);
        }
    }

    /**
     * Called when the user clicks the "Gross Profits" check box for the performance graph. Shows a time series of
     * gross profits on the performance graph.
     */
    @FXML
    private void onGrossProfitsChecked() {
        grossProfitsCheckBox.setSelected(true);
        percentReturnCheckBox.setSelected(false);
        performanceManager.setMode(portRecord, PerformanceManager.Mode.GROSS_PROFITS);
    }

    /**
     * Called when the user clicks the "Percent Return" check box for the performance graph. Shows a time series of
     * percent return on the performance graph.
     */
    @FXML
    private void onPercentReturnChecked() {
        percentReturnCheckBox.setSelected(true);
        grossProfitsCheckBox.setSelected(false);
        performanceManager.setMode(portRecord, PerformanceManager.Mode.PERCENT_RETURN);
    }

    /**
     * Called when the user selects a date starting at which to show data on the performance graph. Adjusts the
     * performance graph to reflect this choice.
     */
    @FXML
    private void onFromDateSelected() {
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        if (portRecord.getHistory().size() > 0) {
            if (toDate == null || validDateRange(fromDate, toDate)) {
                showFromDateNormal();
                showToDateNormal();
                performanceManager.setDateBounds(portRecord, fromDate, toDate);
            } else {
                showFromDateError();
            }
        } else {
            fromDatePicker.setValue(null);
        }

        performanceGraph.requestFocus();
    }

    /**
     * Called when the user selects a date ending at which to show data on the performance graph. Adjusts the
     * performance graph to reflect this choice.
     */
    @FXML
    private void onToDateSelected() {
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        if (portRecord.getHistory().size() > 0) {
            if (fromDate == null || validDateRange(fromDate, toDate)) {
                showFromDateNormal();
                showToDateNormal();
                performanceManager.setDateBounds(portRecord, fromDatePicker.getValue(), toDatePicker.getValue());
            } else {
                showToDateError();
            }
        } else {
            toDatePicker.setValue(null);
        }

        performanceGraph.requestFocus();
    }

    /**
     * Determines if the date range selected by the user for the performance graph is valid.
     *
     * @param fromDate The date starting at which to show data on the performance graph.
     * @param toDate The date ending at which to show data on the performance graph.
     * @return True if the selected date range is valid; return false otherwise.
     */
    private boolean validDateRange(LocalDate fromDate, LocalDate toDate) {
        List<DataPoint> history = portRecord.getHistory();
        LocalDate begDate = history.get(0).getDate();
        LocalDate endDate = history.get(history.size() - 1).getDate();

        return fromDate != null && toDate != null && fromDate.compareTo(toDate) <= 0
                && ((fromDate.compareTo(begDate) >= 0 && fromDate.compareTo(endDate) <= 0)
                || (toDate.compareTo(begDate) >= 0 && toDate.compareTo(endDate) <= 0));
    }

    /**
     * Determines if the user inputs for committing a transaction are valid.
     *
     * @return True if they are valid; return false otherwise.
     */
    private boolean validInputs(boolean isBuy) {
        String ticker = tickerInput.getText();
        String amount = amountInput.getText();
        String numShares = numSharesInput.getText();
        boolean validInputs = true;

        if (ticker == null || ticker.length() == 0 || ticker.length() > 4
                || (!isBuy && !stockRecords.containsKey(ticker))) {
            showTickerInputError();
            validInputs = false;
            invalidTickerInput = true;
        } else {
            showTickerInputNormal();
        }

        try {
            if (amount.charAt(0) == '$') {
                amount = amount.substring(1);
            }

            double amountVal = Double.parseDouble(amount);

            if (amountVal <= 0) {
                showAmountInputError();
                invalidAmountInput = true;
                validInputs = false;
            } else {
                showAmountInputNormal();
            }
        } catch (Exception ex) {
            showAmountInputError();
            invalidAmountInput = true;
            validInputs = false;
        }

        try {
            if (numShares.charAt(0) == '$') {
                numShares = numShares.substring(1);
            }

            double numSharesVal = Double.parseDouble(numShares);
            PortfolioRecord.Allocation allocation = portRecord.getAllocations().get(ticker);

            if (numSharesVal <= 0 || (allocation != null && allocation.getNumShares() < numSharesVal)) {
                showNumSharesError();
                invalidNumSharesInput = true;
                validInputs = false;
            } else {
                showNumSharesNormal();
            }
        } catch (Exception ex) {
            showNumSharesError();
            invalidNumSharesInput = true;
            validInputs = false;
        }

        LocalDate date = transactionDatePicker.getValue();

        if (date == null) {
            showTransactionDatePickerError();
            validInputs = false;
        } else {
            showTransactionDatePickerNormal();
        }

        return validInputs;
    }

    /**
     * Display the progress indicators at the top of the UI.
     */
    private void showProgressIndicators() {
        leftProgressIndicator.setVisible(true);
        rightProgressIndicator.setVisible(true);
    }

    /**
     * Hide the progress indicators at the top of the UI.
     */
    private void hideProgressIndicators() {
        leftProgressIndicator.setVisible(false);
        rightProgressIndicator.setVisible(false);
    }

    /**
     * Called every time a manager finishes its initialization procedures.
     *
     * @return True if all managers have been initialized; false otherwise.
     */
    private boolean allInitialized() {
       return performanceManagerInitialized && allocationsManagerInitialized && transactionsManagerInitialized && statsManagerInitialized;
    }

    /**
     * Initiates the transaction procedure.
     *
     * @param transactionRecord The transaction record which contains all relevant information for this transaction.
     */
    private void commitTransaction(TransactionRecord transactionRecord) {
        System.out.println("Initiating transaction...");

        showProgressIndicators();

        if (!stockRecords.containsKey(transactionRecord.getTicker())) {
            if (portRecord.getHistory().size() > 0) {
                waitingTransaction = transactionRecord;
                performanceManager.initialize(portRecord, stockRecords, true, false);
            } else {
                transactionsManager.initiateTransaction(portRecord, stockRecords, transactionRecords, transactionRecord, false);
            }
        } else {
            transactionsManager.initiateTransaction(portRecord, stockRecords, transactionRecords, transactionRecord, false);
        }
    }

    /**
     * Creates a transaction record by collecting all user input for a particular transaction.
     *
     * @param isBuy True if the user initiated a "Buy" transaction; false if it is a "Sell" transaction.
     *
     * @return A transaction record object which contains all the information inputted by the user for the transaction.
     */
    private TransactionRecord createTransactionRecord(boolean isBuy) {
        String ticker = tickerInput.getText().toUpperCase();
        String amountStr = amountInput.getText();
        double numShares = Double.parseDouble(numSharesInput.getText());

        if (amountStr.charAt(0) == '$') {
            amountStr = amountStr.substring(1);
        }

        double price = Double.parseDouble(amountStr);

        return new TransactionRecord(transactionDatePicker.getValue(), ticker, numShares, price, isBuy);
    }

    /**
     * Creates a dividend record by collecting all user input for a dividend reinvestment transaction.
     *
     * @return A dividend record object which contains all the information inputted by the user for the dividend transaction.
     */
    private DividendRecord createDividendRecord() {
        String ticker = tickerInput.getText().toUpperCase();
        String priceStr = amountInput.getText();
        double numShares = Double.parseDouble(numSharesInput.getText());
        
        if (priceStr.charAt(0) == '$') {
            priceStr = priceStr.substring(1);
        }

        double price = Double.parseDouble(priceStr);

        return new DividendRecord(transactionDatePicker.getValue(), ticker, numShares, price);
    }

    /**
     * Initializes and formats the portfolio overview values at the top-left of the UI (portfolio value, amount invested,
     * total profits, and total return).
     */
    private void setOverviewValues() {
        if (portRecord.getHistory().size() > 0) {
            double currPortfolioValue = portRecord.getCurrPortValue();
            double currAmountInvested = portRecord.getCurrMoneyInvested() < 0 ? 0 : portRecord.getCurrMoneyInvested();
            double totProfits = portRecord.getCurrMoneyInvested() < 0 ? currPortfolioValue
                    + Math.abs(portRecord.getCurrMoneyInvested()) : currPortfolioValue - currAmountInvested;
            double totReturn = portRecord.getCurrMoneyInvested() < 0
                    ? Double.MAX_VALUE : (currPortfolioValue - currAmountInvested) / currAmountInvested * 100;
            String currPortfolioValueStr = Utils.formatDollars(currPortfolioValue);
            String currAmountInvestedStr = Utils.formatDollars(currAmountInvested);
            String totProfitsStr = Utils.formatDollars(totProfits);
            String totReturnStr = totReturn == Double.MAX_VALUE ? "-" : Utils.formatPercentage(totReturn);
            SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yyyy h:mm a");

            portfolioValue.setText(currPortfolioValueStr);
            amountInvested.setText(currAmountInvestedStr);
            totalProfits.setText(totProfitsStr);
            totalReturn.setText(totReturnStr);

            if (currPortfolioValue >= 0) {
                if (currPortfolioValueStr.equals("$0.00")) {
                    portfolioValue.setStyle("-fx-text-fill: white;");
                } else {
                    portfolioValue.setStyle("-fx-text-fill: #00e600;");
                }
            } else {
                portfolioValue.setStyle("-fx-text-fill: #ff4c00;");
            }

            if (currAmountInvested >= 0) {
                if (currAmountInvestedStr.equals("$0.00")) {
                    amountInvested.setStyle("-fx-text-fill: white;");
                } else {
                    amountInvested.setStyle("-fx-text-fill: #00e600;");
                }
            } else {
                amountInvested.setStyle("-fx-text-fill: #ff4c00;");
            }

            if (totProfits >= 0) {
                if (totProfitsStr.equals("$0.00")) {
                    totalProfits.setStyle("-fx-text-fill: white;");
                } else {
                    totalProfits.setStyle("-fx-text-fill: #00e600;");
                }
            } else {
                totalProfits.setStyle("-fx-text-fill: #ff4c00;");
            }

            if (totReturn == Double.MAX_VALUE) {
                totalReturn.setStyle("-fx-text-fill: white;");
            } else if (totReturn >= 0.0) {
                if (totReturnStr.equals("0.00%")) {
                    totalReturn.setStyle("-fx-text-fill: white;");
                } else {
                    totalReturn.setStyle("-fx-text-fill: #00e600;");
                }
            } else {
                totalReturn.setStyle("-fx-text-fill: #ff4c00;");
            }

            lastUpdate.setText(dateFormat.format(portRecord.getLastUpdate()));
        } else {
            portfolioValue.setText("-");
            portfolioValue.setStyle("-fx-text-fill: white;");

            amountInvested.setText("-");
            amountInvested.setStyle("-fx-text-fill: white;");

            totalProfits.setText("-");
            totalProfits.setStyle("-fx-text-fill: white;");

            totalReturn.setText("-");
            totalReturn.setStyle("-fx-text-fill: white;");

            lastUpdate.setText("-");
        }
    }

    /**
     * Clears and resets all input fields for the transaction component of the UI.
     */
    private void clearInputs() {
        tickerInput.clear();
        numSharesInput.clear();
        amountInput.clear();
        transactionDatePicker.getEditor().clear();
        transactionDatePicker.setValue(null);

        tickerInput.setPromptText("Ticker");
        numSharesInput.setPromptText("Number of Shares");
        amountInput.setPromptText("Amount");
        transactionDatePicker.setPromptText("Date");
    }

    /**
     * Disables all buttons on the UI.
     */
    private void disableButtons() {
        buyButton.setDisable(true);
        sellButton.setDisable(true);
        dividendButton.setDisable(true);
        refreshButton.setDisable(true);
    }

    /**
     * Enables all buttons on the UI.
     */
    private void enableButtons() {
        buyButton.setDisable(false);
        sellButton.setDisable(false);
        dividendButton.setDisable(false);
        refreshButton.setDisable(false);
    }

    /**
     * Kicks off a thread to read the portfolio record from disk into memory.
     */
    private void readPortfolioDataRecord() {
        System.out.println("Reading portfolio record from disk...");
        diskDataManager.readPortfolioDataRecord(new PortfolioDataRecordReadCallback());
    }

    /**
     * Kicks off a thread to read all stock records from disk into memory.
     */
    private void readStockDataRecords() {
        System.out.println("Reading stock records from disk...");
        diskDataManager.readStockDataRecords(new StockDataRecordReadCallback());
    }

    /**
     * Kicks off a thread to read all transaction records from disk into memory.
     */
    private void readTransactionRecords() {
        System.out.println("Reading transaction records from disk...");
        diskDataManager.readTransactionRecords(new TransactionRecordReadCallback());
    }

    // methods for displaying or removing error indicators on user inputs

    private void showFromDateError() {
        fromDatePicker.getEditor().setStyle("-fx-border-color: #ff4c00;");
    }

    private void showFromDateNormal() {
        fromDatePicker.getEditor().setStyle("-fx-border-color: #4d4d4d;");
    }

    private void showToDateError() {
        toDatePicker.getEditor().setStyle("-fx-border-color: #ff4c00;");
    }

    private void showToDateNormal() {
        toDatePicker.getEditor().setStyle("-fx-border-color: #4d4d4d;");
    }

    private void showTickerInputError() {
        tickerInput.setStyle("-fx-border-color: #ff4c00;");
    }

    private void showTickerInputNormal() {
        tickerInput.setStyle("-fx-border-color: #4d4d4d;");
    }

    private void showAmountInputError() {
        amountInput.setStyle("-fx-border-color: #ff4c00;");
    }

    private void showAmountInputNormal() {
        amountInput.setStyle("-fx-border-color: #4d4d4d;");
    }

    private void showNumSharesError() {
        numSharesInput.setStyle("-fx-border-color: #ff4c00;");
    }

    private void showNumSharesNormal() {
        numSharesInput.setStyle("-fx-border-color: #4d4d4d;");
    }

    private void showTransactionDatePickerError() {
        transactionDatePicker.getEditor().setStyle("-fx-border-color: #ff4c00;");
    }

    private void showTransactionDatePickerNormal() {
        transactionDatePicker.getEditor().setStyle("-fx-border-color: #4d4d4d;");
    }

    /**
     * Does error-checking and input formatting for ticker input in transaction component of UI.
     */
    private class TickerInputChangeListener implements ChangeListener<Boolean> {

        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            String input = tickerInput.getText();

            if (input.length() > 4) {
                tickerInput.setText(input.toUpperCase());
                showTickerInputError();
            } else {
                if (input.length() > 0) {
                    tickerInput.setText(input.toUpperCase());
                    invalidTickerInput = false;
                } else if (newValue) {
                    tickerInput.setPromptText(null);
                } else {
                    tickerInput.setPromptText("Ticker");
                }

                if (!invalidTickerInput) {
                    showTickerInputNormal();
                }
            }
        }
    }

    /**
     * Does error-checking and input formatting for amount input in transaction component of UI.
     */
    private class AmountInputChangeListener implements  ChangeListener<Boolean> {

        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            String input = amountInput.getText();

            if (input.length() > 0 && !input.equals("$")) {
                if (input.charAt(0) == '$') {
                    input = input.substring(1);
                }

                try {
                    double amount = Double.parseDouble(input);

                    amountInput.setText(Utils.formatDollars(amount));

                    if (amount <= 0) {
                        showAmountInputError();
                    } else {
                        showAmountInputNormal();
                        invalidAmountInput = false;
                    }
                } catch (Exception ex) {
                    showAmountInputError();
                }
            } else {
                if (newValue) {
                    amountInput.setText("$");
                } else {
                    amountInput.clear();
                    amountInput.setPromptText("Amount");
                }

                if (!invalidAmountInput) {
                    showAmountInputNormal();
                }
            }
        }
    }

    /**
     * Does error-checking and input formatting for number of shares input in transaction component of UI.
     */
    private class NumSharesChangeListener implements ChangeListener<Boolean> {

        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            String input = numSharesInput.getText();

            if (input.length() > 0) {
                try {
                    double numShares = Double.parseDouble(input);

                    numSharesInput.setText(Utils.roundDecimal(numShares, 4));

                    if (numShares <= 0) {
                        showNumSharesError();
                    } else {
                        showNumSharesNormal();
                        invalidNumSharesInput = false;
                    }
                } catch (Exception ex) {
                    showNumSharesError();
                }
            } else {
                if (newValue) {
                    numSharesInput.setPromptText(null);
                } else {
                    numSharesInput.setPromptText("Number of Shares");
                }

                if (!invalidNumSharesInput) {
                    showNumSharesNormal();
                }
            }
        }
    }

    /**
     * Callback class invoked when the portfolio record is finished being read from disk. Kicks off another thread to read
     * the stock records from disk and starts the initialization procedure for the allocations manager if the read
     * is successful.
     */
    private class PortfolioDataRecordReadCallback implements RecordCallback {

        @Override
        public void failed(Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }

        @Override
        public void completed(List<Record> records) {
            System.out.println("Successfully read portfolio record from disk.");
            portRecord = (PortfolioRecord) records.get(0);

            readStockDataRecords();
            Platform.runLater(() -> allocationsManager.initialize(portRecord));
        }
    }

    /**
     * Callback class invoked when the portfolio record is finished being written to disk.
     */
    private class PortfolioDataRecordWriteCallback implements RecordCallback {

        @Override
        public void failed(Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }

        @Override
        public void completed(List<Record> records) {
            System.out.println("Portfolio record successfully written to disk.");
            semaphore.release();
        }
    }

    /**
     * Callback class invoked when all stock records are finished being read from disk. Starts the initialization procedure
     * for the performance manager if the read is successful.
     */
    private class StockDataRecordReadCallback implements RecordCallback {

        @Override
        public void failed(Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }

        @Override
        public void completed(List<Record> records) {
            System.out.println("Successfully read stock records from disk.");

            for (Record record : records) {
                StockRecord stockRecord = (StockRecord) record;

                stockRecords.put(stockRecord.getTicker(), stockRecord);
            }

            Platform.runLater(() -> performanceManager.initialize(portRecord, stockRecords, false, true));
        }
    }

    /**
     * Callback class invoked when all stock records are finished being written to disk.
     */
    private class StockDataRecordWriteCallback implements RecordCallback {

        @Override
        public void failed(Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }

        @Override
        public void completed(List<Record> records) {
            System.out.println("Stock records successfully written to disk.");
            semaphore.release();
        }
    }

    /**
     * Callback class invoked when all transaction records are finished being read from disk. Starts the initialization
     * procedure for the transactions manager if the read is successful.
     */
    private class TransactionRecordReadCallback implements RecordCallback {

        @Override
        public void failed(Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }

        @Override
        public void completed(List<Record> records) {
            System.out.println("Successfully read transaction records from disk.");

            for (Record record : records) {
                transactionRecords.add((TransactionRecord) record);
            }

            Platform.runLater(() -> transactionsManager.initialize(transactionRecords));
        }
    }

    /**
     * Callback class invoked when all transaction records are finished being written to disk.
     */
    private class TransactionRecordWriteCallback implements RecordCallback {

        @Override
        public void failed(Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        }

        @Override
        public void completed(List<Record> records) {
            System.out.println("Transaction records successfully written to disk.");
            semaphore.release();
        }
    }
}
