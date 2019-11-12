package viewmanagers;

import controllers.PortfolioOverviewController;
import data.datapoints.DataPoint;
import data.datapoints.StockDataPoint;
import data.records.PortfolioRecord;
import data.records.StockRecord;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import utils.Utils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Created by scottreese on 7/4/19.
 *
 * The manager responsible for controlling and updating the portfolio statistics component of the main screen.
 */
public class StatsManager {
    private PortfolioOverviewController overviewController;
    private TableView<UpperTableRow> upperStatsTable;
    private TableView<LowerTableRow> lowerStatsTable;
    private ObservableList<UpperTableRow> waitingUpperTableRows;
    private ObservableList<LowerTableRow> waitingLowerTableRows;

    /**
     * Initializes class member variables and does some visual formatting for the table.
     *
     * @param overviewController The controller of the main screen.
     * @param upperStatsTable The table used to display percentage gains and/or losses for the portfolio.
     * @param lowerStatsTable The table used to display 52 week statistics.
     */
    public StatsManager(PortfolioOverviewController overviewController, TableView<UpperTableRow> upperStatsTable,
                        TableView<LowerTableRow> lowerStatsTable) {
        this.overviewController = overviewController;
        this.upperStatsTable = upperStatsTable;
        this.lowerStatsTable = lowerStatsTable;
        this.waitingUpperTableRows = null;
        this.waitingLowerTableRows = null;

        initUpperTable();
        initLowerTable();
    }

    /**
     * Initializes the statistics tables with current portfolio data.
     *
     * @param portRecord The portfolio record containing the relevant data.
     * @param stockDataRecords A map containing all records of assets currently allocated in the portfolio.
     * @param updateUI True if the caller wants the stats tables to be updated visually after initialization; false otherwise.
     */
    public void initialize(PortfolioRecord portRecord, Map<String, StockRecord> stockDataRecords, boolean updateUI) {
        if (portRecord.getHistory().size() == 0 || stockDataRecords.size() == 0) {
            overviewController.statsManagerFinished(false);
            return;
        }

        Thread thread = new Thread(() -> {
            List<UpperTableRow> upperTableRows = new ArrayList<>();
            List<LowerTableRow> lowerTableRows = new ArrayList<>();

            upperTableRows.add(new UpperTableRow(portRecord));

            for (String ticker : stockDataRecords.keySet()) {
                upperTableRows.add(new UpperTableRow(stockDataRecords.get(ticker)));
                lowerTableRows.add(new LowerTableRow(stockDataRecords.get(ticker)));
            }

            if (updateUI) {
                Platform.runLater(() -> updateStatsTables(false, upperTableRows, lowerTableRows));
            } else {
                waitingUpperTableRows = FXCollections.observableArrayList(upperTableRows);
                waitingLowerTableRows = FXCollections.observableArrayList(lowerTableRows);
                overviewController.statsManagerFinished(false);
            }
        });

        thread.start();
    }

    /**
     * Updates the statistics tables to reflect any updates to the historical portfolio data.
     *
     * @param portRecord The portfolio record containing the updated historical data.
     * @param stockRecord The stock record responsible for the change in the portfolio data.
     */
    public void update(PortfolioRecord portRecord, StockRecord stockRecord) {
        List<UpperTableRow> updatedUpperTableRows = new ArrayList<>();
        List<LowerTableRow> updatedLowerTableRows = new ArrayList<>();
        ObservableList<UpperTableRow> currUpperTableRows = waitingUpperTableRows != null ?
                waitingUpperTableRows : upperStatsTable.getItems();
        ObservableList<LowerTableRow> currLowerTableRows = waitingLowerTableRows != null ?
                waitingLowerTableRows : lowerStatsTable.getItems();
        boolean newAsset = stockRecord != null;

        if (portRecord.getAllocations().size() == 0) {
            updateStatsTables(true, updatedUpperTableRows, updatedLowerTableRows);

            return;
        }

        updatedUpperTableRows.add(new UpperTableRow(portRecord));

        for (int i = 1; i < currUpperTableRows.size(); i++) {
            UpperTableRow upperTableRow = currUpperTableRows.get(i);
            LowerTableRow lowerTableRow = currLowerTableRows.get(i - 1);

            if (stockRecord != null && upperTableRow.getTicker().equals(stockRecord.getTicker())) {
                newAsset = false;
                updatedUpperTableRows.add(upperTableRow);
                updatedLowerTableRows.add(lowerTableRow);
            } else if (portRecord.getAllocations().containsKey(upperTableRow.getTicker())) {
                updatedUpperTableRows.add(upperTableRow);
                updatedLowerTableRows.add(lowerTableRow);
            }
        }

        if (newAsset) {
            updatedUpperTableRows.add(new UpperTableRow(stockRecord));
            updatedLowerTableRows.add(new LowerTableRow(stockRecord));
        }

        updateStatsTables(true, updatedUpperTableRows, updatedLowerTableRows);

        if (waitingUpperTableRows != null) {
            waitingUpperTableRows = null;
        }

        if (waitingLowerTableRows != null) {
            waitingLowerTableRows = null;
        }
    }

    /**
     * The helper function which actually updates the statistics table.
     *
     * @param isUpdate True if this stats table manager is completing an update; false if it is initializing.
     * @param upperTableRows The updated table rows.
     */
    private void updateStatsTables(boolean isUpdate, List<UpperTableRow> upperTableRows, List<LowerTableRow> lowerTableRows) {
        ObservableList<UpperTableRow> upperStats = FXCollections.observableArrayList(upperTableRows);
        ObservableList<LowerTableRow> lowerStats = FXCollections.observableArrayList(lowerTableRows);

        upperStats.sort(new UpperTableRowComparator());
        lowerStats.sort(new LowerTableRowComparator());

        upperStatsTable.setItems(upperStats);
        lowerStatsTable.setItems(lowerStats);
        overviewController.statsManagerFinished(isUpdate);
    }

    /**
     * Initializes the table which displays percentage gains and/or losses for the portfolio.
     */
    private void initUpperTable() {
        upperStatsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<UpperTableRow, String> ticker = new TableColumn<>();
        ticker.setCellValueFactory(new PropertyValueFactory<>("ticker"));
        ticker.setSortable(false);
        ticker.impl_setReorderable(false);

        TableColumn<UpperTableRow, String> value = new TableColumn<>("Value");
        value.setCellValueFactory(new PropertyValueFactory<>("value"));
        value.setSortable(false);
        value.impl_setReorderable(false);

        TableColumn<UpperTableRow, String> dayGL = new TableColumn<>("Day G/L");
        dayGL.setCellValueFactory(new PropertyValueFactory<>("dayGL"));
        dayGL.setCellFactory(new StatCellFactory());
        dayGL.setSortable(false);
        dayGL.impl_setReorderable(false);

        TableColumn<UpperTableRow, String> monthGL = new TableColumn<>("Month G/L");
        monthGL.setCellValueFactory(new PropertyValueFactory<>("monthGL"));
        monthGL.setCellFactory(new StatCellFactory());
        monthGL.setSortable(false);
        monthGL.impl_setReorderable(false);

        TableColumn<UpperTableRow, String> yearGL = new TableColumn<>("Year G/L");
        yearGL.setCellValueFactory(new PropertyValueFactory<>("yearGL"));
        yearGL.setCellFactory(new StatCellFactory());
        yearGL.setSortable(false);
        yearGL.impl_setReorderable(false);

        upperStatsTable.getColumns().setAll(ticker, value, dayGL, monthGL, yearGL);
    }

    /**
     * Initializes the table used to display 52 week statistics.
     */
    private void initLowerTable() {
        lowerStatsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<LowerTableRow, String> ticker = new TableColumn<>();
        ticker.setCellValueFactory(new PropertyValueFactory<>("ticker"));
        ticker.setSortable(false);
        ticker.impl_setReorderable(false);

        TableColumn<LowerTableRow, String> high = new TableColumn<>("52W High");
        high.setCellValueFactory(new PropertyValueFactory<>("high"));
        high.setSortable(false);
        high.impl_setReorderable(false);

        TableColumn<LowerTableRow, String> low = new TableColumn<>("52W Low");
        low.setCellValueFactory(new PropertyValueFactory<>("low"));
        low.setSortable(false);
        low.impl_setReorderable(false);

        TableColumn<LowerTableRow, String> average = new TableColumn<>("52W Avg");
        average.setCellValueFactory(new PropertyValueFactory<>("average"));
        average.setSortable(false);
        average.impl_setReorderable(false);

        TableColumn<LowerTableRow, String> currSD = new TableColumn<>("Current SD");
        currSD.setCellValueFactory(new PropertyValueFactory<>("currSD"));
        currSD.setSortable(false);
        currSD.impl_setReorderable(false);

        lowerStatsTable.getColumns().setAll(ticker, high, low, average, currSD);
    }

    /**
     * Represents an individual row in the table which displays percentage gains and/or losses for the portfolio.
     */
    public class UpperTableRow {
        private String ticker;
        private double value;
        private double dayGL;
        private double monthGL;
        private double yearGL;
        private boolean isPortRow;

        /**
         * Initializes some class member variables, then calculates the statistics needed to populate this table row
         * for the portfolio as a whole.
         *
         * @param portRecord The portfolio record that contains the relevant historical data needed for the calculations.
         */
        UpperTableRow(PortfolioRecord portRecord) {
            ticker = "My Portfolio";
            value = portRecord.getCurrPortValue();
            isPortRow = true;

            calculateStats(portRecord.getHistory());
        }

        /**
         * Initializes some class member variables, then calculates the statistics needed to populate this table row
         * for an individual asset currently allocated in the portfolio.
         *
         * @param stockRecord The stock record that contains the relevant historical data needed for the calculations.
         */
        UpperTableRow(StockRecord stockRecord) {
            int historySize = stockRecord.getHistory().size();

            ticker = stockRecord.getTicker();
            value = stockRecord.getHistory().get(historySize - 1).getMarketCloseValue();
            isPortRow = false;

            calculateStats(stockRecord.getHistory());
        }

        // getter methods for class member variables with some conditional statements for formatting purposes

        public String getTicker() {
            return ticker;
        }

        public String getValue() {
            if (value == Double.MAX_VALUE) {
                return "N/A";
            } else {
                return Utils.formatDollars(value);
            }
        }

        public String getDayGL() {
            if (dayGL == Double.MAX_VALUE) {
                return "N/A";
            } else if (dayGL < 0) {
                return Utils.formatPercentage(dayGL);
            } else {
                return "+" + Utils.formatPercentage(dayGL);
            }
        }

        public String getMonthGL() {
            if (monthGL == Double.MAX_VALUE) {
                return "N/A";
            } else if (monthGL < 0) {
                return Utils.formatPercentage(monthGL);
            } else {
                return "+" + Utils.formatPercentage(monthGL);
            }
        }

        public String getYearGL() {
            if (yearGL == Double.MAX_VALUE) {
                return "N/A";
            } else if (yearGL < 0) {
                return Utils.formatPercentage(yearGL);
            } else {
                return "+" + Utils.formatPercentage(yearGL);
            }
        }

        /**
         * The function which does the actual calculations to output the statistics for the table row.
         *
         * @param history The historical data used to calculate the statistics.
         */
        private void calculateStats(List<DataPoint> history) {
            if (history.size() == 0) {
                value = Double.MAX_VALUE;
                dayGL = Double.MAX_VALUE;
                monthGL = Double.MAX_VALUE;
                yearGL = Double.MAX_VALUE;

                return;
            }

            DataPoint todayDataPoint = history.get(history.size() - 1);
            DataPoint currDataPoint = todayDataPoint;
            LocalDate todayDate = todayDataPoint.getDate();
            boolean calculatedMonthGL = false;
            boolean calculatedYearGL = false;

            dayGL = isPortRow ? getPortDataValue(todayDataPoint, currDataPoint) : getStockDataValue(todayDataPoint, currDataPoint);

            for (int i = history.size() - 2; i >= 0; i--) {
                currDataPoint = history.get(i);

                LocalDate currDate = currDataPoint.getDate();
                DataPoint refDataPoint = history.get(i + 1);

                if (!calculatedMonthGL && currDate.getMonthValue() != todayDate.getMonthValue()) {
                    monthGL = isPortRow ? getPortDataValue(todayDataPoint, refDataPoint) : getStockDataValue(todayDataPoint, refDataPoint);
                    calculatedMonthGL = true;
                } else if (currDate.getYear() != todayDate.getYear()) {
                    yearGL = isPortRow ? getPortDataValue(todayDataPoint, refDataPoint) : getStockDataValue(todayDataPoint, refDataPoint);
                    calculatedYearGL = true;

                    break;
                }
            }

            if (!calculatedMonthGL) {
                monthGL = isPortRow ? getPortDataValue(todayDataPoint, currDataPoint) : getStockDataValue(todayDataPoint, currDataPoint);
                yearGL = monthGL;
            }

            if (!calculatedYearGL) {
                yearGL = isPortRow ? getPortDataValue(todayDataPoint, currDataPoint) : getStockDataValue(todayDataPoint, currDataPoint);
            }
        }

        /**
         * Helper function to calculate percentage return values of the portfolio needed for further calculations.
         *
         * @param todayDataPoint The data point for today.
         * @param refDataPoint The data point which serves as a reference from which to calculate the percentage return for today.
         *
         * @return The percentage return of today from the reference point.
         */
        private double getPortDataValue(DataPoint todayDataPoint, DataPoint refDataPoint) {
            double todayReturn = (todayDataPoint.getMarketCloseValue() - todayDataPoint.getMoneyInvested()) / todayDataPoint.getMoneyInvested() * 100;
            double refReturn = (refDataPoint.getMarketOpenValue() - refDataPoint.getMoneyInvested()) / refDataPoint.getMoneyInvested() * 100;

            return todayReturn - refReturn;
        }

        /**
         * Helper function to calculate percentage return values of an individual asset needed for further calculations.
         *
         * @param todayDataPoint The data point for today.
         * @param refDataPoint The data point which serves as a reference from which to calculate the percentage return for today.
         *
         * @return The percentage return of today from the reference point.
         */
        private double getStockDataValue(DataPoint todayDataPoint, DataPoint refDataPoint) {
            return (todayDataPoint.getMarketCloseValue() - refDataPoint.getMarketOpenValue()) / refDataPoint.getMarketOpenValue() * 100;
        }
    }

    /**
     * Represents an individual row in the table which displays 52 week statistics.
     */
    public class LowerTableRow {
        private String ticker;
        private double high;
        private double low;
        private double average;
        private double currSD;

        /**
         * Initializes class member variables and calculates the statistics for the row.
         *
         * @param stockRecord The stock record which contains historical data.
         */
        LowerTableRow(StockRecord stockRecord) {
            this.ticker = stockRecord.getTicker();

            calculateStats(stockRecord.getHistory());
        }

        // getter methods for class member variables

        public String getTicker() {
            return ticker;
        }

        public String getHigh() {
            return high == 0 ? "-" : Utils.formatDollars(high);
        }

        public String getLow() {
            return low == 0 ? "-" : Utils.formatDollars(low);
        }

        public String getAverage() {
            return average == 0 ? "-" : Utils.formatDollars(average);
        }

        public String getCurrSD() {
            return currSD == 0 ? "-" : Utils.roundDecimal(currSD, 2);
        }

        /**
         * Calculates the 52 weeks statistics for this row.
         *
         * @param history The stock history used to calculate these statistics.
         */
        private void calculateStats(List<DataPoint> history) {
            if (history.size() > 260) {
                LocalDate stopDate = history.get(history.size() - 1).getDate().minusWeeks(52);
                double currHigh = Double.MIN_VALUE;
                double currLow = Double.MAX_VALUE;
                double sum = 0;
                int count = 0;

                for (int i = history.size() - 1; i >= 0; i--) {
                    StockDataPoint dataPoint = (StockDataPoint) history.get(i);

                    if (dataPoint.getDate().compareTo(stopDate) < 0) {
                        break;
                    } else {
                        currHigh = Math.max(currHigh, dataPoint.getMarketHighValue());
                        currLow = Math.min(currLow, dataPoint.getMarketLowValue());
                        sum += dataPoint.getMarketCloseValue();
                        count += 1;
                    }
                }

                high = currHigh;
                low = currLow;
                average = sum / count;
                currSD = calculateCurrSD(average, history);
            }
        }

        /**
         * Calculates the standard deviation away from the 52 week average for the current price of the asset
         * associated with this row.
         *
         * @param average The 52 week average price.
         * @param history The history of the asset.
         * @return The current standard deviation.
         */
        private double calculateCurrSD(double average, List<DataPoint> history) {
            DataPoint currDataPoint = history.get(history.size() - 1);
            LocalDate stopDate = history.get(history.size() - 1).getDate().minusWeeks(52);
            double meanDiffSum = 0;
            int count = 0;

            for (int i = history.size() - 1; i >= 0; i--) {
                DataPoint dataPoint = history.get(i);

                if (dataPoint.getDate().compareTo(stopDate) < 0) {
                    break;
                } else {
                    meanDiffSum += Math.pow(dataPoint.getMarketCloseValue() - average, 2);
                    count += 1;
                }
            }

            return (currDataPoint.getMarketCloseValue() - average) / Math.sqrt(meanDiffSum / count);
        }
    }

    /**
     * Sorts the table rows for the upper table. The portfolio row is always first, and the rest of the rows are sorted
     * alphabetically based on asset tickers.
     */
    private class UpperTableRowComparator implements Comparator<UpperTableRow> {

        @Override
        public int compare(UpperTableRow o1, UpperTableRow o2) {
            if (o1.getTicker().equals("My Portfolio")) {
                return -1;
            } else if (o2.getTicker().equals("My Portfolio")) {
                return 1;
            } else {
                return o1.getTicker().compareTo(o2.getTicker());
            }
        }
    }

    /**
     * Sorts the table rows for the lower table. Rows are sorted alphabetically based on asset tickers.
     */
    private class LowerTableRowComparator implements Comparator<LowerTableRow> {

        @Override
        public int compare(LowerTableRow o1, LowerTableRow o2) {
            return o1.getTicker().compareTo(o2.getTicker());
        }
    }

    /**
     * Custom cell factory used to style individual cells in the upper table.
     */
    private class StatCellFactory implements Callback<TableColumn<UpperTableRow, String>, TableCell<UpperTableRow, String>> {

        @Override
        public TableCell<UpperTableRow, String> call(TableColumn<UpperTableRow, String> param) {
            return new StatCell();
        }
    }

    /**
     * Custom cell for the upper table used to individually style the cells.
     */
    private class StatCell extends TableCell<UpperTableRow, String> {

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (item != null) {
                double val = item.charAt(0) == '+' ? Double.parseDouble(item.substring(1, item.length() - 1))
                        : Double.parseDouble(item.substring(0, item.length() - 1));

                if (val >= 0) {
                    if (Utils.roundDecimal(val, 2).equals("0.00")) {
                        setStyle("-fx-text-fill: white;");
                    } else {
                        setStyle("-fx-text-fill: #00e600;");
                    }
                } else {
                    setStyle("-fx-text-fill: #ff4c00;");
                }

                setText(item);
            } else {
                setText(null);
            }
        }
    }
}
