package org.metabit.platform.interfacing.jjournal;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.metabit.platform.interfacing.jjournal.JJournalException.ErrorCodes.FIELD_EMPTY;

public class JournalFieldTest
{
    // note: if you want to know the actual fields, better use getAvailableFields()
    @Test
    void readAllKnownFields()
        {
        try(Journal journal = new Journal(EnumSet.of(OpenFlags.CURRENT_USER)))
            {
            journal.moveToEarliest();
            assertEquals(journal.skip(1000), 0);
            for (JournalField field : JournalField.values())
                {
                System.out.printf("%-30s: ", field.toString());
                try
                    {
                    final String tmp = journal.readFieldAsString(field);
                    System.out.println(tmp);
                    }
                catch (JJournalException jex)
                    {
                    if (jex.getCode() == FIELD_EMPTY)
                        {
                        System.out.println(" (field not in use)");
                        }
                    else
                        {
                        throw jex;
                        }
                    }
                }
            }
        catch (JJournalException e)
            {
            throw new RuntimeException(e);
            }
        return;
        }


    @Test void readFieldBinaryForComparison() throws JJournalException
        {
        try(Journal journal = new Journal(EnumSet.of(OpenFlags.CURRENT_USER)))
            {
            journal.moveToEarliest();
            journal.moveToNext();
            final byte[] bytes = journal.readFieldAsByteArray(JournalField.MESSAGE);
            String test = new String(bytes, StandardCharsets.US_ASCII);
            System.out.println(test);
            }
        }

    @Test void getAvailableFields() throws JJournalException
        {
        try(Journal journal = new Journal(EnumSet.of(OpenFlags.CURRENT_USER)))
            {
            journal.moveToEarliest();
            journal.moveToNext();
            final EnumSet<JournalField> fields = journal.getAvailableFields();
            assertFalse(fields.isEmpty());
            // System.out.println(fields.toString());
            }
        }

    @Test void getAvailableFieldsAsStringsAndCompare() throws JJournalException
        {
        try(Journal journal = new Journal(EnumSet.of(OpenFlags.CURRENT_USER)))
            {
            journal.moveToEarliest();
            journal.moveToNext();
            final EnumSet<JournalField> fields = journal.getAvailableFields();
            assertFalse(fields.isEmpty());

            final Set<String> fieldNames = journal.getAvailableFieldsAsStrings();
            final Set<String> unmappedFieldNames = new HashSet<>();
            fieldNames.stream().iterator().forEachRemaining(name -> {
                    try
                        {
                        final JournalField field = JournalField.lookup(name);
                        fields.remove(field); // so at the end, it must be empty for all fields found
                        }
                    catch ( IllegalArgumentException ex)
                        {
                        unmappedFieldNames.add(name);
                        }
                    });
            assertTrue(fields.isEmpty());
            System.out.println("field names not mapped to enum: " + unmappedFieldNames.toString());
            return;
            }
        }


    @Test void checkDefinedFieldNamesForValidity() throws JJournalException
        {
        for (JournalField field : JournalField.values())
            {
            System.out.printf("%-30s\n", field.toString());
            assertTrue(JournalField.checkFieldNameValidity(field.getValue()));
            }
        }

    // + test some randomly generated ones.

}
