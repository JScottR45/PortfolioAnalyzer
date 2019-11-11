package data;

import data.records.*;

import java.io.*;
import java.util.*;

/**
 * Created by scottreese on 6/5/19.
 *
 * Manages all disk content. In charge of reading and writing records to and from disk.
 */

public class DiskDataManager {
    private final String RECORDS_DIRECTORY = "records/";
    private final String PORTFOLIO_DATA_FILE_PATH = "records/portfolio_data.txt";
    private final String STOCK_DATA_FILE_PATH = "records/stock_data.txt";
    private final String TRANSACTION_DATA_FILE_PATH = "records/transaction_data.txt";

    /**
     * Initializes the file system if this is the first time the user has run the application.
     */
    public DiskDataManager() {
        File portfolioDataFile = new File(PORTFOLIO_DATA_FILE_PATH);

        if (!portfolioDataFile.exists() && !initializeFileSystem()) {
            System.out.println("Failed to initialize file system");
            System.exit(-1);
        }
    }

    /**
     * Reads the portfolio record from disk.
     *
     * @param callback The callback used to hand back the portfolio record once it is read from disk.
     */
    public void readPortfolioDataRecord(RecordCallback callback) {
        readRecords(PORTFOLIO_DATA_FILE_PATH, callback);
    }

    /**
     * Writes the portfolio record to disk.
     *
     * @param record The portfolio record to be written to disk.
     * @param callback The callback used to signify when the record is finished being written to disk.
     */
    public void writePortfolioDataRecord(PortfolioRecord record, RecordCallback callback) {
        List<Record> recordToWrite = new ArrayList<>();

        recordToWrite.add(record);
        writeRecords(recordToWrite, PORTFOLIO_DATA_FILE_PATH, callback);
    }

    /**
     * Reads all stock records from disk.
     *
     * @param callback The callback used to hand back the stock records once they are all read from disk.
     */
    public void readStockDataRecords(RecordCallback callback) {
        readRecords(STOCK_DATA_FILE_PATH, callback);
    }

    /**
     * Writes stock records to disk.
     *
     * @param stockRecords A list of stock records to be written to disk.
     * @param callback The callback used to signify when all stock records have been written to disk.
     */
    public void writeStockDataRecords(List<StockRecord> stockRecords, RecordCallback callback) {
        List<Record> recordsToWrite = new ArrayList<>(stockRecords);

        writeRecords(recordsToWrite, STOCK_DATA_FILE_PATH, callback);
    }

    /**
     * Reads all transaction records from disk.
     *
     * @param callback The callback used to hand back the transaction records once they are all read from disk.
     */
    public void readTransactionRecords(RecordCallback callback) {
        readRecords(TRANSACTION_DATA_FILE_PATH, callback);
    }

    /**
     * Writes transactions records to disk.
     *
     * @param transactionRecords A list of transaction records to be written to disk.
     * @param callback The callback used to signify when all transaction records have been written to disk.
     */
    public void writeTransactionRecords(List<TransactionRecord> transactionRecords, RecordCallback callback) {
        List<Record> recordsToWrite = new ArrayList<>(transactionRecords);

        writeRecords(recordsToWrite, TRANSACTION_DATA_FILE_PATH, callback);
    }

    /**
     * The function which does the actual reading of records from disk.
     *
     * @param filepath The filepath of the file containing the records to be read from disk.
     * @param callback The callback used to hand back the records once they are all read from disk.
     */
    private void readRecords(String filepath, RecordCallback callback) {
        Thread thread = new Thread(() -> {
            try {
                ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(filepath));
                List<Record> records = new ArrayList<>();
                Record record = (Record) inputStream.readObject();

                while (record.getType() != RecordType.EOF_RECORD) {
                    records.add(record);
                    record = (Record) inputStream.readObject();
                }

                inputStream.close();
                callback.completed(records);
            } catch (IOException ex) {
                callback.failed(ex);
            } catch (ClassNotFoundException ex) {
                callback.failed(ex);
            }
        });

        thread.start();
    }

    /**
     * The function which does the actual writing of records to disk.
     *
     * @param records A list of records to be written to disk.
     * @param filepath The filepath of the file where the records are to be written.
     * @param callback The callback used to signify when all records have been written to disk.
     */
    private void writeRecords(List<Record> records, String filepath, RecordCallback callback) {
        Thread thread = new Thread(() -> {
            try {
                ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(filepath));

                for (Record record : records) {
                    outputStream.writeObject(record);
                }

                outputStream.writeObject(new EOFRecord());
                outputStream.close();
                callback.completed(records);
            } catch (IOException ex) {
                callback.failed(ex);
            }
        });

        thread.start();
    }

    /**
     * Initializes the file system. Sets up the three files used to contain all relevant records.
     *
     * @return True if the initialization is successful; false otherwise.
     */
    private boolean initializeFileSystem() {
        PortfolioRecord portfolioRecord = new PortfolioRecord();
        File recordsDir = new File(RECORDS_DIRECTORY);
        File portfolioDataFile = new File(PORTFOLIO_DATA_FILE_PATH);
        File stockDataFile = new File(STOCK_DATA_FILE_PATH);
        File transactionDataFile = new File(TRANSACTION_DATA_FILE_PATH);
        ObjectOutputStream outputStream;
        boolean success;

        try {
            success = recordsDir.mkdir();

            if (!success) {
                return false;
            }

            success = portfolioDataFile.createNewFile();

            if (!success) {
                return false;
            }

            success = stockDataFile.createNewFile();

            if (!success) {
                recordsDir.delete();
                portfolioDataFile.delete();
                return false;
            }

            success = transactionDataFile.createNewFile();

            if (!success) {
                recordsDir.delete();
                portfolioDataFile.delete();
                stockDataFile.delete();
                return false;
            }

            outputStream = new ObjectOutputStream(new FileOutputStream(PORTFOLIO_DATA_FILE_PATH));
            outputStream.writeObject(portfolioRecord);
            outputStream.writeObject(new EOFRecord());
            outputStream.close();

            outputStream = new ObjectOutputStream(new FileOutputStream(STOCK_DATA_FILE_PATH));
            outputStream.writeObject(new EOFRecord());
            outputStream.close();

            outputStream = new ObjectOutputStream(new FileOutputStream(TRANSACTION_DATA_FILE_PATH));
            outputStream.writeObject(new EOFRecord());
            outputStream.close();

        } catch (IOException ex) {
            return false;
        }

        return true;
    }
}
