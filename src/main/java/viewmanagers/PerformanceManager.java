package viewmanagers;

import controllers.PortfolioOverviewController;
import data.datapoints.DataPoint;
import data.datapoints.PortfolioDataPoint;
import data.records.PortfolioRecord;
import data.records.StockRecord;
import data.stockdata.HistoricalStockData;
import data.stockdata.StockData;
import data.stockdata.StockDataCallback;
import data.stockdata.StockDataFetcher;
import error.PAException;
import javafx.application.Platform;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Created by scottreese on 6/16/19.
 *
 * The manager responsible for controlling and updating the performance graph component of the main screen.
 */
public class PerformanceManager {
    private static final int MAX_DATA_POINTS = 51;

    private PortfolioOverviewController overviewController;
    private LineChart<String, Double> performanceGraph;
    private Mode currMode;
    private LocalDate fromDateBound;
    private LocalDate toDateBound;
    private boolean userSetDates;

    // represents the "mode" the overview graph is currently set in
    public enum Mode {
        GROSS_PROFITS,
        PERCENT_RETURN
    }

    /**
     * Initializes class member variables and does some visual formatting for the graph.
     *
     * @param overviewController The controller of the main screen.
     * @param performanceGraph The performance graph used to display time series of profits and returns of the portfolio.
     */
    public PerformanceManager(PortfolioOverviewController overviewController, LineChart<String, Double> performanceGraph) {
        this.overviewController = overviewController;
        this.performanceGraph = performanceGraph;
        this.currMode = Mode.GROSS_PROFITS;
        this.fromDateBound = null;
        this.toDateBound = null;
        this.userSetDates = false;

        Axis<String> xAxis = performanceGraph.getXAxis();
        Axis<Double> yAxis = performanceGraph.getYAxis();

        xAxis.setAnimated(false);
        xAxis.setTickLabelGap(5);

        yAxis.setAnimated(false);
        yAxis.setTickLabelGap(5);

        formatYAxis();
    }

    /**
     * Updates historical portfolio data if it is stale and then initializes the performance graph to reflect this data.
     *
     * @param portRecord The portfolio record which contains the relevant historical data.
     * @param stockDataRecords A map containing all records of assets currently allocated in the portfolio.
     * @param updateUI True if the caller wants the performance graph to update visually after initialization; false otherwise.
     */
    public void initialize(PortfolioRecord portRecord, Map<String, StockRecord> stockDataRecords, boolean isRefresh, boolean updateUI) {
        if (portRecord.updateNeeded() || isRefresh) {
            fetchNewStockData(portRecord, stockDataRecords, updateUI);
        } else {
            checkDateBounds(portRecord);

            if (updateUI) {
                updatePerformanceGraph(false, portRecord);
            } else {
                overviewController.performanceManagerFinished(false, fromDateBound, toDateBound);
            }
        }

        performanceGraph.requestFocus();
    }

    /**
     * Updates the performance graph to reflect any updates to the historical portfolio data. This occurs after a
     * transaction has been made.
     *
     * @param portRecord The portfolio record containing the updated historical data.
     */
    public void update(PortfolioRecord portRecord) {
        checkDateBounds(portRecord);
        updatePerformanceGraph(true, portRecord);
    }

    /**
     * Changes the current mode of the overview graph, either to display gross profits or percent return.
     *
     * @param portRecord The portfolio record containing historical portfolio data.
     * @param mode The mode to set the performance graph in.
     */
    public void setMode(PortfolioRecord portRecord, Mode mode) {
        if (mode != currMode) {
            currMode = mode;

            if (portRecord.getHistory().size() > 0) {
                updatePerformanceGraph(true, portRecord);
            } else {
                formatYAxis();
            }
        }
    }

    /**
     * Sets the date range between which to show data on the overview graph.
     *
     * @param portRecord The portfolio record that contains historical portfolio data.
     * @param fromDate The date starting from which to show data on the performance graph.
     * @param toDate The date ending at which to show data on the performance graph.
     */
    public void setDateBounds(PortfolioRecord portRecord, LocalDate fromDate, LocalDate toDate) {
        List<DataPoint> history = portRecord.getHistory();
        LocalDate minDate = history.get(0).getDate();
        LocalDate maxDate = history.get(history.size() - 1).getDate();

        if (fromDate != null && toDate != null && !(fromDate.equals(fromDateBound) && toDate.equals(toDateBound))) {
            userSetDates = true;

            if (fromDate.compareTo(minDate) < 0) {
                fromDateBound = minDate;
            } else {
                fromDateBound = fromDate;
            }

            if (toDate.compareTo(maxDate) > 0) {
                toDateBound = maxDate;
            } else {
                toDateBound = toDate;
            }

            updatePerformanceGraph(true, portRecord);
        }
    }

    /**
     * Initializes the date bounds for the performance graph or sets them to appropriate values depending on the
     * portfolio state.
     *
     * @param portRecord The portfolio record which contains historical portfolio data.
     */
    private void checkDateBounds(PortfolioRecord portRecord) {
        List<DataPoint> history = portRecord.getHistory();

        if (!userSetDates) {
            fromDateBound = null;
            toDateBound = null;
        }

        if (history.size() > 0) {
            LocalDate minDate = history.get(0).getDate();
            LocalDate maxDate = history.get(history.size() - 1).getDate();

            if (fromDateBound == null && toDateBound == null) {
                fromDateBound = minDate;
                toDateBound = maxDate;
            } else if (toDateBound.compareTo(maxDate) < 0) {
                toDateBound = maxDate;
            }
        } else {
            fromDateBound = null;
            toDateBound = null;
            userSetDates = false;
        }
    }

    /**
     * Fetches the appropriate amount of historical data for each asset in the portfolio to update the portfolio's
     * historical data.
     *
     * @param portRecord The portfolio record whose historical data needs to be updated.
     * @param stockDataRecords A map containing all records of assets in the portfolio whose historical data needs to be updated.
     */
    private void fetchNewStockData(PortfolioRecord portRecord, Map<String, StockRecord> stockDataRecords, boolean updateUI) {
        Date lastUpdate = portRecord.getLastUpdate();
        Date currDate = new Date();
        StockDataFetcher stockDataFetcher = new StockDataFetcher();
        HistoricalStockDataCallback callback = new HistoricalStockDataCallback(portRecord, stockDataRecords, updateUI);
        List<String> tickers = new ArrayList<>(stockDataRecords.keySet());

        stockDataFetcher.fetchHistoricalStockData(tickers, lastUpdate, currDate, callback);
    }

    /**
     * Updates the portfolio's historical data from the updated historical data of each of its assets.
     *
     * @param portRecord The portfolio record whose historical data needs to be updated.
     * @param stockDataUpdates A map containing all records of assets in the portfolio whose historical data has been updated.
     */
    private void updatePortfolioHistory(PortfolioRecord portRecord, List<StockData> stockDataUpdates) {
        List<DataPoint> newHistory = new ArrayList<>();
        Map<String, PortfolioRecord.Allocation> portAllocations = portRecord.getAllocations();
        int numDataPoints = ((HistoricalStockData) stockDataUpdates.get(0)).getHistory().size();
        double currMoneyInvested = portRecord.getCurrMoneyInvested();

        for (int i = 0; i < numDataPoints; i++) {
            LocalDate date = null;
            double openPortValue = 0;
            double closePortValue = 0;

            for (StockData stockData : stockDataUpdates) {
                HistoricalStockData historicalStockData = (HistoricalStockData) stockData;
                String ticker = historicalStockData.getTicker();
                DataPoint dataPoint = historicalStockData.getHistory().get(i);
                date = dataPoint.getDate();

                openPortValue += dataPoint.getMarketOpenValue() * portAllocations.get(ticker).getNumShares();
                closePortValue += dataPoint.getMarketCloseValue() * portAllocations.get(ticker).getNumShares();
            }

            newHistory.add(new PortfolioDataPoint(date, openPortValue, closePortValue, currMoneyInvested));
        }

        portRecord.addHistory(newHistory);
    }

    /**
     * Updates the performance graph to reflect the updated historical portfolio data.
     *
     * @param isUpdate True if this overview graph manager is completing an update, false if it is initializing.
     * @param portRecord The portfolio record which contains the updated historical data.
     */
    private void updatePerformanceGraph(boolean isUpdate, PortfolioRecord portRecord) {
        XYChart.Series<String, Double> series = new XYChart.Series<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yy");
        List<DataPoint> history = portRecord.getHistory();
        boolean lowerBoundFound = false;

        for (int i = 0; i < history.size(); i++) {
            DataPoint dataPoint = history.get(i);
            LocalDate date = dataPoint.getDate();
            double moneyMade = dataPoint.getMarketCloseValue() - dataPoint.getMoneyInvested();
            double percentReturn = (dataPoint.getMarketCloseValue() - dataPoint.getMoneyInvested()) / dataPoint.getMoneyInvested() * 100;

            if (date.compareTo(toDateBound) > 0) {
                toDateBound = history.get(i - 1).getDate();

                break;
            } else if (date.equals(toDateBound)) {
                series.getData().add(new XYChart.Data<>(date.format(formatter), currMode == Mode.GROSS_PROFITS ? moneyMade : percentReturn));

                break;
            } else if (date.compareTo(fromDateBound) > 0 && !lowerBoundFound) {
                series.getData().add(new XYChart.Data<>(date.format(formatter), currMode == Mode.GROSS_PROFITS ? moneyMade : percentReturn));
                lowerBoundFound = true;
                fromDateBound = date;

            } else if (date.equals(fromDateBound)) {
                series.getData().add(new XYChart.Data<>(date.format(formatter), currMode == Mode.GROSS_PROFITS ? moneyMade : percentReturn));
                lowerBoundFound = true;
            } else if (date.compareTo(fromDateBound) > 0 && date.compareTo(toDateBound) < 0) {
                series.getData().add(new XYChart.Data<>(date.format(formatter), currMode == Mode.GROSS_PROFITS ? moneyMade : percentReturn));
            }
        }

        if (series.getData().size() > MAX_DATA_POINTS) {
            series = resizeDataSeries(series);
        }

        formatYAxis();

        performanceGraph.getData().clear();
        performanceGraph.getData().add(series);
        overviewController.performanceManagerFinished(isUpdate, fromDateBound, toDateBound);
    }

    /**
     * Resizes (shortens) the data series displayed in the overview graph to have a maximum length (currently set to 51).
     *
     * @param series The data series to be resized.
     * @return The resized data series.
     */
    private XYChart.Series<String, Double> resizeDataSeries(XYChart.Series<String, Double> series) {
        int adjustedNumPoints = MAX_DATA_POINTS - 2;
        int adjustedLength = series.getData().size() - 2;
        double divisor = adjustedLength - adjustedNumPoints + 1;
        double removeEvery = adjustedLength / divisor;
        double indexSum = removeEvery;
        int[] skippedIndices = new int[adjustedLength - adjustedNumPoints];
        int skippedIndex = (int) Math.ceil(indexSum);
        int i = 0;

        XYChart.Series<String, Double> resizedSeries = new XYChart.Series<>();
        XYChart.Data<String, Double> first = series.getData().get(0);
        XYChart.Data<String, Double> last = series.getData().get(series.getData().size() - 1);

        series.getData().remove(0);
        series.getData().remove(series.getData().size() - 1);
        resizedSeries.getData().add(first);

        while (skippedIndex < adjustedLength) {
            skippedIndices[i] = skippedIndex;
            indexSum += removeEvery;
            skippedIndex = (int) Math.ceil(indexSum);
            i += 1;
        }

        i = 0;
        skippedIndex = skippedIndices[i];

        for (int j = 0; j < series.getData().size(); j++) {
            if (j != skippedIndex) {
                resizedSeries.getData().add(series.getData().get(j));
            } else if (i < skippedIndices.length - 1) {
                i += 1;
                skippedIndex = skippedIndices[i];
            }
        }

        resizedSeries.getData().add(last);

        return resizedSeries;
    }

    /**
     * Formats the Y-axis of the performance graph to reflect the current mode, either gross profits or percent return.
     */
    private void formatYAxis() {
        ((ValueAxis<Double>) performanceGraph.getYAxis()).setTickLabelFormatter(new StringConverter<Double>() {
            @Override
            public String toString(Double object) {
                if (currMode == Mode.GROSS_PROFITS) {
                    if (object < 0) {
                        return "-$" + Math.abs(object);
                    } else {
                        return "$" + object;
                    }
                } else if (currMode == Mode.PERCENT_RETURN) {
                    return object + "%";
                } else {
                    return "err";
                }
            }

            @Override
            public Double fromString(String string) {
                return null;
            }
        });
    }

    /**
     * The callback used to receive historical asset data updates when the historical portfolio data needs to be updated.
     */
    private class HistoricalStockDataCallback implements StockDataCallback {
        private PortfolioRecord portRecord;
        private Map<String, StockRecord> stockDataRecords;
        private boolean updateUI;

        /**
         * Initializes class member variables.
         *
         * @param portRecord The portfolio record whose historical data needs to be updated.
         * @param stockDataRecords A map containing all records of assets in the portfolio whose historical data needs to be updated.
         */
        HistoricalStockDataCallback(PortfolioRecord portRecord, Map<String, StockRecord> stockDataRecords, boolean updateUI) {
            this.portRecord = portRecord;
            this.stockDataRecords = stockDataRecords;
            this.updateUI = updateUI;
        }

        @Override
        public void failed(PAException ex) {
            if (ex.getType() == PAException.Type.WEEKEND_NO_DATA) {
                checkDateBounds(portRecord);
                portRecord.setUpdated();

                if (updateUI) {
                    Platform.runLater(() -> updatePerformanceGraph(false, portRecord));
                } else {
                    Platform.runLater(() -> overviewController.performanceManagerFinished(false, fromDateBound, toDateBound));
                }
            } else {
                if (ex.getType() == PAException.Type.MISC) {
                    ex.getMiscException().printStackTrace();
                } else {
                    ex.printStackTrace();
                }

                System.exit(-1);
            }
        }

        @Override
        public void completed(List<StockData> data) {
            for (StockData stockData : data) {
                String ticker = stockData.getTicker();
                HistoricalStockData historicalStockData = (HistoricalStockData) stockData;

                stockDataRecords.get(ticker).addHistory(historicalStockData.getHistory());
            }

            updatePortfolioHistory(portRecord, data);
            checkDateBounds(portRecord);

            if (updateUI) {
                Platform.runLater(() -> updatePerformanceGraph(false, portRecord));
            } else {
                Platform.runLater(() -> overviewController.performanceManagerFinished(false, fromDateBound, toDateBound));
            }
        }
    }
}
