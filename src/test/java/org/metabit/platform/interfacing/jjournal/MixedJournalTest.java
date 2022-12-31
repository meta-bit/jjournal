package org.metabit.platform.interfacing.jjournal;

import org.junit.jupiter.api.Test;
import java.util.EnumSet;

class MixedJournalTest
{
 @Test
 public void howManyBytesUsed() throws JJournalException
     {
     try (Journal journal = new Journal(EnumSet.of(OpenFlags.CURRENT_USER)))
         {
         long bytesUsed = journal.getBytesUsedByJournalFiles();
        System.out.println(bytesUsed + " bytes used by systemd journal files");
         }
     return;
     }

    @Test
    public void checkForRuntimeFiles() throws JJournalException
        {
        try (Journal journal = new Journal(EnumSet.of(OpenFlags.CURRENT_USER)))
            {
            boolean flag = journal.hasRuntimeFiles();
            System.out.println("has runtime files? " + flag);
            }
        return;
        }

    @Test
    public void checkForPersistentFiles() throws JJournalException
        {
        try (Journal journal = new Journal(EnumSet.of(OpenFlags.CURRENT_USER)))
            {
            boolean flag = journal.hasPersistentFiles();
            System.out.println("has persistent files? " + flag);
            }
        return;
        }

}