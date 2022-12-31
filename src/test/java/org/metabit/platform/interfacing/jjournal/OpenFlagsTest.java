package org.metabit.platform.interfacing.jjournal;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class OpenFlagsTest
{

    @Test
    void getCombinedFlagValue()
        {
        int test1 = OpenFlags.getCombinedFlagValue(EnumSet.of(OpenFlags.OS_ROOT));
        assertEquals(test1, OpenFlags.OS_ROOT.getValue());

        int test2 = OpenFlags.getCombinedFlagValue(EnumSet.of(OpenFlags.INCLUDE_DEFAULT_NAMESPACE));
        assertEquals(test2, OpenFlags.INCLUDE_DEFAULT_NAMESPACE.getValue());

        int test3 = OpenFlags.getCombinedFlagValue(EnumSet.range(OpenFlags.LOCAL_ONLY, OpenFlags.INCLUDE_DEFAULT_NAMESPACE)); // that should be bits 0,1,2,3,4,5,6
        assertEquals(test3, 0x7F);
        }

}