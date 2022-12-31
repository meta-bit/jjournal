/**
 * (c) metabit 2022.
 *
 * This is dual-licensed under GPL-3 for non-commercial use.
 * For commercial use, obtain an Apache-2.0-style license via (free) registration from metabit.
 * See LICENSE file for details.
 */
package org.metabit.platform.interfacing.jnrffi;

import jnr.ffi.Runtime;
import jnr.ffi.Memory;
import jnr.ffi.Pointer;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

/**
 * supplying missing functions to JNR FFI
 *
 * @author jwilkes
 * @version $Id: $Id
 */
public class JNRFFISupportFunctions
{
// this should be a regular JNR FFI helper function!

    /**
     * encode strings for passing to a function which expects an NULL-terminated array of strings.
     *
     * @param runtime the library runtime handle, used for allocation and size calculation
     * @param inputStrings the strings to be passed
     * @param inputEncoding the encoding to be used (e.g. Charset.defaultCharset() ).
     * @param targetBufferOffset offset to use; keep 0 for most cases.
     * @return FFI Pointer for passing.
     *
     * this is the counterpart to the existing function Pointer.getNullTerminatedStringArray(0);
     * Parameter checking is expected to be performed by the calling function.
     */
    public static Pointer generateNullTerminatedStringArray(Runtime runtime, List<String> inputStrings, final Charset inputEncoding, int targetBufferOffset)
        {
        int numEntries = inputStrings.size();
        // allocate pointer array matching the input.
        Pointer[] arrayOfPointers = new Pointer[numEntries];
        // fill the pointer array.
        Iterator<String> it = inputStrings.iterator();
        for (int i=0; (i<numEntries)&&(it.hasNext()); i++)
            {
            final String currentString = it.next();
            final int stringLength = currentString.length();
            arrayOfPointers[i] = Memory.allocateDirect(runtime, stringLength);
            arrayOfPointers[i].putString(0, currentString, stringLength, inputEncoding);
            }
        // calculate memory required for the actual data
        int bytesRequiredToStorePointersPlusTrailingNullOnThisRuntime = (arrayOfPointers.length + 1) * runtime.addressSize();
        // allocate memory. The trailing clear=true flag sets our last entry to NULL implicitly.
        Pointer memory = Memory.allocateDirect(runtime, bytesRequiredToStorePointersPlusTrailingNullOnThisRuntime, true);
        // copy/convert all our prepared pointers into the memory buffer
        memory.put(targetBufferOffset, arrayOfPointers, 0, arrayOfPointers.length);
        // further conversion of the pointers themselves is performed by the lower layers - as should the reference handling for the strings referenced via the pointers!
        return memory;
        }

}
