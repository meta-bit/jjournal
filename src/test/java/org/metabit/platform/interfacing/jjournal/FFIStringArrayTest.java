package org.metabit.platform.interfacing.jjournal;

import jnr.ffi.Memory;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metabit.platform.interfacing.jnrffi.JNRFFISupportFunctions;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.EnumSet;

public class FFIStringArrayTest
{
    static private Runtime runtime;
    static private Journal instance;

    @BeforeAll
    static void initLibraryAndRuntime() throws JJournalException
        {
        // just regular journal with defaults, to get runtime and library from.
        instance = new Journal(EnumSet.of(OpenFlags.LOCAL_ONLY));
        runtime = instance.runtime;
        }

    /*
        this is the test from https://github.com/jnr/jnr-ffi/blob/master/src/test/java/jnr/ffi/PointerTest.java
        it does some black magic without any explanation.
        and it works.
     */
    @Test public void JNRFFITestnullTerminatedStringArray()
        {
        Pointer[] array = new Pointer[10];
        String[] in = new String[array.length]; // same size.
        // fill the pointer array.
        for (int i = 0; i < array.length; i++)
            {
            array[i] = Memory.allocateDirect(runtime, 128); // allocate big empty chunks
            array[i].putString(0, in[i] = Integer.toString(i), 128, Charset.defaultCharset()); // fill in the integers and then put them somehow somewhere
            }
        // allocate buffer, do black magic
        Pointer memory = Memory.allocateDirect(runtime, (2 * array.length + 1) * runtime.addressSize(), true);
        long offset = (long)array.length * runtime.addressSize();
        memory.put(offset, array, 0, array.length);
        // read back and compare
        String[] out = memory.getNullTerminatedStringArray(offset);
        Assertions.assertArrayEquals(in, out);
        }


    @Test public void Test2()
        {
        String[] in = {"this","is","an","real","string","array"};
        Pointer[] array = new Pointer[in.length];

        // fill the pointer array.
        for (int i = 0; i < in.length; i++)
            {
            array[i] = Memory.allocateDirect(runtime, 128); // allocate big empty chunks
            array[i].putString(0, in[i], 128, Charset.defaultCharset()); // fill in the integers and then put them somehow somewhere
            }

        // allocate buffer, do black magic
        Pointer memory = Memory.allocateDirect(runtime, (2 * array.length + 1) * runtime.addressSize(), true);
        memory.put(array.length * runtime.addressSize(), array, 0, array.length);
        // read back and compare
        String[] out = memory.getNullTerminatedStringArray(array.length * runtime.addressSize());
        for (String s:out)
            System.out.println(s);
        Assertions.assertArrayEquals(in, out);
        }

    @Test public void Test3()
        {
        String[] in = {"this","is","an","real","string","array"};
        Pointer[] arrayOfPointers = new Pointer[in.length];

        // fill the pointer array.
        for (int i = 0; i < in.length; i++)
            {
            final String string = in[i];
            final int stringLength = string.length();
            arrayOfPointers[i] = Memory.allocateDirect(runtime, stringLength); // allocate big empty chunks
            arrayOfPointers[i].putString(0, string, stringLength, Charset.defaultCharset()); // fill in the integers and then put them somehow somewhere
            }

        int blackMagicSize = (2 * arrayOfPointers.length + 1) * runtime.addressSize();
        System.out.println("blackMagicSize = " + blackMagicSize);

        // Hypothese: dieser offset ist nur dazu da, offsetteerei zu testen. den braucht man eigentlich gar nicht.
        int blackMagicOffset = arrayOfPointers.length * runtime.addressSize();
        System.out.println("blackMagicOffset = " + blackMagicOffset);
        // und die size ist darum auch überhaupt erst so kompliziert berechnet, damit man den offset dabei hat!
        // eigentlich will man nur einen mehr haben als die Pointes, sind für die null am Ende, und dann die pointer einfach drüberhauen.

        // allocate buffer, do black magic
        Pointer memory = Memory.allocateDirect(runtime, (2 * arrayOfPointers.length + 1) * runtime.addressSize(), true);
        memory.put(blackMagicOffset, arrayOfPointers, 0, arrayOfPointers.length);


        // read back and compare
        String[] out = memory.getNullTerminatedStringArray(blackMagicOffset);
        for (String s:out)
            System.out.println(s);
        Assertions.assertArrayEquals(in, out);
        }

    @Test public void Test4()
        {
        String[] in = {"this","is","an","real","string","array"}; // test input
        final int TARGET_BUFFER_OFFSET = 0;

        // allocate pointer array matching the input.
        Pointer[] arrayOfPointers = new Pointer[in.length];
        // fill the pointer array.
        for (int i = 0; i < in.length; i++)
            {
            final String string = in[i];
            final int stringLength = string.length();
            arrayOfPointers[i] = Memory.allocateDirect(runtime, stringLength); // allocate big empty chunks
            arrayOfPointers[i].putString(0, string, stringLength, Charset.defaultCharset()); // fill in the integers and then put them somehow somewhere
            }


        int bytesRequiredToStorePointersPlusTrailingNullOnThisRuntime = (arrayOfPointers.length + 1) * runtime.addressSize();
        // the trailing clear=true sets our last entry to NULL implicitly
        Pointer memory = Memory.allocateDirect(runtime, (arrayOfPointers.length + 1) * runtime.addressSize(), true);
        memory.put(TARGET_BUFFER_OFFSET, arrayOfPointers, 0, arrayOfPointers.length);


        // read back and compare
        String[] out = memory.getNullTerminatedStringArray(TARGET_BUFFER_OFFSET);
        for (String s:out)
            System.out.println(s);
        Assertions.assertArrayEquals(in, out);
        }

    @Test public void Test5()
        {
        String[] in = {"this","is","an","real","string","array"}; // test input
        final int TARGET_BUFFER_OFFSET = 0;



        Pointer pointer = JNRFFISupportFunctions.generateNullTerminatedStringArray(runtime, Arrays.asList(in), Charset.defaultCharset(),0);

        // read back and compare
        String[] out = pointer.getNullTerminatedStringArray(TARGET_BUFFER_OFFSET);
        for (String s:out)
            System.out.println(s);
        Assertions.assertArrayEquals(in, out);
        }

}
