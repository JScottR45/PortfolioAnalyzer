package error;

/**
 * Created by scottreese on 10/14/19.
 *
 * An exception wrapper class. Some exceptions this class can represent simply need a specific response. Others
 * require that the application terminates.
 */
public class PAException extends Exception {
    private Type type;
    private String errorMessage;
    private Exception miscException;

    // Used to specify the type of exceptions that this class can represent.
    public enum Type {
        NOT_FOUND,
        WEEKEND_NO_DATA,
        INVALID_TRANSACTION_DATE,
        REQUEST_CANCELLED,
        MISC
    }

    /**
     * Initializes class member variables.
     *
     * @param type The type of exception this will represent.
     * @param errorMessage The error message.
     */
    public PAException(Type type, String errorMessage) {
        this.type = type;
        this.errorMessage = errorMessage;
    }

    /**
     * Initializes class member variables.
     *
     * @param miscException A "miscellaneous" exception being wrapped by this class. Typically requires app termination.
     */
    public PAException(Exception miscException) {
        this.type = Type.MISC;
        this.errorMessage = miscException.getMessage();
        this.miscException = miscException;
    }

    @Override
    public String toString() {
        return errorMessage;
    }

    // getter methods for class member variables

    public Type getType() {
        return type;
    }

    public Exception getMiscException() {
        return miscException;
    }
}
