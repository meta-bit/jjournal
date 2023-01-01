/**
 * JJournal is an original work by metabit GmbH.
 * <br/>
 * It is dual-licensed under GPL-3 for non-commercial use.
 * For commercial use, obtain an Apache-2.0-style license via (free) registration from metabit.
 * See LICENSE file for details.
 */
package org.metabit.platform.interfacing.jjournal;

import jnr.ffi.*;
import jnr.ffi.Runtime;
import jnr.ffi.byref.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.regex.Pattern;
import static org.metabit.platform.interfacing.jjournal.JJournalException.ErrorCodes.*;

/**
 * jjournal main class for systemd journal read access.
 * <br/>
 * Instantiate with the appropriate set of access flags,
 * then use its member functions to select, read, iterate, filter etc.
 * systemd journal entries.
 * <br/>
 * This library reflects the concepts used in systemd journals,
 * especially way they designed cursor and filters.
 *
 * @author jwilkes
 * @version $Id: $Id
 */
public class Journal implements AutoCloseable
{

    /** Constant <code>ACCEPTABLE_FLAGS_FOR_DIRECTORY_OPEN</code> */
    public static final EnumSet<OpenFlags> ACCEPTABLE_FLAGS_FOR_DIRECTORY_OPEN = EnumSet.of(OpenFlags.OS_ROOT, OpenFlags.SYSTEM, OpenFlags.CURRENT_USER);

    // constructor helper to reduce code duplication
    private void init()
        {
        // libJournal needs to be loaded/instantiated first, e.g. by static function.
        if (libJournal == null)
            loadLib();
        // init standard things
        reusableByteBuffer = ByteBuffer.allocate(8192); //@OPTIMISE
        journalCharset = StandardCharsets.UTF_8; //@CHECK
        filtersActiveFlag = false;
        journalHandle = new AddressByReference();
        return;
        }

    /**
     * constructor with varargs.
     *
     * @param openFlags flags to use when opening the Journal access. See OpenFlags enum for details.
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException on severe errors
     */
    public Journal(final OpenFlags... openFlags)  throws JJournalException
        {
        EnumSet<OpenFlags> flags;
        // flags = EnumSet.copyOf(Arrays.asList(openFlags)); fails on empty array
        switch (openFlags.length)
            {
            case 0: flags = EnumSet.noneOf(OpenFlags.class); break;
            case 1: flags = EnumSet.of(openFlags[0]); break;
            default:flags = EnumSet.of(openFlags[0], openFlags);
            }

        init();
        // journalHandle needs to be set/provided first; it will be a member variable of this class.
        int ret = libJournal.sd_journal_open(journalHandle,OpenFlags.getCombinedFlagValue(flags));
        // throw exception if return code is not ==0
        if (ret != 0x00)
            throw new JJournalException(JJournalException.ErrorCodes.FAILED_TO_OPEN);
        actualJournalHandle = journalHandle.getValue();
        }

    /**
     * open journal accesss with flags.
     *
     * @param openFlags an EnumSet of flags.
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException
     *
     * example for specifying a single flag: EnumSet.of(OpenFlags.LOCAL_ONLY)
     */
    public Journal(Set<OpenFlags> openFlags) throws JJournalException
        {
        if (openFlags == null)
            openFlags = EnumSet.of(OpenFlags.ALL_NAMESPACES); // try some sensible default, then

        init();
        EnumSet<OpenFlags> flags = EnumSet.copyOf(openFlags);
        int ret = libJournal.sd_journal_open(journalHandle,OpenFlags.getCombinedFlagValue(flags));
        // throw exception if return code is not ==0
        if (ret != 0x00)
            throw new JJournalException(JJournalException.ErrorCodes.FAILED_TO_OPEN);
        actualJournalHandle = journalHandle.getValue();
        return;
        }


    /**
     * constructor, opening journal access within a namespace.
     * <p>
     * sd_journal_open_namespace() is similar to sd_journal_open() but takes an additional namespace parameter that specifies which journal namespace to operate on.
     * If specified as NULL the call is identical to sd_journal_open().
     * If non-NULL only data from the namespace identified by the specified parameter is accessed.
     * This call understands two additional flags:
     * if SD_JOURNAL_ALL_NAMESPACES is specified the namespace parameter is ignored and all defined namespaces are accessed simultaneously;
     * if SD_JOURNAL_INCLUDE_DEFAULT_NAMESPACE the specified namespace and the default namespace are accessed but no others (this flag has no effect when namespace is passed as NULL).
     * For details about journal namespaces see systemd-journald.service(8).
     * </p>
     *
     * @param namespace namespace to operate on
     * @param openFlags flags for opening journal access
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException on failure
     */
    public Journal(final String namespace, EnumSet<OpenFlags> openFlags) throws JJournalException
        {
        if ((namespace == null)||(openFlags == null))
            throw new IllegalArgumentException();
        init();
        EnumSet<OpenFlags> flags = EnumSet.copyOf(openFlags);

        int ret = libJournal.sd_journal_open_namespace(journalHandle,namespace,OpenFlags.getCombinedFlagValue(flags));
        // throw exception if return code is not ==0
        if (ret != 0x00)
            throw new JJournalException(JJournalException.ErrorCodes.FAILED_TO_OPEN);
        actualJournalHandle = journalHandle.getValue();
        return;
        }

    /**
     * constructor, opening journal access within a namespace.
     * <p>
     * sd_journal_open_namespace() is similar to sd_journal_open() but takes an additional namespace parameter that specifies which journal namespace to operate on.
     * If specified as NULL the call is identical to sd_journal_open().
     * If non-NULL only data from the namespace identified by the specified parameter is accessed.
     * This call understands two additional flags:
     * if SD_JOURNAL_ALL_NAMESPACES is specified the namespace parameter is ignored and all defined namespaces are accessed simultaneously;
     * if SD_JOURNAL_INCLUDE_DEFAULT_NAMESPACE the specified namespace and the default namespace are accessed but no others (this flag has no effect when namespace is passed as NULL).
     * For details about journal namespaces see systemd-journald.service(8).
     * </p>
     *
     * @param namespace namespace to operate on
     * @param openFlags flags for opening journal access
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException on failure
     *
     * NB: if you provide a String parameter, you'll get this - the namespace constructor, which is the one used most often.
     * If you are looking to use the file-based constructor, make sure to provide an List of String objects.
     */
    public Journal(final String namespace, OpenFlags... openFlags) throws JJournalException
        {
        if ((namespace == null)||(openFlags == null))
            throw new IllegalArgumentException();
        EnumSet<OpenFlags> flags;
        // flags = EnumSet.copyOf(Arrays.asList(openFlags)); fails on empty array
        switch (openFlags.length)
            {
            case 0: flags = EnumSet.noneOf(OpenFlags.class); break;
            case 1: flags = EnumSet.of(openFlags[0]); break;
            default:flags = EnumSet.of(openFlags[0], openFlags);
            }
        init();

        int ret = libJournal.sd_journal_open_namespace(journalHandle,namespace,OpenFlags.getCombinedFlagValue(flags));
        // throw exception if return code is not ==0
        if (ret != 0x00)
            throw new JJournalException(JJournalException.ErrorCodes.FAILED_TO_OPEN);
        actualJournalHandle = journalHandle.getValue();
        return;
        }



    /**
     * open one or multiple journal files.
     * "All files will be opened and interleaved automatically.
     * Please note that in the case of a live journal, this function is only useful for debugging,
     * because individual journal files can be rotated at any moment,
     * and the opening of specific files is inherently racy."
     *
     * As per documentation, there are no flags available or used here.
     *
     * @param filenames the files to use, with paths. (no special order, it might as well be a set).
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException exception on severe errors
     */
    public Journal(final List<String> filenames) throws JJournalException
        {
        if (filenames == null)
            throw new IllegalArgumentException();
        init();

        // convert input from a set (or list) into an NULL-terminated array.
        final Pointer stringArrayPointer = org.metabit.platform.interfacing.jnrffi.JNRFFISupportFunctions.generateNullTerminatedStringArray(runtime, filenames, StandardCharsets.UTF_8,0);
        // documentation quote: "This call also takes a flags argument, but it must be passed as 0 as no flags are currently understood for this call."
        int ret = libJournal.sd_journal_open_files(journalHandle, stringArrayPointer, 0);
        // throw exception if return code is not ==0
        if (ret != 0x00)
            throw new JJournalException(JJournalException.ErrorCodes.FAILED_TO_OPEN);
        actualJournalHandle = journalHandle.getValue();
        return;
        }


    /**
     * open a journal *directory*.
     * to differentiate from the open-files signature, this is intentionally using a single Path, instead of a String.
     *
     * @param directory path
     * @param openFlags This accepts OS_ROOT, SYSTEM, and CURRENT_USER flags.
     *              for OS_ROOT, the specified path is taken as root from where the relative subdirectories /var/log/journal and /run/log/journal are searched.
     *              SYSTEM and CURRENT_USER filter/limit the available files to system, and current user, respectively.
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException exception on severe errors
     */
    @Deprecated // not tested yet! it may still contain bugs.
    public Journal(final Path directory, Set<OpenFlags> openFlags) throws JJournalException
        {
        if ((directory == null)||(openFlags == null))
            throw new IllegalArgumentException();
        // check flags.
        EnumSet<OpenFlags> flags = EnumSet.copyOf(openFlags);
        boolean invalidInput = flags.retainAll(ACCEPTABLE_FLAGS_FOR_DIRECTORY_OPEN); // throw out all flags outside the acceptable range
        // we could quietly continue; but it seems better to notify of such programming errors.
        if (invalidInput)
            throw new IllegalArgumentException("flags supplied are not allowed on open-directory constructor");
        final String absoluteDirectoryPathString = directory.toAbsolutePath().toString();

        init();

        int ret = libJournal.sd_journal_open_directory(journalHandle, absoluteDirectoryPathString, OpenFlags.getCombinedFlagValue(flags));
        // throw exception if return code is not ==0
        if (ret != 0x00)
            throw new JJournalException(JJournalException.ErrorCodes.FAILED_TO_OPEN);
        actualJournalHandle = journalHandle.getValue();
        return;
        }

    /**
     * open a journal *directory*.
     * to differentiate from the open-files signature, this is intentionally using a single Path, instead of a String.
     *
     * @param directory path
     * @param openFlags This accepts OS_ROOT, SYSTEM, and CURRENT_USER flags.
     *              for OS_ROOT, the specified path is taken as root from where the relative subdirectories /var/log/journal and /run/log/journal are searched.
     *              SYSTEM and CURRENT_USER filter/limit the available files to system, and current user, respectively.
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException exception on severe errors
     */
    public Journal(final Path directory, OpenFlags... openFlags) throws JJournalException
        {
        if ((directory == null)||(openFlags == null))
            throw new IllegalArgumentException();
        EnumSet<OpenFlags> flags;
        // flags = EnumSet.copyOf(Arrays.asList(openFlags)); fails on empty array
        switch (openFlags.length)
            {
            case 0: flags = EnumSet.noneOf(OpenFlags.class); break;
            case 1: flags = EnumSet.of(openFlags[0]); break;
            default:flags = EnumSet.of(openFlags[0], openFlags);
            }
        if ((directory == null)||(flags == null))
            throw new IllegalArgumentException();
        // check flags.
        boolean invalidInput = flags.retainAll(ACCEPTABLE_FLAGS_FOR_DIRECTORY_OPEN); // throw out all flags outside the acceptable range
        // we could quietly continue; but it seems better to notify of such programming errors.
        if (invalidInput)
            throw new IllegalArgumentException("flags supplied are not allowed on open-directory constructor");
        final String absoluteDirectoryPathString = directory.toAbsolutePath().toString();
        init();

        int ret = libJournal.sd_journal_open_directory(journalHandle, absoluteDirectoryPathString, OpenFlags.getCombinedFlagValue(flags));
        // throw exception if return code is not ==0
        if (ret != 0x00)
            throw new JJournalException(JJournalException.ErrorCodes.FAILED_TO_OPEN);
        actualJournalHandle = journalHandle.getValue();
        return;
        }
     // sd_journal objects cannot be used in the child after a fork. Functions which take a journal object as an argument (sd_journal_next() and others) will return -ECHILD after a fork.

    /**
     * {@inheritDoc}
     *
     * close after use. Also used for AutoCloseable.
     */
    @Override
    public void close()
        {
        if (journalHandle == null)
            return; // ignore invalid state (or log if we have e.g. slf4j activated.)
        libJournal.sd_journal_close(actualJournalHandle);
        actualJournalHandle = null;
        //:delete journalHandle;
        journalHandle = null;
        return;
        }


    /**
     * move to first valid entry.
     *  (log is ordered chronologically)
     *
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException on failure
     */
    public void moveToEarliest() throws JJournalException
        {
        int r = libJournal.sd_journal_seek_head(actualJournalHandle);
        if (r< 0x00)
            { throw new JJournalException(r); }
        return;
        }

    /**
     * move to next entry.
     *
     * @return true if successful, false if not.
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException if the movement fails altogether in a bad way.
     *
     * useful for loops like this:
     *      `while(journal.moveToNext())  {...} `
     */
    public boolean moveToNext() throws JJournalException
        {
        int r = libJournal.sd_journal_next(actualJournalHandle);
        if (r == 1) // moved
            return true;
        if (r == 0x00) // not moved (e.g. EOF)
            return false;
        if (r < 0x00)
            { throw new JJournalException(r); } // error
        // (r > 1) ... we moved more than one step?!
        throw new JJournalException(UNEXPECTED_RETURN_CODE);
        }

    /**
     * move back one entry.
     *
     * @return true if successful, false if not.
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException if this goes really wrong
     */
    public boolean moveToPrevious() throws JJournalException
        {
        int r = libJournal.sd_journal_previous(actualJournalHandle);
        if (r == 1) // moved
            return true;
        if (r == 0x00) // not moved (e.g. EOF)
            return false;
        if (r < 0x00)
            { throw new JJournalException(r); }
        // (r > 1) ... we moved more than one step?!
        throw new JJournalException(UNEXPECTED_RETURN_CODE);
        }


    /**
     * skip a number of entries
     *
     * @param numEntriesToSkip positive to skip forward, negative to skip backwards.
     * @return difference between requested skip steps, and actual ones.
     *         if the move was performed as asked, this is 0.
     *         if the move was only partially performed, because the cursor hit the end, this is &gt;0 and the number of "missing skips"
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException if the movement failed entirely
     *
     * usage e.g. while (journal.skip(SKIPAHEADVALUE) == 0) ...
     */
    public long skip(long numEntriesToSkip) throws JJournalException
        {
        long diff;
        if (numEntriesToSkip == 0x00)
            return 0x00;
        if (numEntriesToSkip > 0x00)
            {
            long r;
            r = libJournal.sd_journal_next_skip(actualJournalHandle, numEntriesToSkip);
            diff = numEntriesToSkip - r;
            }
        else
            {
            long r;
            r = libJournal.sd_journal_previous_skip(actualJournalHandle, -numEntriesToSkip);
            diff = -(numEntriesToSkip + r); //@CHECK
            }
        if (diff >= 0x00)
            { return diff; }
        else
            { throw new JJournalException((int) diff); } // error
        }


    /**
     * move to last valid entry.
     *  (log is ordered chronologically)
     *
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException on failure
     */
    public void moveToLatest() throws JJournalException
        {
        int r;
        r = libJournal.sd_journal_seek_tail(actualJournalHandle);
        if (r< 0x00)
            { throw new JJournalException(r); }
        return;
        }

    //---------------------------------------------------------------------------------------------------------------

/*
<quote>
        Note that these calls do not actually make any entry the new
       current entry, this needs to be done in a separate step with a
       subsequent sd_journal_next(3) invocation (or a similar call).
       Only then, entry data may be retrieved via sd_journal_get_data(3)
       or an entry cursor be retrieved via sd_journal_get_cursor(3). If
       no entry exists that matches exactly the specified seek address,
       the next closest is sought to.
</quote>
*/

    /**
     * get current cursor value as String.
     *
     * @return current journal cursor as String.
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException on severe failures.
     *
     * "The cursor identifies a journal entry globally and in a stable way"
     *
     * Debugging hint: if you get no cursor on a journal, you may have opened something empty or nonexistent. (e.g. accessing a file as namespace)
     */
    public String getCursorRaw() throws JJournalException
        {
        int r;
        r = libJournal.sd_journal_get_cursor(actualJournalHandle, reusablePointerByReference);
        if (r< 0x00)
            { throw new JJournalException(r); }
        return reusablePointerByReference.getValue().getString(0x00); // getString parameter is the offset.
        }

    /**
     * test whether current position matches supplied cursor.
     *
     * @param cursorValue the cursor to use
     * @return true if valid, false if invalid
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException on severe failures.
     */
    public boolean testCursorRaw(final String cursorValue) throws JJournalException
        {
        int r = libJournal.sd_journal_test_cursor(actualJournalHandle, cursorValue);
        if (r > 0x00)
            return true;
        if (r == 0x00)
            return false;
        if (r == -99)
            throw new IllegalArgumentException("parameter is not a valid journald cursor");
        // and for (r < 0)
        throw new JJournalException(r); //99 meaning invalid cursor!
        }

    /**
     * journal cursor seek function, wrapped for Java.
     *
     * @param cursorValue the string value containing the opaque cursor
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException if things go seriously wrong
     */
    public void seekCursorRaw(final String cursorValue) throws JJournalException
        {
        int r = libJournal.sd_journal_seek_cursor(actualJournalHandle, cursorValue);
        if (r!= 0x00)
            { throw new JJournalException(r); }
        return;
        }

    /**
     * move journal access to a provided cursor; going to the first valid entry on that cursor.
     *
     * @param cursorValue the cursor to seek=move to
     * @return true if successful, false if not.
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException if things go seriously wrong
     */
    public boolean seekCursor(final String cursorValue) throws JJournalException
        {
        seekCursorRaw(cursorValue);
        // and have we actually arrived at the requested cursor now?
        //@CHECK not certain the interpretation of documentation is entrirely valid - or whether these two steps should be left to the not-raw-version
        moveToNext(); // so we got to the *valid* entry from the cursor.
        return testCursorRaw(cursorValue);
        }

    //---------------------------------------------------------------------------------------------------------------

    /**
     * list all available fields in current journal, insofar defined in the fields enum.
     * additional fields are ignored - see getAvailableFieldsAsString() for the alternative encompassing these.
     *
     * @return the set of fields defined in this journal
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException if things go seriously wrong
     */
    public EnumSet<JournalField> getAvailableFields() throws JJournalException
        {
        EnumSet<JournalField> fieldsFound = EnumSet.noneOf(JournalField.class);
        int r = 1;
        libJournal.sd_journal_restart_fields(actualJournalHandle);
        // "Note that this call is subject to the data field size threshold as controlled by sd_journal_set_data_threshold()"
        while (r > 0x00)
            {
            r = libJournal.sd_journal_enumerate_fields(actualJournalHandle, reusablePointerByReference);
            switch (r)
                {
                case 0x00: break; // end loop
                case 1:
                    String tmp2 = reusablePointerByReference.getValue().getString(0x00);
                    // map to field enum
                    try
                        {
                        final JournalField field = JournalField.lookup(tmp2);
                        fieldsFound.add(field);
                        }
                    catch ( IllegalArgumentException ex)
                        {
                        // log "unknown field" in higher debug levels
                        // ignore otherwise.
                        }
                    break;
                default: // especially when <0
                    throw new JJournalException(r);
                }
            }
        return fieldsFound;
        }


    /**
     * list all available fields in current journal, as Strings.
     *
     * @return the set of field names in this journal
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException if things go seriously wrong
     */
    public Set<String> getAvailableFieldsAsStrings() throws JJournalException
        {
        Set<String> fieldsFound = new HashSet<>(); // or list? vector
        int r = 1;
        libJournal.sd_journal_restart_fields(actualJournalHandle);
        // "Note that this call is subject to the data field size threshold as controlled by sd_journal_set_data_threshold()"
        while (r > 0x00)
            {
            r = libJournal.sd_journal_enumerate_fields(actualJournalHandle, reusablePointerByReference);
            switch (r)
                {
                case 0x00:
                    break; // end loop
                case 1:
                    // "The returned data is in a read-only memory map and is only valid until the next invocation of sd_journal_enumerate_fields()."
                    // so we store a copy of the String, to be on the safe side.
                    String tmp2 = reusablePointerByReference.getValue().getString(0x00);
                    fieldsFound.add(tmp2);
                    break;
                default: // especially when <0
                    throw new JJournalException(r);
                }
            }
        return fieldsFound;
        }
    //------------------

/* --- ggf. für roberts bedarf ---
    int sd_journal_enumerate_data(@In AddressByReference handle, @Out PointerByReference data, @Out @size_t IntByReference length);// const void **data, size_t *l  -  not tested yet
    // the @Out ByReference<size_t> length construct doesn't look that good. fall back only if we have to.
    int sd_journal_enumerate_available_data(@In AddressByReference handle, @Out  PointerByReference data, @Out @size_t IntByReference length); // const void **data, size_t *l  -  not tested yet
    void sd_journal_restart_data(@In AddressByReference handle);  // -  not tested yet
*/

    //-----------------------------------------------------------------------------------------------------------------
    /**
     * get current journal entry timestamp as Java8 instant
     *
     * @return Instant of the current journal entry
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException if things go wrong (e.g. no valid current entry)
     */
    public Instant getTimestampAsInstant() throws JJournalException
        {
        int r = libJournal.sd_journal_get_realtime_usec (actualJournalHandle, reusableLongLongByReference);
        if (r != 0x00)
            throw new JJournalException(r);
        long usec = reusableLongLongByReference.longValue(); // C/C++ long long maps to Java Long; both are 64 bit in size.
        final long seconds     = usec/Journal.MILLION; // the seconds part.
        final long nanoseconds = usec%Journal.MILLION*1000; // the microseconds, then scaled to nanoseconds
        return Instant.ofEpochSecond(seconds, nanoseconds);
        }

    /**
     * get raw microseconds value of the current journal entry.
     *
     * @return a java Long containing the microseconds of the current journal entry.
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException if things go wrong (e.g. no valid current entry)
     */
    public long getTimestampAsMicrosecondsLong() throws JJournalException
        {
        int r = libJournal.sd_journal_get_realtime_usec (actualJournalHandle, reusableLongLongByReference);
        if (r != 0x00)
            throw new JJournalException(r);
        return reusableLongLongByReference.longValue();
        }

    /**
     * get the timeframe the current journal is covering - its earliest, and its latest entry,
     * specified in microseconds.
     *
     * it's a pity java doesn't have a tuple, nor a proper "Period" class representing an start-to-end value.
     * java.time.Period instead is just a distance vector, just like Duration. I'd call that a naming failure.
     *
     * @return a library class containing the "since" and "until" time
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException if things go wrong
     */
    public  org.metabit.platform.interfacing.jjournal.Period getFirstAndLastInstant() throws JJournalException
        {
        // the single reusable one doesn't cut it... yet
        LongLongByReference since = new LongLongByReference();
        LongLongByReference until = new LongLongByReference();
        int r = libJournal.sd_journal_get_cutoff_realtime_usec(actualJournalHandle, since, until);
        if (r != 0x00)
            throw new JJournalException(r);
        return new Period(since.longValue(), until.longValue());
        }


    /**
     * move forward in the journal until a given time is matched.
     *
     * @param time the earliest instance the move may stop at.
     * @return true if we arrived at a match, false if we did not (e.g. too late).
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException on severe problems.
     *
     * UNTESTED -- TODO: use seek instead, sd_journal_seek_realtime_usec
     */
    public boolean moveForwardUntilTime(final Instant time) throws JJournalException
        {
        long targetTimeAsLong = (time.getEpochSecond() *Journal.MILLION) + time.getLong(ChronoField.MICRO_OF_SECOND);
        LongLongByReference reusableLongLongByReference = new LongLongByReference();
        int r;
        do
            {
            r = libJournal.sd_journal_next(actualJournalHandle);
            switch (r)
                {
                case 1: break; // all OK, as expected
                case 0x00: return false; // hit end, stop
                default: throw new JJournalException(r);
                }
            r = libJournal.sd_journal_get_realtime_usec(actualJournalHandle, reusableLongLongByReference);
            if (r != 0x00)
                throw new JJournalException(r);
            if ( reusableLongLongByReference.longValue() >=  targetTimeAsLong)
                return true; // match found!
            }
        while( r == 0x00);
        return false; // no match found before end.
        }

    /**
     * move backward in the journal until a given time is matched.
     *
     * @param time the latest instance the move may stop at.
     *             consider moveBackwardUntilEarliestTime if you want the earliest entry
     * @return true if we arrived at a match, false if we did not (e.g. too late).
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException on severe problems.
     *
     * UNTESTED -- TODO: use seek instead, sd_journal_seek_realtime_usec
     */
    public boolean moveBackwardUntilLatestTime(final Instant time) throws JJournalException
        {
        long targetTimeAsLong = (time.getEpochSecond() *Journal.MILLION) + time.getLong(ChronoField.MICRO_OF_SECOND);
        int r;
        do
            {
            r = libJournal.sd_journal_get_realtime_usec (actualJournalHandle, reusableLongLongByReference);
            if (r != 0x00)
                throw new JJournalException(r);
            if ( reusableLongLongByReference.longValue() <=  targetTimeAsLong)
                return true; // match found!
            r = libJournal.sd_journal_previous(actualJournalHandle);
            }
        while( r > 0x00);
        return false; // no match found before end.
        }

    /**
     * moveBackwardUntilEarliestTime move to the first
     *
     * @param time the earliest time acceptable
     * @return true if this function could achieve its goal, false if not.
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException if there was an error, e.g. journal access failing.
     *
     * UNTESTED
     */
    public boolean moveBackwardUntilEarliestTime(final Instant time) throws JJournalException
        {
        long targetTimeAsLong = (time.getEpochSecond() *Journal.MILLION) + time.getLong(ChronoField.MICRO_OF_SECOND);
        int r;
        // --- first, find first match exactly as in moveBackwardUntilLatestTime
        do
            {
            r = libJournal.sd_journal_get_realtime_usec (actualJournalHandle, reusableLongLongByReference);
            if (r != 0x00)
                throw new JJournalException(r);
            if ( reusableLongLongByReference.longValue() >=  targetTimeAsLong)
                break; // match found!
            r = libJournal.sd_journal_previous(actualJournalHandle);
            if (r != 0x00)
                throw new JJournalException(r);
            }
        while( r > 0x00);
        if (r == 0x00)
            return false; // loop ended not because of match.
        //--- now move back some more until we don't match anymore
        do
            {
            r = libJournal.sd_journal_get_realtime_usec (actualJournalHandle, reusableLongLongByReference);
            if (r != 0x00)
                throw new JJournalException(r);
            if ( reusableLongLongByReference.longValue() == targetTimeAsLong)
                return true; // match found!
            r = libJournal.sd_journal_previous(actualJournalHandle);
            if (r != 0x00)
                throw new JJournalException(r);
            }
        while( r > 0x00);
        if (r == 0x00)
            return false; // loop ended not because of match.
        // and then exactly one step forward.
        r = libJournal.sd_journal_next(actualJournalHandle);
        if (r != 0x00)
            throw new JJournalException(r);
        return true; // no match found before end.
        }

    // our foreach in timerange-function...
    // -- move to start
    // -- move forward to start
    // -- until we end the loop, perform a call for each entry

    /**
     * loop over journal entries within a given timeframe.
     * note this does not do a moveToEarliest() by itself (anymore). It starts from current position.
     *
     * @param startTime             the time of the first entry to operate on, from current position onward.
     *                              "null" for: do not move.
     *                              use moveToEarliest(); before calling this function if you want to start from the earliest entry.
     * @param endTime               the time of the first entry the range ended (that is, not processed anymore - one past last)
     *                              "null" for tail of journal.
     * @param limitCount            abort after this many entries having been processed.
     * @param toBeCalledOnEachEntry Consumer to be called on each entry.
     *                              <p>
     *                              If you need to abort the loop, suggested way is to throw a RuntimeException, or an JJournalException.
     *                              There is also JJournalException(CONSUMER_ENDING_LOOP) for the specific purpose of identifying this.
     * @return number of entries processed in this range. 0 if empty/none found matching.
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException on errors, or on Consumer function aborting the loop.
     */
    public int foreachInTimerange(final Instant startTime, final Instant endTime, int limitCount, JournalConsumer<Journal, JJournalException> toBeCalledOnEachEntry) throws JJournalException
        {
        // idea: or count entries, and return the count? ==0 would be empty.
        int r;
        int entriesProcessed = 0;
        Period currentFirstAndLast;
        if (startTime == null)
            {
            // ignore, do not move.
            // previous behaviour:  moveToEarliest(); by default, move to first entry.-
            }
        else
            {
            if (moveForwardUntilTime(startTime) == false)
                throw new JJournalException(TIME_NOT_FOUND);
            }

        long endTimeAsLong;
        if (endTime == null)
            {
            // get current last journal entry time as default
            LongLongByReference until = new LongLongByReference();
            r = libJournal.sd_journal_get_cutoff_realtime_usec(actualJournalHandle, this.reusableLongLongByReference, until);
            if (r != 0x00)
                throw new JJournalException(r);
            endTimeAsLong = until.longValue();
            }
        else
            {
            endTimeAsLong = (endTime.getEpochSecond() * MILLION) + endTime.getLong(ChronoField.MICRO_OF_SECOND);
            }
        // direction is fixed here
        do
            {
            toBeCalledOnEachEntry.accept(this); // @TODO ponder additional error handling
            r = libJournal.sd_journal_get_realtime_usec (actualJournalHandle, reusableLongLongByReference);
            if (r != 0x00)
                throw new JJournalException(r);
            if ( reusableLongLongByReference.longValue() >=  endTimeAsLong)
                break; // end-time reached.
            entriesProcessed++;
            if (entriesProcessed >=  limitCount)
                return entriesProcessed;
            r = libJournal.sd_journal_next(actualJournalHandle);
            if (r < 0x00)
                throw new JJournalException(r);
            }
        while( r > 0x00);
        return entriesProcessed;
        }

    //===============================================================================================================
    // filtering
    /**
     * <p>hasActiveFilters.</p>
     *
     * @return a boolean
     */
    public boolean hasActiveFilters()
        { return filtersActiveFlag; }

    /*
    Parameter data must be of the form
       "FIELD=value", where the FIELD part is a short uppercase string
       consisting only of 0–9, A–Z and the underscore; it may not begin
       with two underscores or be the empty string. The value part may
       be anything, including binary. Parameter size specifies the
       number of bytes in data (i.e. the length of FIELD, plus one, plus
       the length of value). Parameter size may also be specified as 0,
       in which case data must be a NUL-terminated string, and the bytes
       before the terminating zero are used as the match.
     */

    /**
     * add a filter for a field to match a String
     *
     * @param journalField field to match
     * @param valueToMatch value to match exactly with
     * @throws java.lang.IllegalArgumentException if the field name string provided doesn't match an actual field.
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException if any.
     */
    public void filteringAddFilterExpressionExactMatch(final String journalField, final String valueToMatch) throws JJournalException
        {
        validateFieldName(journalField);
        this.filteringAddFilterExpressionExactMatchUnchecked(journalField, valueToMatch.getBytes(this.journalCharset));
        return;
        }

    /**
     * add a filter for a field to match a String
     *
     * @param journalField field to match
     * @param valueToMatch value to match exactly with
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException if any.
     */
    public void filteringAddFilterExpressionExactMatch(JournalField journalField, final String valueToMatch) throws JJournalException
        {
        this.filteringAddFilterExpressionExactMatchUnchecked(journalField.getValue(),valueToMatch.getBytes(this.journalCharset));
        return;
        }

    /**
     * add a filter for a field to match a byte sequence
     *
     * @param journalField field to match
     * @param valueToMatch value to match exactly with
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException on severe errors
     */
    public void filteringAddFilterExpressionExactMatch(JournalField journalField, final byte[] valueToMatch) throws JJournalException
        {
        this.filteringAddFilterExpressionExactMatchUnchecked(journalField.getValue(), valueToMatch);
        return;
        }

    /**
     * add a filter for a field name outside the predefined ones.
     *
     * @param fieldName field to match. may contain 0-9, A-Z and _ only.
     * @param valueToMatch value to match exactly with
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException if the field name is invalid, or something else went wrong.
     */
    public void filteringAddFilterExpressionExactMatch(final String fieldName, final byte[] valueToMatch) throws JJournalException
        {
        // check field name for correct value
        if (JournalField.checkFieldNameValidity(fieldName) == false)
            throw new JJournalException(FIELD_NAME_INVALID);
        this.filteringAddFilterExpressionExactMatchUnchecked(fieldName, valueToMatch);
        return;
        }





    // add the filter, but don't check the field name string.
    private void filteringAddFilterExpressionExactMatchUnchecked(final String fieldName, final byte[] valueToMatch)  throws JJournalException
        {
        int combinedLength = fieldName.length() + 1 + valueToMatch.length;
        ByteBuffer constructed = ByteBuffer.allocate(combinedLength);
        constructed.put(fieldName.getBytes(journalCharset));
        constructed.put((byte) 0x3D); // '=' in ASCII
        constructed.put(valueToMatch);
        constructed.flip(); // we *do* need to flip the buffer here.
        int r = libJournal.sd_journal_add_match(actualJournalHandle, constructed, combinedLength);
        if (r != 0x00)
            throw new JJournalException(r);
        filtersActiveFlag = true;
        return;
        }
    /**
     * add an "OR" to the filter expression list.
     *
     * It connects all filteringAddFilter* entries added since
     *  (a) most recent OR,
     *  (b) most recent AND;
     *  (c) the start of the filter expression list,
     *  whichever comes first,
     *
     * in an logical OR
     * with all matches added afterwards, until
     *  (a) next OR
     *  (b) next AND
     *  (c) the end of filter expression list
     *  whichever comes next.
     *
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException if this fails, or if it is invoked without and filters defined.
     */
    public void filteringAddDisjunctionOperator() throws JJournalException
        {
        if (! filtersActiveFlag)
            throw new JJournalException(JJournalException.ErrorCodes.NO_FILTER_DEFINED);
        int r = libJournal.sd_journal_add_disjunction(actualJournalHandle);
        if (r != 0x00)
            throw new JJournalException(r);
        return;
        }

/*quote from manpage:
 sd_journal_add_conjunction() may be used to insert a conjunction(i.e. logical AND) in the match list.
 If this call is invoked, all previously added matches since the last invocation of   sd_journal_add_conjunction()
  are combined in an AND with all matches added afterwards,
  until sd_journal_add_conjunction() is invoked again to begin the next AND term.
...
  Note that sd_journal_add_conjunction() operates one level 'higher' than sd_journal_add_disjunction().

  It is hence possible  to build an expression of AND terms, consisting of OR terms, consisting of AND terms, consisting of OR terms of matches
  (the latter OR expression is implicitly created for matches with the same field name, see above).
 */
    /**
     * add an "AND" to the filter expression list.
     *
     * It connects all filteringAddFilter* entries added since
     *  (a) most recent AND;
     *  (b) the start of the filter expression list,
     *  whichever comes first,
     *
     * in an logical OR
     * with all matches added afterwards, until
     *  (a) next AND
     *  (b) the end of filter expression list
     *  whichever comes next.
     *
     * This AND operator takes precedence over the OR operator.
     *
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException if this fails, or if it is invoked without and filters defined.
     */
    public void filteringAddConjunctionOperator() throws JJournalException
        {
        if (! filtersActiveFlag)
            throw new JJournalException(JJournalException.ErrorCodes.NO_FILTER_DEFINED);
        int r = libJournal.sd_journal_add_conjunction(actualJournalHandle);
        if (r != 0x00)
            throw new JJournalException(r);
        return;
        }

    /**
     * reset the filter list.
     */
    public void filteringReset()
        {
        libJournal.sd_journal_flush_matches(actualJournalHandle);
        filtersActiveFlag = false;
        return;
        }



    //===============================================================================================================
    /**
     * read a field as String
     *
     * @param fieldname ID of the field to be read
     * @return field contents (without field name or separating :)
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException on failure - e.g. if the field does not exist
     * @throws IllegalArgumentException if the fieldname contains invalid characters
     * note: this reuses the internal buffer, so it is even less thread-safe than other calls.
     */
    public String readFieldAsString(final String fieldname) throws JJournalException, IllegalArgumentException
        {
        ((java.nio.Buffer) this.reusableByteBuffer).clear();
        validateFieldName(fieldname);
        this.readDataToBuffer(fieldname,this.reusableByteBuffer);
        final CharBuffer chardings = this.journalCharset.decode(reusableByteBuffer);
        // now trim the FIELDNAME= intro bit
        return chardings.toString().substring(fieldname.length() + 1);
        }

    /**
     * read a field as byte[].
     * The byte array is, as is wasteful Java tradition, allocated for this purpose only.
     *
     * @param fieldname ID of the field to be read
     * @return a newly allocated byte array containing the field contents.
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException on failure - e.g. if the field does not exist
     * @throws IllegalArgumentException if the fieldname contains invalid characters
     **/
    public byte[] readFieldAsByteArray(final String fieldname) throws JJournalException, IllegalArgumentException
        {
        ((java.nio.Buffer) this.reusableByteBuffer).clear();
        validateFieldName(fieldname);
        this.readDataToBuffer(fieldname,this.reusableByteBuffer);
        int headerLength = fieldname.length()+1;
        int dataSize = ((java.nio.Buffer) this.reusableByteBuffer).limit() - headerLength;
        byte[] newAllocation = new byte[dataSize];
        System.arraycopy(((java.nio.Buffer) this.reusableByteBuffer).array(), headerLength, newAllocation, 0x00, dataSize);
        /// ((java.nio.Buffer) this.reusableByteBuffer).get(newAllocation,headerLength,dataSize);
        return newAllocation;
        }

    /**
     * read a field into existing byte[].
     * This call retains the fieldname and separator!
     *
     * @param fieldname ID of the field to be read
     * @param destinationBuffer ByteBuffer for storing the result in. caller needs to take care of position, mark, limit etc.
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException on failure - e.g. if the field does not exist
     * @throws IllegalArgumentException if the fieldname contains invalid characters
     */
    public void readFieldToByteArray(final String fieldname, ByteBuffer destinationBuffer) throws JJournalException, IllegalArgumentException
        {
        ((java.nio.Buffer) this.reusableByteBuffer).clear();
        validateFieldName(fieldname);
        this.readDataToBuffer(fieldname,destinationBuffer);
        return;
        }

    //---- not exactly deprecated... just possible candidates for deprecation.
    /**
     * read a field as String
     *
     * @param field ID of the field to be read
     * @return a newly allocated byte array containing the field contents.
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException on failure - e.g. if the field does not exist
     */
    public String readFieldAsString(final JournalField field) throws JJournalException
        {
        ((java.nio.Buffer) this.reusableByteBuffer).clear();
        this.readDataToBuffer(field.getValue(),this.reusableByteBuffer);
        final CharBuffer chardings = this.journalCharset.decode(reusableByteBuffer);
        // now trim the FIELDNAME= intro bit
        return chardings.toString().substring(field.getValue().length() + 1);
        }

    /**
     * read a field as byte[].
     * The byte array is, as is wasteful Java tradition, allocated for this purpose only.
     *
     * @param field ID of the field to be read
     * @return a newly allocated byte array containing the field contents.
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException on failure - e.g. if the field does not exist
     */
    public byte[] readFieldAsByteArray(final JournalField field) throws JJournalException
        {
        ((java.nio.Buffer) this.reusableByteBuffer).clear();
        this.readDataToBuffer(field.getValue(),this.reusableByteBuffer);
        int headerLength = field.getValue().length()+1;
        int dataSize = ((java.nio.Buffer) this.reusableByteBuffer).limit() - headerLength;
        byte[] newAllocation = new byte[dataSize];
        System.arraycopy(((java.nio.Buffer) this.reusableByteBuffer).array(), headerLength, newAllocation, 0x00, dataSize);
        /// ((java.nio.Buffer) this.reusableByteBuffer).get(newAllocation,headerLength,dataSize);
        return newAllocation;
        }

    /**
     * read a field as String
     *
     * @return full contents of the field, including the FIELDNAME header.
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException on failure - e.g. if the field does not exist
     * note: this reuses the internal buffer, so it is even less thread-safe than other calls.
     * @param field a {@link org.metabit.platform.interfacing.jjournal.JournalField} object
     */
    public String readFieldAsStringRaw(JournalField field) throws JJournalException
        {
        ((java.nio.Buffer) this.reusableByteBuffer).clear();
        this.readDataToBuffer(field.getValue(),this.reusableByteBuffer);
        // proper conversion to String as follows:
        final CharBuffer chardings = this.journalCharset.decode(reusableByteBuffer);
        return chardings.toString();
        }

    /**
     * read a complete set of fields.
     * @param fieldnames a collection of fields to read. illegal fieldnames cause an exception; non-existing fields will appear, with the value to be set (overwritten) to null.
     * @param targetMap  collection to place the findings in.
     * @throws JJournalException on severe errors.
     */
    public void readFieldsAsStrings(List<String> fieldnames, Map<String,String> targetMap) throws JJournalException
        {
        if ((fieldnames == null)||(targetMap == null))
            throw new IllegalArgumentException("null pointer");
        // validate all field names before the first read, otherwise the map may be half-updated on exception. -- optimise away?
        // fieldnames.forEach(fieldname -> validateFieldName(fieldname)); // will throw exception on invalid ones.
        // perform the actual reading.
        fieldnames.forEach(fieldname -> targetMap.put(fieldname, readFieldAsStringReturnNullOnEmpty(fieldname)));
        return;
        }

    /**
     * read a complete set of fields, using the JournalField enum.
     * @param fields a collection of fields to read. empty fields will get their corresponding values set to null.
     * @param targetMap  collection to place the findings in.
     * @throws JJournalException on severe errors.
     */
    public void readFieldsAsStrings(Set<JournalField> fields, Map<JournalField, String> targetMap) throws JJournalException
        {
        if ((fields == null)||(targetMap == null))
            throw new IllegalArgumentException("null pointer");
        fields.forEach(field -> targetMap.put(field, readFieldAsStringReturnNullOnEmpty(field.getValue())));
        return;
        }

    /* private helper function */
    private String readFieldAsStringReturnNullOnEmpty(final String fieldname) throws JJournalException
        {
        try
            {
            return readFieldAsString(fieldname);
            }
        catch (JJournalException jjex)
            {
            if (jjex.getCode() == FIELD_EMPTY)
                return null;
            else
                throw jjex;
            }
        }

    //-----------------------------------------------------------------------------------------------------------------

    /**
     * read field data to buffer.
     * Lower level access; you have to take care of the buffer, conversion etc. yourself.
     * This is suitable for accessing binary data.
     * The easier way in most cases is to use readFieldAsString().
     *
     * @param fieldName the name of the field to be read, as String
     * @param buffer the ByteBuffer to read the data into. You have to allocate and prepare (e.g. clear, flip etc) the buffer yourself.
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException when things go wrong. org.metabit.platform.interfacing.jjournal.JJournalException BUFFER_TOO_SMALL exception when the message doesn't fit in.
     */
    public void readDataToBuffer(String fieldName, ByteBuffer buffer) throws JJournalException
        {
        if (buffer.hasArray() ==  false)
            throw new JJournalException(NEED_BUFFER_WITH_BYTE_ARRAY); // that is, one that is allcoated not DIRECT
        int ret;
        final IntByReference datalength = new IntByReference();
        // OK, pointer by reference looks about right - and works.
        PointerByReference messageBuffer = new PointerByReference();
        ret = libJournal.sd_journal_get_data(actualJournalHandle, fieldName, messageBuffer, datalength);
        if (ret != 0x00)
            {
            switch (-ret)
                {
                case 2: throw new JJournalException(FIELD_EMPTY);
                case 22: throw new JJournalException(NO_SUCH_FIELD);
                case 99: throw new JJournalException(OOB);
                }
            throw new JJournalException(ret);
            }
        // now get the actual data out of it.
        Pointer ptr = messageBuffer.getValue();
        final Integer len = datalength.getValue();
        byte[] byteBuffer = buffer.array();
        if (byteBuffer.length < len)
            {
            // we might want to know how small exactly...
            throw new JJournalException(BUFFER_TOO_SMALL); // let's find a more elegant way later.
            }
        ptr.get(0L, byteBuffer, 0x00, len);
        buffer.position(0x00);
        buffer.limit(len);
        return;
        }

    /**
     * how long is that field anyhow?
     *
     * @param fieldName field to check for its length
     * @return the size of the field, in bytes.
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException on errors, especially invalid field names (and being on an invalid position).
     */
    public int getDataSize(final String fieldName) throws JJournalException
        {
        final IntByReference datalength = new IntByReference();
        PointerByReference messageBuffer = new PointerByReference(); // necessary though ignored
        int ret = libJournal.sd_journal_get_data(actualJournalHandle, fieldName, messageBuffer, datalength);
        if (ret != 0x00)
            {
            switch (-ret)
                {
                case 99:
                    throw new JJournalException(JJournalException.ErrorCodes.OOB);
                default:
                    throw new JJournalException(ret);
                }
            }
        return datalength.getValue();
        }

    /**
     * convenience function to read MESSAGE field of current journal entry.
     * not threadsafe.
     *
     * @return String containing the journal entry message
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException exception on severe errors
     */
    public String readMessageField() throws JJournalException
        {
        return readFieldAsString(JournalField.MESSAGE);
        }

    /**
     *  as the function name says: getBytesUsedByJournalFiles
     *
     * @return long containing the bytes used by journal files
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException when things go wrong.
     */
    public long getBytesUsedByJournalFiles() throws JJournalException
        {
        LongLongByReference byby = new LongLongByReference();
        int r= libJournal.sd_journal_get_usage(actualJournalHandle, byby);
        if (r == 0x00)
            { return byby.getValue();}
        else
            { throw new JJournalException(r); }
        }

    /**
     * check if runtime journal files have been found.
     *
     * @return true if runtime journal files have been found.
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException  when things go wrong.
     * see directory /run/systemd/journal/
     */
    public boolean hasRuntimeFiles() throws JJournalException
        {
        int r = libJournal.sd_journal_has_runtime_files(actualJournalHandle);
        return (r > 0x00) ? true : false;
        }

    /**
     * check if persistent journal files have been found.
     *
     * @return true if persistent journal files have been found.
     * @throws org.metabit.platform.interfacing.jjournal.JJournalException when things go wrong.
     * see directory /var/log/journal/
     */
    public boolean hasPersistentFiles() throws JJournalException
        {
        int r = libJournal.sd_journal_has_persistent_files(actualJournalHandle);
        return (r > 0x00) ? true : false;
        }

    /**
     * <p>Getter for the field <code>journalTimeZoneOffset</code>.</p>
     *
     * @return a {@link java.time.ZoneOffset} object
     */
    public ZoneOffset getJournalTimeZoneOffset()
        {
        return journalTimeZoneOffset;
        }

    /**
     * <p>Setter for the field <code>journalTimeZoneOffset</code>.</p>
     *
     * @param journalTimeZoneOffset a {@link java.time.ZoneOffset} object
     */
    public void setJournalTimeZoneOffset(ZoneOffset journalTimeZoneOffset)
        {
        this.journalTimeZoneOffset = journalTimeZoneOffset;
        }



    /**
     * validate field names. returns quietly if everything is ok.
     *
     * @param journalField string to be used as a journal field name.
     *                     Use Uppercase ASCII characters, digits, and underscores only.
     * @throws java.lang.IllegalArgumentException if the string is refused
     *
     *
     * man sd_journal_send:
     * <CITE>
     * ...The variable name must be in uppercase and consist only of characters,
     *        numbers and underscores, and may not begin with an underscore.[...] A number
     *        of well-known fields are defined, see systemd.journal-fields(7) for details, but
     *        additional application defined fields may be used.
     * </CITE>
     *
     * This omits specifying which charset this refers to; so "ÖRTLICHKEIT" would be valid too.
     * But judging from all examples, and the source code, interpretation of this library is
     * that this is *meaning* ASCII uppercase.
     */
    public static void validateFieldName(final String journalField) throws JJournalException
        {
        if (JOURNAL_FIELD_NAME_REGEXP.matcher(journalField).matches())
            return;
        else
            throw new JJournalException(FIELD_NAME_INVALID);
        }


    private UUID turnTwoLongsIntoUUID(long high, long low)
        {
        // as input, sd-id128. which exists als uint64_t qwords[2] - which is safer re: endinaness.
        // that byte array is Big-Endian (yay!).
        // " All 128-bit IDs generated by the sd-id128 APIs strictly conform to Variant 1 Version 4 UUIDs, as per RFC 4122." (quote from sd-id128(3) man page)
        // get two longs from pointer first...
        return new UUID(high, low);
        }

    /*
    loading the underlying native library, and initialising required handles.
     */
    static void loadLib()
        {
        Map<LibraryOption, Object> libraryOptions = new HashMap<>();
        libraryOptions.put(LibraryOption.LoadNow, true); // load immediately instead of lazily (ie on first use)
        libraryOptions.put(LibraryOption.IgnoreError, true); // calls shouldn't save last errno after call -- turn on only during debugging
        libJournal = LibraryLoader.loadLibrary(
                NativeJournal.class,
                libraryOptions,
                LIB_NAME
                );
        if (libJournal == null)
            throw new RuntimeException("required library libsystemd could not be found in library paths. please make sure it is installed.");
        runtime = Runtime.getRuntime(libJournal);
        return;
        }

    /** Constant <code>LIB_NAME="systemd"</code> */
    public static final String LIB_NAME = "systemd"; // the journal library has been merged with the systemd library years ago.
    static final int MILLION = 1000000; // for time unit conversions

    static final Pattern JOURNAL_FIELD_NAME_REGEXP = Pattern.compile("^([\\p{Digit}\\p{Upper}_]{1,255})$"); // 1 to 255 uppercase letters, digits, or underscores
    private static NativeJournal libJournal; // library handle is shared among instances. no locking since this handle is atomic in read and write. @CHECK
    /** Constant <code>runtime</code> */
    protected static Runtime runtime;
    private AddressByReference journalHandle;
    private Address actualJournalHandle;
    private boolean filtersActiveFlag;
    private ByteBuffer reusableByteBuffer; // final, definitely not static
    private final LongLongByReference reusableLongLongByReference = new LongLongByReference();
    private final PointerByReference reusablePointerByReference = new PointerByReference();
    private Charset journalCharset; // static, final
    private ZoneOffset journalTimeZoneOffset = ZoneOffset.UTC; //@TODO add accessor to allow changing this.

}
//___EOF___
