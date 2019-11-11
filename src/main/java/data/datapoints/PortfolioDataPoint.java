package data.datapoints;

import java.time.LocalDate;

/**
 * Created by scottreese on 7/9/19.
 *
 * Represents a data point for a specific date for the portfolio as a whole. See DataPoint.java for more detailed
 * documentation.
 */
public class PortfolioDataPoint extends DataPoint {

    public PortfolioDataPoint(LocalDate date, double marketOpenValue, double marketCloseValue, double moneyInvested) {
        super(date, marketOpenValue, marketCloseValue, moneyInvested);
    }
}
