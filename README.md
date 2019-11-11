# PortfolioAnalyzer

## Overview
This application allows a user to easily log investment activity and track their portfolio performance across time. The dashboard is divided into four quadrants: Performance, Allocations, Transactions, and Statistics. The Performance quadrant displays a line graph with multiple modes to show a time series of either percent return or gross profits across time for the portfolio as a whole. It is also possible to specify a specific time interval in which to show data on the graph. The Allocations quadrant displays two tables and a pie chart. These widgets provide the user with a detailed overview of how their portfolio is allocated. The dollar amount invested in each asset, number of shares owned, and percentage allocated of each asset are all available to the user. The pie chart reflects the breakdown of the dollar amount invested in (or percentage allocated of) each asset in the portfolio. The Transactions quadrant is used to log "Buy", "Sell", or "Dividend" transactions (where the "Dividend" transaction allows the user to log a reinvestment of dividends into a specific asset). There is also a table provided which displays the history of all previously made transactions. The Statistics quadrant shows two tables containing various statistics for both the portfolio as a whole as well as for the individual assets comprising the portfolio. Statistics such as percentage day gain/loss, percentage month gain/loss, 52 week high, 52 week low, and more are made available to the user in these tables.

## Setup and Run
1.	This is currently a private repository, so please contact the owner for an access token in order to be able to clone the repository. 
2.	You will also need to setup an account with `rapidapi.com`. Once you have an account created, you will be provided with a `Rapid Api Token`. Paste this token in line `25` of `PortfolioAnalyzer/src/main/java/data/stockdata/StockDataFetcher.java` where indicated. 
3.	Using a shell terminal, `cd` into the root directory of the project. Run the following command to compile the project: `mvn compile`. NOTE: You will need to have `maven` installed on your machine for this and the following commands to work.
4.	Run the application with the command: `mvn exec:java -Dexec.mainClass=start.Main`.

## Development Changes
In the event that the repository owner makes any updates to the source code:

1.	Pull in the changes using `git pull`.
2.	Clean the project with `mvn clean`.
3.	Recompile the project with `mvn compile`.
4.	Run the project with `mvn exec:java -Dexec.mainClass=start.Main`.

NOTE: In the event that the repository owner makes changes to any of the following files, please delete the `PortfolioAnalyzer/records/` directory. After completing the above steps, you will have to re-log every transaction previously made with the application. Due to the level of effort required with this procedure, changes to these files will be avoided at all costs. However, if such a change is absolutely necessary, this is the current procedure for merging in such changes.
* `PortfolioAnalyzer/src/main/java/data/records/Record.java`
* `PortfolioAnalyzer/src/main/java/data/records/EOFRecord.java`
* `PortfolioAnalyzer/src/main/java/data/records/PortfolioRecord.java`
* `PortfolioAnalyzer/src/main/java/data/records/StockRecord.java`
* `PortfolioAnalyzer/src/main/java/data/records/TransactionRecord.java`
* `PortfolioAnalyzer/src/main/java/data/records/DividendRecord.java`
* `PortfolioAnalyzer/src/main/java/data/datapoints/DataPoint.java`
* `PortfolioAnalyzer/src/main/java/data/datapoints/PortfolioDataPoint.java`
* `PortfolioAnalyzer/src/main/java/data/datapoints/StockDataPoint.java`

