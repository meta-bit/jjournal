package org.metabit.platform.interfacing.jjournal;

import jnr.ffi.*;

import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import jnr.ffi.byref.AddressByReference;
import jnr.ffi.byref.IntByReference;
import jnr.ffi.byref.PointerByReference;


class BasicSdJournaltest
{

    // need stdlib::strerror to print it
static String getErrorString(int ret)
    {
    switch (-ret)
        {
        case 1: return "Operation not permitted";
        case 10: return "No child processes"; // wtf?
        case 99: return "cannot assign requested address";
        default:
            return "std::strerror() for " + (-ret);
        }
    }

    public static void main(String[] args)
        {
        Map<LibraryOption, Object> libraryOptions = new HashMap<>();
        libraryOptions.put(LibraryOption.LoadNow, true); // load immediately instead of lazily (ie on first use)
        libraryOptions.put(LibraryOption.IgnoreError, true); // calls shouldn't save last errno after call
        String libName = "systemd";

        NativeJournal libJournal = LibraryLoader.loadLibrary(
                NativeJournal.class,
                libraryOptions,
                libName
                );


        // lesson learned:
        // 1. die namen im NativeJournal müssen exakt den funktionsnamen der C -lib entsprechen.
        // macht man das, bekommt man sofort statt -1 einen anderen fehlercode... beispielsweise
        // -22    = TESTING JNR ACCESS: No such file or directory
        // sichtbar mittels "journalctl -f" in normaler shell.
        // 2. man kann sd_journal_perror verwenden, um im globalen journal die errornummern übersetzut zu bekommen https://man7.org/linux/man-pages/man3/errno.3.html
        // 3. im klassischen errno() steht der code drin.

        // "AddressByReference is used when the address of a primitive pointer value must be passed as a parameter to a function."
        // https://javadoc.io/static/com.github.jnr/jnr-ffi/2.1.1/jnr/ffi/byref/AddressByReference.html
        AddressByReference ptrToPtr = new AddressByReference();
        Address journalHandle = ptrToPtr.getValue();

        // OK, wir sollten erst mal auf C-ebene ausprobieren, mit welchen flags ich als user da überhaupt etwas erreiche.
        // es gibt als error code ja immer nur "-1", nie details.
        int openflags = OpenFlags.getCombinedFlagValue(EnumSet.of(OpenFlags.LOCAL_ONLY));
        int ret = libJournal.sd_journal_open( ptrToPtr, 0);
        if (ret != 0) // 0 on success -- these are errno values, right?
            {
            System.out.println("OPEN Error: " + getErrorString(ret));
            // libJournal.sd_journal_perror("TESTING JNR ACCESS TO JOURNALD");
            libJournal.sd_journal_close( ptrToPtr.getValue());
            return;
            }
            // only when we first pass the initial open, continuing makes any sense.

        ret = libJournal.sd_journal_seek_tail(ptrToPtr.getValue());
        if (ret != 0)
            {
            System.out.println("seek_tail Error code: "+ getErrorString(ret));
            return;
            }

        /// ---- first: move to head!
        ret = libJournal.sd_journal_seek_head(ptrToPtr.getValue());
        if (ret != 0)
            {
            System.out.println("seek_head Error code: "+ getErrorString(ret));
            return;
            }

        ret = libJournal.sd_journal_next(ptrToPtr.getValue());
        if (ret < 0)
            {
            System.out.println("sd_journal_next Error code: "+ getErrorString(ret));
            return;
            }
        if (ret == 0)
            {
            System.out.println("no more entries");
            }




///--- wrap this in a function: read the journal message to a byte[] buffer. return somehow when buffer was insufficient - or abort/throw exception in that case.
// next wrapper: into a ByteBuffer, so we can resize, just in case.
// provide the bit *what* to read as parameter, possibly string-enum, of which "MESSAGE" is just one predefined thingy. This as convenience for safety, allow Strings so we got later extensions covered.

        final IntByReference datalength = new IntByReference();
        // OK, pointer by reference looks about right - and works.
        PointerByReference messageBuffer = new PointerByReference();
        ret = libJournal.sd_journal_get_data(ptrToPtr.getValue() , "MESSAGE", messageBuffer, datalength);
        if (ret != 0)
            {
            System.out.println("get data Error code: "+ getErrorString(ret));
            // libJournal.sd_journal_perror("TESTING JNR ACCESS TO JOURNALD");
            libJournal.sd_journal_close( ptrToPtr.getValue());
            return;
            }
        // now get the actual data out of it.
        //
        Pointer ptr = messageBuffer.getValue();
        final Integer len = datalength.getValue();
        byte[] buffer = new byte[len];  // instead of this, we want to reuse a buffer. potentially even a ByteBuffer's storage array.
        ptr.get(0L,buffer,0,len);

        Charset charset = Charset.forName("UTF-8");
        String text = new String(buffer,charset);
        System.out.println("message read: " + text);

        libJournal.sd_journal_close( ptrToPtr.getValue());
        // Memory.free(journalHandle);
        System.out.println("sauberes ende");
        return;
        }

}