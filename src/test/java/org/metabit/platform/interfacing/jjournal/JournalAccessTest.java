package org.metabit.platform.interfacing.jjournal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

public class JournalAccessTest
{
 @Test void openAndCloseCurrentUser() throws JJournalException
     {
     Journal journal;

     journal = new Journal(EnumSet.of(OpenFlags.CURRENT_USER));
     //...
     journal.close();

     // try-with-resource has implicit close
     try(Journal journal2 = new Journal(EnumSet.of(OpenFlags.CURRENT_USER)))
        {
        }
     catch (JJournalException e)
         {
         throw new RuntimeException(e);
         }
     return;
     }

    @Test void openAndCloseVariants() throws JJournalException
        {
        // try-with-resource has implicit close
        try(Journal journal = new Journal(EnumSet.of(OpenFlags.CURRENT_USER)))
            {
            }
        try(Journal journal = new Journal(EnumSet.of(OpenFlags.LOCAL_ONLY)))
            {
            }
        try(Journal journal = new Journal(EnumSet.of(OpenFlags.INCLUDE_DEFAULT_NAMESPACE)))
            {
            }
        try(Journal journal = new Journal(EnumSet.of(OpenFlags.SYSTEM)))
            {
            }
        try(Journal journal = new Journal(EnumSet.of(OpenFlags.RUNTIME_ONLY)))
            {
            }
        return;
        }


    @Test void openCurrentIterateToEnd() throws JJournalException
        {
        int count = 0;
        // try-with-resource has implicit close
        try(Journal journal = new Journal(EnumSet.of(OpenFlags.CURRENT_USER)))
            {
            journal.moveToEarliest();
            while (journal.moveToNext())
                {
                count++;
                }
            }
        System.out.println(count);
        Assertions.assertTrue(count > 0); // empty journal for current user is implausible.
        return;
        }


    @Test void sipToEnd() throws JJournalException
        {
        int count = 0;
        // try-with-resource has implicit close
        try(Journal journal = new Journal(EnumSet.of(OpenFlags.CURRENT_USER)))
            {
            journal.moveToEarliest();
            while (journal.skip(100) == 0)
                {
                count++;
                }
            }
        System.out.println(count);
        Assertions.assertTrue(count > 0); // empty journal for current user is implausible.
        return;
        }

    @Test void skipBackwardsFromEndToStart() throws JJournalException
        {
        int count = 0;
        // try-with-resource has implicit close
        try(Journal journal = new Journal(EnumSet.of(OpenFlags.CURRENT_USER)))
            {
            journal.moveToLatest();
            while (journal.skip(-100) == 0)
                {
                count++;
                }
            }
        System.out.println(count);
        Assertions.assertTrue(count > 0); // empty journal for current user is implausible.
        return;
        }

    @Test void findLongestMessageLength() throws JJournalException
        {
        int max = 0;
        try(Journal journal = new Journal(EnumSet.of(OpenFlags.CURRENT_USER)))
            {
            journal.moveToEarliest();
            while (journal.moveToNext())
                {
                int num = journal.getDataSize(JournalField.MESSAGE.getValue());
                if (num > max)
                    max = num;
                }
            }
        System.out.println("longest message length: " + max);
        return;
        }

    @Test void intentionallyReadingInvalidEntries() throws JJournalException
        {
        try(Journal journal = new Journal(EnumSet.of(OpenFlags.CURRENT_USER)))
            {
            journal.moveToEarliest();
            final JJournalException thrown = Assertions.assertThrows(JJournalException.class, () ->
                        {
                        int trigger = journal.getDataSize(JournalField.MESSAGE.getValue());
                        Assertions.fail("this should've thrown an exception");
                        });
            System.err.println(thrown.toString());

            }
        return;
        }

    @Test void readSomeMessages() throws JJournalException
        {
        try(Journal journal = new Journal(EnumSet.of(OpenFlags.CURRENT_USER)))
            {
            journal.moveToEarliest();
            while (journal.skip(100) == 0)
                {
                final String tmp = journal.readFieldAsString(JournalField.MESSAGE);
//                System.out.println(tmp);
                }
            }
        return;
        }


}
