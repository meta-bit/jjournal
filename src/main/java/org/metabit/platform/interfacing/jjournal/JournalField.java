package org.metabit.platform.interfacing.jjournal;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.metabit.platform.interfacing.jjournal.JournalField.DataType.*;

/**
 * class for predefined, standard Journald-fieldnames.
 * the string value needs to be the precise character sequence of the respective field.
 * The enum name may be different, improving on abbreviations or the like.
 * <p>
 * see https://www.man7.org/linux/man-pages/man7/systemd.journal-fields.7.html for the man pages
 *
 * @author jwilkes
 * @version $Id: $Id
 */
public enum JournalField
{
    /**
     * The human-readable message string for this entry. This is
     * supposed to be the primary text shown to the user. It is
     * usually not translated (but might be in some cases), and is
     * not supposed to be parsed for metadata.
     */
    MESSAGE("MESSAGE", STRING),
    /**
     * A 128-bit message identifier ID for recognizing certain
     * message types, if this is desirable. This should contain a
     * 128-bit ID formatted as a lower-case hexadecimal string,
     * without any separating dashes or suchlike. This is
     * recommended to be a UUID-compatible ID, but this is not
     * enforced, and formatted differently. Developers can generate
     * a new ID for this purpose with systemd-id128 new.
     */
    MESSAGE_ID("MESSAGE_ID", STRING), // check
    /**
     * A priority value between 0 ("emerg") and 7 ("debug")
     * formatted as a decimal string. This field is compatible with
     * syslog's priority concept.
     */
    PRIORITY("PRIORITY", STRING),

    /**
     * The code location generating this message, if known. Contains
     * the source filename, the line number and the function name.
     */
    CODE_FILE("CODE_FILE", STRING),
    CODE_LINE("CODE_LINE", STRING),
    CODE_FUNC("CODE_FUNC", STRING),

    /**
     * The low-level Unix error number causing this entry, if any.
     * Contains the numeric value of errno(3) formatted as a decimal
     * string.
     */
    ERRNO("ERRNO", STRING),

    /**
     * A randomized, unique 128-bit ID identifying each runtime
     * cycle of the unit. This is different from
     * _SYSTEMD_INVOCATION_ID in that it is only used for messages
     * coming from systemd code (e.g. logs from the system/user
     * manager or from forked processes performing systemd-related
     * setup).
     */
    INVOCATION_ID("INVOCATION_ID", STRING),
    USER_INVOCATION_ID("USER_INVOCATION_ID", STRING),


    /**
     * Syslog compatibility fields containing the facility
     * (formatted as decimal string), the identifier string (i.e.
     * "tag"), the client PID, and the timestamp as specified in the
     * original datagram. (Note that the tag is usually derived from
     * glibc's program_invocation_short_name variable, see
     * program_invocation_short_name(3).)
     * <p>
     * Note that the journal service does not validate the values of
     * any structured journal fields whose name is not prefixed with
     * an underscore, and this includes any syslog related fields
     * such as these. Hence, applications that supply a facility,
     * PID, or log level are expected to do so properly formatted,
     * i.e. as numeric integers formatted as decimal strings.
     */
    SYSLOG_FACILITY("SYSLOG_FACILITY", STRING),
    SYSLOG_IDENTIFIER("SYSLOG_IDENTIFIER", STRING),
    SYSLOG_PID("SYSLOG_PID", STRING),
    SYSLOG_TIMESTAMP("SYSLOG_TIMESTAMP", STRING),


    /**
     * The original contents of the syslog line as received in the
     * syslog datagram. This field is only included if the MESSAGE=
     * field was modified compared to the original payload or the
     * timestamp could not be located properly and is not included
     * in SYSLOG_TIMESTAMP=. Message truncation occurs when when the
     * message contains leading or trailing whitespace (trailing and
     * leading whitespace is stripped), or it contains an embedded
     * NUL byte (the NUL byte and anything after it is not
     * included). Thus, the original syslog line is either stored as
     * SYSLOG_RAW= or it can be recreated based on the stored
     * priority and facility, timestamp, identifier, and the message
     * payload in MESSAGE=.
     */
    SYSLOG_RAW("SYSLOG_RAW", BINARY),

    /**
     * A documentation URL with further information about the topic
     * of the log message. Tools such as journalctl will include a
     * hyperlink to an URL specified this way in their output.
     * Should be a "http://", "https://", "file:/", "man:" or
     * "info:" URL.
     */

    DOCUMENTATION("DOCUMENTATION", STRING),

    /**
     * The numeric thread ID (TID) the log message originates from.
     */
    THREAD_ID("TID", STRING), // or NUMERICAL?

    /**
     * The so-called "trusted journal fields" are those added automatically by the journal system.
     * They cannot be altered by client code, and so they are considered "trusted".
     * Their field names start with an underscore.
     * <p>
     * trusted fields (implicitly added by the journald)
     * <p>
     * The process, user, and group ID of the process the journal
     * entry originates from formatted as a decimal string. Note
     * that entries obtained via "stdout" or "stderr" of forked
     * processes will contain credentials valid for a parent process
     * (that initiated the connection to systemd-journald).
     */
    PROCESS_ID("_PID", STRING),
    USER_ID("_UID", STRING),
    GROUP_ID("_GID", STRING),

    /**
     * trusted fields (implicitly added by the journald)
     * <p>
     * of the process that caused this journal entry, its
     * process executable name
     * process executable path
     * process executable full command line
     */
    PROCESS_COMMAND("_COMM", STRING),
    PROCESS_EXECUTABLE_WITH_PATH("_EXE", STRING),
    PROCESS_COMMAND_LINE("_CMDLINE", STRING),

    /** The effective capabilities(7) of the process the journal entry originates from. */
    PROCESS_EFFECTIVE_CAPABILITIES("_CAP_EFFECTIVE", STRING),

    /**
     * The session and login UID of the process the journal entry
     * originates from, as maintained by the kernel audit subsystem.
     */
    AUDIT_SESSION("_AUDIT_SESSION", STRING),
    AUDIT_LOGINUID("_AUDIT_LOGINUID", UID),

    /**
     * The control group path in the systemd hierarchy, the systemd
     * slice unit name, the systemd unit name, the unit name in the
     * systemd user manager (if any), the systemd session ID (if
     * any), and the owner UID of the systemd user unit or systemd
     * session (if any) of the process the journal entry originates
     * from.
     */
    SYSTEMD_CGROUP("_SYSTEMD_CGROUP", STRING),
    SYSTEMD_SLICE("_SYSTEMD_SLICE", STRING),
    SYSTEMD_UNIT("_SYSTEMD_UNIT", STRING),
    SYSTEMD_USER_UNIT("_SYSTEMD_USER_UNIT", STRING),
    SYSTEMD_USER_SLICE("_SYSTEMD_USER_SLICE", STRING),
    SYSTEMD_SESSION("_SYSTEMD_SESSION", STRING),
    SYSTEMD_OWNER_UID("_SYSTEMD_OWNER_UID", UID),

    /** The SELinux security context (label) of the process the journal entry originates from. */
    SELINUX_CONTEXT("_SELINUX_CONTEXT", STRING),

    /**
     * The earliest trusted timestamp of the message, if any is
     * known that is different from the reception time of the
     * journal. This is the time in microseconds since the epoch
     * UTC, formatted as a decimal string.
     */
    SOURCE_REALTIME_TIMESTAMP("_SOURCE_REALTIME_TIMESTAMP", STRING),

    /** The kernel boot ID for the boot the message was generated in, formatted as a 128-bit hexadecimal string. */
    BOOT_ID("_BOOT_ID", STRING),  // UUID or special?

    /** The machine ID of the originating host, as available in machine-id(5). */
    MACHINE_ID("_MACHINE_ID", STRING), //@TODO UUID?
    /** the systemd invocation ID, see systemd.exec(5)  */
    SYSTEMD_INVOCATION_ID("_SYSTEMD_INVOCATION_ID", STRING), // UUID? Numerical?

    /** The name of the originating host. */
    HOSTNAME("_HOSTNAME", STRING),

    /**
     * How the entry was received by the journal service. Valid
     * transports are:
     * audit  - for those read from the kernel audit subsystem
     * driver - for internally generated messages
     * syslog - for those received via the local syslog socket with the syslog protocol
     * journal - for those received via the native journal protocol
     * stdout - for those read from a service's standard output or error output
     * kernel - for those read from the kernel
     **/
    TRANSPORT("_TRANSPORT", STRING),

    /**
     * the "stream ID" applies only to stdout _TRANSPORT record.
     * it is a random 128 bit ID of the stream connection.
     * This may be useful to reconstruct streams; all records
     * with the same streamID originate from the same stream.
     */
    STDOUT_STREAM_ID("_STREAM_ID", STRING),

    /**
     * JW
     * only valid for transport "stdout", hence the name.
     * when the log message in the output / error stream was not terminated with an normal
     * "\n" = ASCII 10 newline, this tells us what else was used.
     * when the normal newline is used, this record is not set.
     * defined values:
     * * "nul" when the line was terminated with ASCII NUL byte (0x00).
     * * "line-max" when the maximum line length (as configuredwith LineMax= in journald.conf(5) ) was reached.
     * * "eof" when the stream ended without a final newline character.
     * * "pid-change" when the process sending the output changed in the middle of a line / before line end.
     */
    STDOUT_LINE_BREAK("_LINE_BREAK", STRING),

    /**
     * when a non-default journal namespace is used, this contains the namespace identifier.
     * See systemd-journald.service(8) about namespaces.
     */
    NAMESPACE("_NAMESPACE", STRING),

    //-------------------------------------------------------------------------------------------------------------------
    // fields used in the kernel journal
    /*            The kernel device name. If the entry is associated to a block
           device, contains the major and minor numbers of the device
           node, separated by ":" and prefixed by "b". Similarly for
           character devices, but prefixed by "c". For network devices,
           this is the interface index prefixed by "n". For all other
           devices, this is the subsystem name prefixed by "+", followed
           by ":", followed by the kernel device name.
*/
    KERNEL_DEVICE("_KERNEL_DEVICE", STRING),
    KERNEL_SUBSYSTEM("_KERNEL_SUBSYSTEM", STRING),
    /* The kernel device name as it shows up in the device tree
    below /sys/.*/
    KERNEL_UDEV_SYSNAME("_UDEV_SYSNAME", STRING),
    /* The device node path of this device in /dev/. */
    KERNEL_UDEV_DEVNOCE("_UDEV_DEVNODE", STRING),
    /* Additional symlink names pointing to the device node in /dev/. This field is frequently set more than once per entry. */
    KERNEL_UDEV_DEVLINK("_UDEV_DEVLINK", STRING),
    //-------------------------------------------------------------------------------------------------------------------
    /**
     * see coredumpctl(1) for how the systemd-coredump kernel helper uses these.
     */
    COREDUMP_UNIT("COREDUMP_UNIT", STRING),
    COREDUMP_USER_UNIT("COREDUMP_USER_UNIT", STRING),

    //-------------------------------------------------------------------------------------------------------------------
    /**
     * these fields may be attached by privileged programs (e.g. UID0)
     * to provide additionl information about where journal entries originated from
     */
    OBJECT_PID("OBJECT_PID", STRING),
    OBJECT_UID("OBJECT_UID", STRING),
    OBJECT_GID("OBJECT_GID", STRING),
    OBJECT_COMM("OBJECT_COMM", STRING),
    OBJECT_EXE("OBJECT_EXE", STRING),
    OBJECT_CMDLINE("OBJECT_CMDLINE", STRING),
    OBJECT_AUDIT_SESSION("OBJECT_AUDIT_SESSION", STRING),
    OBJECT_AUDIT_LOGINUID("OBJECT_AUDIT_LOGINUID", STRING),
    OBJECT_SYSTEMD_CGROUP("OBJECT_SYSTEMD_CGROUP", STRING),
    OBJECT_SYSTEMD_SESSION("OBJECT_SYSTEMD_SESSION", STRING),
    OBJECT_SYSTEMD_OWNER_UID("OBJECT_SYSTEMD_OWNER_UID", STRING),
    OBJECT_SYSTEMD_UNIT("OBJECT_SYSTEMD_UNIT", STRING),
    OBJECT_SYSTEMD_USER_UNIT("OBJECT_SYSTEMD_USER_UNIT", STRING);

    //----------------------------------------------------------

    /**
     * from man sd_journal_send:
     * "The variable name must be in uppercase and consist only of characters, numbers and underscores, and may not begin with an underscore."
     * but for reading, the underscore rule is not valid; there are many automatically generated fields starting with an underscore.
     * (It's reserved to them, that's the reason for above restriction.)
     */
    public static final String JOURNAL_FIELD_NAME_VALIDITY_PATTERN = "^[0-9A-Z_]+$";
    private static final Pattern validFieldNameCheck = Pattern.compile(JOURNAL_FIELD_NAME_VALIDITY_PATTERN);

    enum DataType
    {STRING, BINARY, UID}

    ; // possible improvements: NUMBER / NUMERICAL, UUID?

    private final DataType type;
    private final String value;


    JournalField(final String value, DataType type)
        {
        this.value = value;
        this.type = type;
        }

    /**
     * get the actual string value of the field.
     *
     * @return the JournalD field name as string (not the enum name).
     */
    public String getValue()
        {
        return value;
        }

    /**
     * get the type of the field.
     *
     * @return type, as enum local to this.
     */
    public DataType getType()
        {
        return type;
        }

    /**
     * lookup a field by provided journal field name.
     * use this, not valueOf() to map strings back to enums
     *
     * @param fieldName as provided by journal
     * @return field enum
     * @throws java.lang.IllegalArgumentException if the string provided does not have a corresponding field enum entry.
     */
    public static JournalField lookup(final String fieldName)
        {
        final JournalField tmp = reverseMap.get(fieldName); // may return NULL if mapping fails.
        if (null == tmp)
            {
            throw new IllegalArgumentException("not a predefined JournalField name: "+fieldName);
            }
        return tmp;
        }

    private final static Map<String, JournalField> reverseMap;

    // static map init - done once at startup
    static
        {
        reverseMap = new HashMap<>(JournalField.values().length);
        for (JournalField field : JournalField.values())
            {
            reverseMap.put(field.getValue(), field);
            }
        }

    /**
     * <p>checkFieldNameValidity.</p>
     *
     * @param stringToTest a {@link java.lang.String} object
     * @return a boolean
     */
    public static boolean checkFieldNameValidity(final String stringToTest)
        {
        return validFieldNameCheck.matcher(stringToTest).matches();
        }

}
//___EOF___
