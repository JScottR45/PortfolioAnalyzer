package data.records;

import java.util.List;

/**
 * Created by scottreese on 6/6/19.
 *
 * Interface to be implemented to create callback instances for reading/writing records from/to disk. The records stored
 * in a particular file are handed off to the callback once they are all read/written from/to disk.
 */
public interface RecordCallback {

    /**
     * Called when reading/writing one or more records from/to disk fails.
     *
     * @param ex The specific exception that occurred when reading/writing one or more records from/to disk.
     */
    void failed(Exception ex);

    /**
     * Called when reading/writing one or more records from/to disk is successful.
     *
     * @param records A list of all the records which were read/written from/to a particular file on disk.
     */
    void completed(List<Record> records);
}
