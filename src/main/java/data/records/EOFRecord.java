package data.records;

/**
 * Created by scottreese on 6/28/19.
 *
 * A record type that is used simply at the end of a file comprised of other record types. An EOF record marks the
 * end of the file.
 */
public class EOFRecord implements Record {

    @Override
    public RecordType getType() {
        return RecordType.EOF_RECORD;
    }
}
