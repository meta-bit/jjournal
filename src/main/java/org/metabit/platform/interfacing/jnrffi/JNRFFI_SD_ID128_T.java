/**
 * (c) metabit 2022.
 *
 * This is dual-licensed under GPL-3 for non-commercial use.
 * For commercial use, obtain an Apache-2.0-style license via (free) registration from metabit.
 * See LICENSE file for details.
 */
package org.metabit.platform.interfacing.jnrffi;

import jnr.ffi.Runtime;
import jnr.ffi.Struct;
import jnr.ffi.Union;

/*
a mapping of #include <systemd/sd-id128.h>
        typedef union sd_id128 {
            uint8_t bytes[16];
            uint64_t qwords[2];
            } sd_id128_t;
 */
@SuppressWarnings({"javadoc","unused"}) // this is an internal interface, javadoc processing not intended
public class JNRFFI_SD_ID128_T extends Union implements Comparable<JNRFFI_SD_ID128_T>
{
    public Struct.u_int8_t bytes[] = new Struct.u_int8_t[16];
    public Struct.u_int64_t qwords[] = new Struct.u_int64_t[2];

    protected JNRFFI_SD_ID128_T(Runtime runtime)
        {
        super(runtime);
        }

    @Override
    public int compareTo(final JNRFFI_SD_ID128_T theOther)
        {
        if (this.bytes == null)
            return Integer.MIN_VALUE;
        if (theOther == null)
            return Integer.MAX_VALUE;
        if (theOther.bytes == null)
            throw new IllegalArgumentException("uninitialised union! should be impossible.");
        for (int i=0; i<16; i++)
            {
            int diff = (theOther.bytes[i].intValue() - this.bytes[i].intValue());
            if (diff != 0)
                return diff;
            }
        return 0;
        }
}
