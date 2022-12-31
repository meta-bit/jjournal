package org.metabit.platform.interfacing.jjournal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Test;

public class BasicLibraryAvailabilityTest
{
 @Test public void testForLibSystemd()
    {
    try
        {
        Journal.loadLib();
        }
    catch (RuntimeException rex)
        {
        System.err.println("tests require libsystemd.so to be available in Java library paths");
        /* LD_LIBRARY_PATH on Linux, DYLD_LIBRARY_PATH on OS X, PATH on Windows were the defaults for a long time.
           Not, it's at least for Linux changed to
                java.library.path=$LD_LIBRARY_PATH:/usr/java/packages/lib/amd64:/usr/lib64:/lib64:/lib:/usr/lib
           I found my instance in /usr/lib/x86_64-linux-gnu/ , so YMMV depending on distro.
         */
        System.err.println("please make sure the test environment has libsystemd0 package or equivalent installed");
        System.err.println(rex);
        Assertions.fail();
        }
    return;
    }
}
