package viewmanagers;

import controllers.PortfolioOverviewController;
import data.records.PortfolioRecord;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import utils.Utils;

import java.util.Comparator;
import java.util.Map;

/**
 * Created by scottreese on 6/16/19.
 *
 * The manager responsible for controlling and updating the pie chart chart and table component of the main screen.
 */
public class AllocationsManager {
    private PortfolioOverviewController overviewController;
    private PieChart allocationChart;
    private TableView<UpperTableRow> upperAllocationsTable;
    private TableView<LowerTableRow> lowerAllocationsTable;

    /**
     * Initializes class member variables.
     *
     * @param overviewController The controller of the main screen.
     * @param allocationChart The pie chart used to show visually how the portfolio is allocated.
     * @param upperAllocationsTable The table which displays the dollar amount invested in each asset of the portfolio.
     * @param lowerAllocationsTable The table which displays the number of shares owned and percentage allocated of each asset.
     */
    public AllocationsManager(PortfolioOverviewController overviewController, PieChart allocationChart,
                              TableView<UpperTableRow> upperAllocationsTable, TableView<LowerTableRow> lowerAllocationsTable) {
        this.overviewController = overviewController;
        this.allocationChart = allocationChart;
        this.upperAllocationsTable = upperAllocationsTable;
        this.lowerAllocationsTable = lowerAllocationsTable;

        initUpperTable();
        initLowerTable();
    }

    /**
     * Initializes the pie chart and tables with the current portfolio allocations.
     *
     * @param portRecord The portfolio record containing the relevant allocation data.
     */
    public void initialize(PortfolioRecord portRecord) {
        updateAllocationChartAndTable(false, portRecord);
    }

    /**
     * Updates the pie chart and tables when any of the portfolio allocations are altered.
     *
     * @param portRecord The portfolio record containing the updated allocation data.
     */
    public void update(PortfolioRecord portRecord) {
        allocationChart.getData().clear();

        updateAllocationChartAndTable(true, portRecord);
    }

    /**
     * Initializes the table which displays the dollar amount invested in each asset of the portfolio.
     */
    private void initUpperTable() {
        upperAllocationsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<UpperTableRow, String> ticker = new TableColumn<>("Ticker");
        ticker.setCellValueFactory(new PropertyValueFactory<>("ticker"));
        ticker.setSortable(false);
        ticker.impl_setReorderable(false);

        TableColumn<UpperTableRow, String> amountInvested = new TableColumn<>("$ Invested");
        amountInvested.setCellValueFactory(new PropertyValueFactory<>("amountInvested"));
        amountInvested.setSortable(false);
        amountInvested.impl_setReorderable(false);

        upperAllocationsTable.getColumns().setAll(ticker, amountInvested);
    }

    /**
     * Initializes the table which displays the number of shares owned and percentage allocated of each asset.
     */
    private void initLowerTable() {
        lowerAllocationsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<LowerTableRow, String> ticker = new TableColumn<>("Ticker");
        ticker.setCellValueFactory(new PropertyValueFactory<>("ticker"));
        ticker.setSortable(false);
        ticker.impl_setReorderable(false);

        TableColumn<LowerTableRow, String> numShares = new TableColumn<>("Shares");
        numShares.setCellValueFactory(new PropertyValueFactory<>("numShares"));
        numShares.setSortable(false);
        numShares.impl_setReorderable(false);

        TableColumn<LowerTableRow, String> percentage = new TableColumn<>("%");
        percentage.setCellValueFactory(new PropertyValueFactory<>("percentage"));
        percentage.setSortable(false);
        percentage.impl_setReorderable(false);

        lowerAllocationsTable.getColumns().setAll(ticker, numShares, percentage);
    }

    /**
     * The helper function which does the actual updating of the pie chart and tables. Signifies to the main screen
     * controller when finished.
     *
     * @param isUpdate True if this allocation chart manager is completing an update; false if it is initializing.
     * @param portRecord A map containing the actual allocations of the portfolio.
     */
    private void updateAllocationChartAndTable(boolean isUpdate, PortfolioRecord portRecord) {
        Map<String, PortfolioRecord.Allocation> allocations = portRecord.getAllocations();
        ObservableList<UpperTableRow> upperTableRows = FXCollections.observableArrayList();
        ObservableList<LowerTableRow> lowerTableRows = FXCollections.observableArrayList();

        for (String ticker : allocations.keySet()) {
            PortfolioRecord.Allocation allocation = allocations.get(ticker);

            if (allocation.getNumShares() > 0) {
                PieChart.Data slice = new PieChart.Data(ticker, allocation.getMoneyAmount());
                double percentage = allocation.getMoneyAmount() / portRecord.getCurrMoneyInvested() * 100;

                allocationChart.getData().add(slice);
                upperTableRows.add(new UpperTableRow(ticker, allocation.getMoneyAmount()));
                lowerTableRows.add(new LowerTableRow(ticker, allocation.getNumShares(), percentage));
            }
        }

        upperTableRows.sort(new UpperTableRowComparator());
        lowerTableRows.sort(new LowerTableRowComparator());

        upperAllocationsTable.setItems(upperTableRows);
        lowerAllocationsTable.setItems(lowerTableRows);
        overviewController.allocationsManagerFinished(isUpdate);
    }

    /**
     * Represents a row in the table which displays the dollar amount invested in each asset of the portfolio.
     */
    public class UpperTableRow {
        private String ticker;
        private double amountInvested;

        /**
         * Initializes class member variables.
         *
         * @param ticker The ticker symbol of the asset associated with this row.
         * @param amountInvested The current dollar amount of money invested in the asset.
         */
        UpperTableRow(String ticker, double amountInvested) {
            this.ticker = ticker;
            this.amountInvested = amountInvested;
        }

        // getter methods for class member variables

        public String getTicker() {
            return ticker;
        }

        public String getAmountInvested() {
            return Utils.formatDollars(amountInvested);
        }

        double getAmountInvestedDouble() {
            return amountInvested;
        }
    }

    /**
     * Represents a row in the table which displays the number of shares owned and percentage allocated of each asset.
     */
    public class LowerTableRow {
        private String ticker;
        private double numShares;
        private double percentage;

        /**
         * Initializes class member variables.
         *
         * @param ticker The ticker symbol of the asset associated with this row.
         * @param numShares The number of shares currently owned of this asset.
         * @param percentage The percentage of portfolio assets allocated for this asset.
         */
        LowerTableRow(String ticker, double numShares, double percentage) {
            this.ticker = ticker;
            this.numShares = numShares;
            this.percentage = percentage;
        }

        // getter methods for class member variables

        public String getTicker() {
            return ticker;
        }

        public String getNumShares() {
            return Utils.roundDecimal(numShares, 4);
        }

        public String getPercentage() {
            return Utils.formatPercentage(percentage);
        }

        double getPercentageDouble() {
            return percentage;
        }
    }

    /**
     * Sorts rows for the table which displays the dollar amount invested in each asset of the portfolio. Rows are
     * sorted based on dollar amount invested, from greatest to smallest.
     */
    private class UpperTableRowComparator implements Comparator<UpperTableRow> {

        @Override
        public int compare(UpperTableRow o1, UpperTableRow o2) {
            if (o1.getAmountInvestedDouble() < o2.getAmountInvestedDouble()) {
                return 1;
            } else if (o1.getAmountInvestedDouble() > o2.getAmountInvestedDouble()) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    /**
     * Sorts rows for the table which displays the number of shares owned and percentage allocated of each asset.
     * Rows are sorted based on percentage allocated, from greatest to smallest.
     */
    private class LowerTableRowComparator implements Comparator<LowerTableRow> {

        @Override
        public int compare(LowerTableRow o1, LowerTableRow o2) {
            if (o1.getPercentageDouble() < o2.getPercentageDouble()) {
                return 1;
            } else if (o1.getPercentageDouble() > o2.getPercentageDouble()) {
                return -1;
            } else {
                return 0;
            }
        }
    }
}
