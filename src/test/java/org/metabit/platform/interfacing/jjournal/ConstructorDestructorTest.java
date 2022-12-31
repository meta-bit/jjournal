package org.metabit.platform.interfacing.jjournal;

import org.junit.jupiter.api.Test;

public class ConstructorDestructorTest
{
    @Test
    void testVarargOpenNone() throws JJournalException
        {
        // empty varargs
        final Journal lib = new Journal();
        lib.close();
        }

    @Test
    void testVarargOpenOne() throws JJournalException
        {
        // empty varargs
        final Journal lib = new Journal(OpenFlags.ALL_NAMESPACES);
        lib.close();
        }

    @Test
    void testVarargOpenTwo() throws JJournalException
        {
        // empty varargs
        final Journal lib = new Journal(OpenFlags.SYSTEM, OpenFlags.CURRENT_USER);
        lib.close();
        }

    @Test
    void testVarargOpenThree() throws JJournalException
        {
        // empty varargs
        final Journal lib = new Journal(OpenFlags.SYSTEM, OpenFlags.CURRENT_USER, OpenFlags.RUNTIME_ONLY);
        lib.close();
        }

}
