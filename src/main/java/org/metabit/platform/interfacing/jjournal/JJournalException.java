package org.metabit.platform.interfacing.jjournal;

/**
 * (unchecked) Exception class for JJournal
 *
 * @author jwilkes
 * @version $Id: $Id
 */
public class JJournalException extends RuntimeException
{

    /**
     * get the errorcode for further differentiation.
     * may be NUMERICAL_CODE with further processing required-
     *
     * @return enum value from ErrorCodes
     */
    public ErrorCodes getCode()
        { return errorcode; }

    /**
     * get the numerical sub-errorcode when the errorcode NUMERICAL_CODE is given.
     *
     * @return numerical value that may be mapped using strerror().
     */
    public int getNumericalErrorCode()
        { return returncode; }

    /**
     * error codes to differentiate by, e.g. with a switch-case.
     */
    public enum ErrorCodes
    {
        /**
         * no further interpretation of error code returned from underlying library; please check getNumericalErrorCode() provided.
         */
    NUMERICAL_CODE,
        /**
         * the error code received from underlying library should not have come up; it doesn't match the documented range. check getNumericalErrorCode()?
         */
    UNEXPECTED_RETURN_CODE,
        /**
         * "native" ByteBuffer not supported here; please provide one which has a backing array.
         */
    NEED_BUFFER_WITH_BYTE_ARRAY,
        /**
         * the buffer provided is too small for its purpose.
         */
    BUFFER_TOO_SMALL,
        /**
         * opening Journal failed.
         */
    FAILED_TO_OPEN,
        /**
         * the journal opened has no (visible/accessible) entries.
         */
    JOURNAL_HAS_NO_ENTRIES, // journal has no entries.
        /**
         * trying to access entries outside the valid journal range (e.g. cursor at past-end position, or before-start)
         */
    OOB,
        /**
         * field exists, but is not used/set. "null contents"
         */
    FIELD_EMPTY,
        /**
         *  no such field exists.
         */
    NO_SUCH_FIELD,
        /**
         * string provided is not valid for field names
         */
    FIELD_NAME_INVALID,
        /**
         * could not find specified timestamp in journal
         */
    TIME_NOT_FOUND,
        /**
         * performing filter operations without filter conditions defined
         */
    NO_FILTER_DEFINED,
        /**
         * consumer function of loop aborting it.
         */
    CONSUMER_ENDING_LOOP
    }

    /**
     * public constructor taking a systemd-journal C API return-code
     *
     * @param returncode the negative int value returned by the C API on errors. supposed to be resolvable by std::strerror, but not always clear in meaning.
     */
    public JJournalException(final int returncode)
        { this.returncode = returncode; this.errorcode = ErrorCodes.NUMERICAL_CODE; }

    /**
     * public constructor for known events
     *
     * @param errorcode code telling us what's the issue, roughly.
     */
    public JJournalException(final ErrorCodes errorcode)
        { this.returncode = 0; this.errorcode = errorcode; }


    /** JDK-8275192 */
    private final int returncode;
    /** JDK-8275192 */
    private final ErrorCodes errorcode;

    /** {@inheritDoc} */
    @Override
    public String toString()
        {
        switch (errorcode)
            {
            case NUMERICAL_CODE:
                switch (-returncode)
                    {
                    case 1: return "Operation not permitted";
                    case 10: return "No child processes"; // wtf?
                    case 99: return "cannot assign requested address / invalid data";
                    default: return "error with std::strerror value of " + (-returncode);
                    }
            case FAILED_TO_OPEN:    return "journal failed to open";
            case JOURNAL_HAS_NO_ENTRIES: return "journal has no entries / invalid journal";
            case BUFFER_TOO_SMALL:  return "buffer supplied to small to hold requested data";
            case NEED_BUFFER_WITH_BYTE_ARRAY: return "supply regular ByteBuffer please, not allocated with native";
            case UNEXPECTED_RETURN_CODE: return "we're confused about the returncode. internal error, possibly.";
            case OOB: return "trying to access entry while outside valid message range (e.g. on head or tail of the journal)";
            case FIELD_EMPTY: return "field type known, but is not set in this journal or journal entry";
            case NO_SUCH_FIELD: return "field name/ID given is not known at all";
            case TIME_NOT_FOUND: return "could not find specified timestamp in journal";
            case CONSUMER_ENDING_LOOP: return "consumer function aborted the loop";
            default:
                return "Internal Error: unexpected error state!";
            }
        }

}
