package org.metabit.platform.interfacing.jjournal;

import jnr.ffi.Runtime;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JournalFilteringTest
{
    public static final String TEST_JOURNAL_FILENAME = "Logreader_Test.journal";
    static private Runtime runtime;
    static private Journal instance;
    private static Path testJournalDirectory;
    private static Path testJournalPath;

    @BeforeAll
    static void initLibraryAndRuntime() throws JJournalException
        {
        // just regular journal with defaults, to get runtime and library from.
        instance = new Journal(EnumSet.of(OpenFlags.LOCAL_ONLY));
        runtime = instance.runtime;
        testJournalDirectory = Paths.get("src", "test", "resources", "journals");
        testJournalPath = testJournalDirectory.resolve(TEST_JOURNAL_FILENAME);
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


    /** testing with a file from test-resources folder
     */
    @Test public void journalFileTest() throws JJournalException
        {
        String[] stringArray = { testJournalPath.toString() };
        List<String> stringList = Arrays.asList(stringArray);
        try (Journal journal = new Journal(stringList))
            {
            journal.moveToEarliest();

            Map<String,String> contents = new HashMap<>();
            final List<String> fieldsToRead = Arrays.asList(JournalField.MESSAGE.getValue(), JournalField.PROCESS_ID.getValue(), JournalField.PROCESS_COMMAND.getValue() /*JournalField.ERRNO.getValue(), JournalField.NAMESPACE.getValue(), JournalField.CODE_FILE.getValue(), JournalField.CODE_FUNC.getValue(), JournalField.CODE_LINE.getValue()*/); // java9 up also: List.of( .... )

            // some unfiltered content
            for (int i=0; i<10; i++)
                {
                journal.moveToNext();

                journal.readFieldsAsStrings(fieldsToRead, contents);
                contents.values().stream().forEach(System.out::print);
                System.out.println();
                }

            System.out.println("---------------------------------------------");
            journal.filteringReset();
            journal.filteringAddFilterExpressionExactMatch(JournalField.PROCESS_ID.getValue(), "4176394");

            journal.moveToEarliest();
            for (int i=0; i<10; i++)
                {
                journal.moveToNext();

                journal.readFieldsAsStrings(fieldsToRead, contents);
                contents.values().stream().forEach(System.out::print);
                System.out.println();
                }
            journal.filteringReset();

            } // implicit .close() here
        return;
        }

}
