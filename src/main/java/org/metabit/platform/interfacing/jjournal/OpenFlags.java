package org.metabit.platform.interfacing.jjournal;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *  flags for opening the Journal access. These are intended to be used in an EnumSet - multiple of these can be combined.
 *  see https://www.freedesktop.org/software/systemd/man/sd_journal_open.html#
 *
 *  the SD_JOURNAL_ prefix is omitted here, because the enum provides the namespace.
 *
 * @author jwilkes
 * @version $Id: $Id
 */
public enum OpenFlags
{
    /**
     *  open only journal files generated on the local machine, no remote ones.
     */
    LOCAL_ONLY(1 << 0),
    /**
     * only volatile journal files will be opened, excluding those which are stored on persistent storage.
     */
    RUNTIME_ONLY(1 << 1),
    /**
     * open journals of system services and the kernel (as opposed to user session processes).
     * If neither SD_JOURNAL_SYSTEM nor SD_JOURNAL_CURRENT_USER are specified, all journal file types will be opened.
     */
    SYSTEM(1 << 2),
    /**
     * open journal files of the current user.
     * If neither SD_JOURNAL_SYSTEM nor SD_JOURNAL_CURRENT_USER are specified, all journal file types will be opened.
     */
    CURRENT_USER(1 << 3),
    /**
     *  journal files are searched for below the usual /var/log/journal and /run/log/journal relative to the specified path, instead of directly beneath it.
     */
    OS_ROOT(1 << 4),
    /**
     * Show all namespaces, not just the default or specified one
     */
    ALL_NAMESPACES(1 << 5),
    /**
     * Show default namespace in addition to specified one
     */
    INCLUDE_DEFAULT_NAMESPACE(1 << 6);

    private final int value;
    OpenFlags(int value)
        {
        this.value = value;
        }

    /**
     * get the numerical value of the flag.
     *
     * @return the numerical bit value of the flag.
     */
    public int getValue()
        {
        return value;
        }

    /**
     * bitwise combine flag values to an int.
     *
     * @param flagset the set of flags to combine
     * @return integer or-ing the numerical values of the set flag bits.
     */
    public static int getCombinedFlagValue(EnumSet<OpenFlags> flagset)
        {
        //@IMPROVEMENT replace by simpler construct
        final AtomicInteger flagValue = new AtomicInteger(); // I think this overkill, but the forEachRemaining() construct seems to need this...
        flagset.iterator().forEachRemaining( openFlags -> flagValue.updateAndGet(v -> v|openFlags.getValue()) );
        return flagValue.intValue();
        }
}
//___EOF___
