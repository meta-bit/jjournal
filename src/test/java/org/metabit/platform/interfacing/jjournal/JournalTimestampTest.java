package org.metabit.platform.interfacing.jjournal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class JournalTimestampTest
{
    @Test
    public void testTimegettingInstants() throws JJournalException
        {
        try (Journal journal = new Journal(EnumSet.of(OpenFlags.CURRENT_USER)))
            {
            journal.moveToEarliest();
            journal.skip(10);

            Instant timestamp;
            for (int i = 0; i < 20; i++)
                {
                timestamp = journal.getTimestampAsInstant();
                System.out.println(timestamp);
                assertTrue(journal.moveToNext());
                }
            }
        return;
        }
 // @TODO test and compare  getTimestampAsMicrosecondsLong() with the Instant() values from the other function.

    @Test
    public void testTimerangeConsumer() throws JJournalException
        {
        // tricky to test, because we have to find a valid timerange first in our ever-changing test journal.
        try (Journal journal = new Journal(EnumSet.of(OpenFlags.CURRENT_USER)))
            {
            journal.moveToEarliest();
            journal.skip(10);

            Instant start = journal.getTimestampAsInstant();
            journal.skip(100); // assuming we have 110 entries at least...
            Instant end = journal.getTimestampAsInstant();

            journal.moveToEarliest();
            assertTrue(journal.moveForwardUntilTime(start));

            journal.moveToEarliest();
            assertTrue(journal.moveForwardUntilTime(end));

            // --- now for the actual test ---
            journal.moveToEarliest();
            printMessagesFromTimeRange(journal, start,end);
            }
        return;
        }

    @Test
    public void printMostRecentFiveMinutes() throws JJournalException
        {
        Instant now = Instant.now();
        Instant fiveMinutesAgo = now.minusSeconds(300);
        try (Journal journal = new Journal(EnumSet.of(OpenFlags.CURRENT_USER)))
            {
            // it's possible we didn't have any entries in the most recent five minutes; so for our automated test, let's test that first.
            journal.moveToEarliest();
            if (journal.moveForwardUntilTime(fiveMinutesAgo) == true)
                {
                // this should run fine and print some entries
                printMessagesFromTimeRange(journal, fiveMinutesAgo, now);
                }
            else
                {
                // expect exception when doing the loop
                final JJournalException thrown = Assertions.assertThrows(JJournalException.class, () ->
                         {
                         printMessagesFromTimeRange(journal, fiveMinutesAgo, now);
                         });
                }
            }
        }

    private void printMessagesFromTimeRange(Journal journal, Instant since, Instant until) throws JJournalException
        {
        journal.foreachInTimerange(since, until, 100, jj ->
            {
            final String msg = jj.readMessageField();
            System.out.println(msg);
            });
        }

}