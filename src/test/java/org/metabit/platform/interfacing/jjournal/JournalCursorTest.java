package org.metabit.platform.interfacing.jjournal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JournalCursorTest
{

    @Test
    void testCursorRaw() throws JJournalException
        {
        try (Journal journal = new Journal(EnumSet.of(OpenFlags.CURRENT_USER)))
            {
            journal.moveToEarliest();
            journal.moveToNext();
            journal.moveToNext();
            String cursor = journal.getCursorRaw();
            System.out.println(cursor);
            assertTrue(journal.testCursorRaw(cursor));
            journal.seekCursorRaw(cursor);
            // so, why can we do a clean seek, but get an -99 error after doing so?
            // https://www.man7.org/linux/man-pages/man3/sd_journal_test_cursor.3.html
            final IllegalArgumentException thrown = Assertions.assertThrows(IllegalArgumentException.class, () ->
                {
                cursor.replace("a","!!!");
                journal.seekCursorRaw(cursor);
                journal.testCursorRaw(cursor);
                Assertions.fail("this should've thrown an exception");
                });

            journal.skip(28);
            boolean ret = journal.seekCursor(cursor);
            assertTrue(ret);
            }
        return;
        }
}
