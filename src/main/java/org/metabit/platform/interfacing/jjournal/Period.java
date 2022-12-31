package org.metabit.platform.interfacing.jjournal;

import java.time.Instant;

/**
 * this is an actual Period: from Instant, until Instant.
 * java.time. has a misnamed class of the same name which is just another Duration (time vector).
 *
 * @author jwilkes
 * @version $Id: $Id
 */
public class Period
{
    private final Instant from;
    private final Instant until;

    /**
     * construct a time range / time period from java Instant values
     *
     * @param instantFrom  starting time
     * @param instantUntil ending time
     */
    public Period(Instant instantFrom, Instant instantUntil)
        {
        this.from = instantFrom;
        this.until = instantUntil;
        }

    /**
     * construct a time range / time period from system tick values
     *
     * @param microsecondFrom  starting time
     * @param microsecondUntil ending time
     */
    public Period(long microsecondFrom, long microsecondUntil)
        {
        long seconds = microsecondFrom/Journal.MILLION; // the seconds part.
        long nanoseconds = microsecondFrom%Journal.MILLION*1000; // the microseconds, then scaled to nanoseconds
        this.from = Instant.ofEpochSecond(seconds, nanoseconds);
        seconds = microsecondUntil/Journal.MILLION; // the seconds part.
        nanoseconds = microsecondUntil%Journal.MILLION*1000; // the microseconds, then scaled to nanoseconds
        this.until = Instant.ofEpochSecond(seconds, nanoseconds);
        }

    /**
     * get starting time.
     *
     * @return java Instant containing the starting time.
     */
    public Instant getFrom() { return this.from; }

    /**
     * get ending time.
     *
     * @return java Instant containing the ending  time.
     */
    public Instant getUntil() { return this.until; }
}
//___EOF___
