package data.records;

import java.io.Serializable;

/**
 * Created by scottreese on 6/7/19.
 *
 * Interface representing a record which is meant to package a specific set of data (such as for a transaction, a
 * specific asset, or the portfolio as a whole) that can be written to disk.
 */
public interface Record extends Serializable {

    // getter method that returns the type of record which implements this interface

    RecordType getType();
}
