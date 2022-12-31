package org.metabit.platform.interfacing.jjournal;

import jnr.ffi.Runtime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JournalFromFilesTest
{

    public static final String TEST_JOURNAL_FILENAME = "Logreader_Test.journal";
    static private Runtime runtime;
    static private Journal instance;
    private static Path testJournalDirectory;

    @BeforeAll
    static void initLibraryAndRuntime() throws JJournalException
        {
        // just regular journal with defaults, to get runtime and library from.
        instance = new Journal(EnumSet.of(OpenFlags.LOCAL_ONLY));
        runtime = instance.runtime;
        testJournalDirectory = Paths.get("src", "test", "resources", "journals");
        }

    @Test public void checkingForOurTestJournalFile() throws JJournalException
        {
        assertTrue(testJournalDirectory.toFile().exists());
        assertTrue(testJournalDirectory.toFile().canRead());
        assertTrue(testJournalDirectory.toFile().isDirectory());
        String testJournalFilename = TEST_JOURNAL_FILENAME;
        Path testJournalPath = testJournalDirectory.resolve(testJournalFilename);
        assertTrue(testJournalPath.toFile().exists());
        assertTrue(testJournalPath.toFile().canRead());
        assertTrue(testJournalPath.toFile().isFile());
        }

    /** testing with a directory from test-resources folder
     */
    @Test public void journalDirectoryTest() throws JJournalException
        {

        // @TODO try to access the files first here, checking access rights.
        // directory access, using the Path
        try (Journal journal = new Journal(testJournalDirectory))
            {
            journal.moveToEarliest();
            journal.moveToNext();
            journal.moveToNext();
            String cursor = journal.getCursorRaw();
            System.out.println(cursor);
            assertTrue(journal.testCursorRaw(cursor));
            journal.seekCursorRaw(cursor);
            } // implicit .close() here
        return;
        }

    /** testing with a file from test-resources folder
     */
    @Test public void journalFileTest() throws JJournalException
        {
        Path testJournalPath = testJournalDirectory.resolve(TEST_JOURNAL_FILENAME);
        assertTrue(testJournalPath.toFile().exists());
        assertTrue(testJournalPath.toFile().canRead());
        assertTrue(testJournalPath.toFile().isFile());

        String[] stringArray = { testJournalPath.toString() };
        List<String> stringList = Arrays.asList(stringArray);

        try (Journal journal = new Journal(stringList))
            {
            journal.moveToEarliest();
            journal.moveToNext();
            journal.moveToNext();
            String cursor = journal.getCursorRaw();
            System.out.println(cursor);
            assertTrue(journal.testCursorRaw(cursor));
            journal.seekCursorRaw(cursor);
            } // implicit .close() here
        return;
        }

}
