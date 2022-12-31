package org.metabit.platform.interfacing.jjournal;
/**
 * Wakeup event types.
 * These entries exist because they're part of the sd-journal.h API. Not in current use.
 *
 * The SD_JOURNAL_ prefix is omitted here, because the enum provides the namespace.
 *
 * @author jwilkes
 * @version $Id: $Id
 */
public enum WakeupEvent
{
    /**
     * add description if you happen to know what's the meaning. These entries exist because they're part of the sd-journal.h API.
     */
    NOP,
    /**
     * add description if you happen to know what's the meaning. These entries exist because they're part of the sd-journal.h API.
     */
    APPEND,
    /**
     * add description if you happen to know what's the meaning. These entries exist because they're part of the sd-journal.h API.
     */
    INVALIDATE;
}
//___EOF___
