package org.metabit.platform.interfacing.jjournal;

import jnr.ffi.Address;
import jnr.ffi.Pointer;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.byref.*;
import jnr.ffi.types.size_t;

import java.nio.ByteBuffer;

/*
 JNR native interface to systemd journal API

 https://github.com/systemd/systemd/blob/main/src/systemd/sd-journal.h is the header file corresponding to this interface.
 https://www.freedesktop.org/software/systemd/man/sd-journal.html is the documentation.

 The internal sd_journal structure is to be found at https://fossies.org/linux/systemd/src/libsystemd/sd-journal/journal-internal.h
 - but we should try and treat sd_journal as an opaque handle.

... get_data
... get_catalog -- should give us texts for IDs.
*/

/*
 *  declared public for technical reasons in JNR FFI (superinterface access).
 *  internal stub interface for the native access.
 */

/**
 * <p>NativeJournal interface. internal library use only</p>
 *
 * @author jwilkes
 * @version $Id: $Id
 */
@SuppressWarnings({"javadoc","unused"}) // this is an internal interface, javadoc processing not intended
public interface NativeJournal
{
  /*
   * open journal standard access, by specifying flags.
   * @param handle where to store the journal handle
   * @param flags which journal(s) types to open
   * @return library return code
   */
    /**
     * <p>sd_journal_open.</p>
     *
     * @param handle a {@link jnr.ffi.byref.AddressByReference} object
     * @param flags a int
     * @return a int
     */
    int sd_journal_open(@Out AddressByReference handle, @In int flags);  // int sd_journal_open(sd_journal **ret, int flags);
    /**
     * <p>sd_journal_open_namespace.</p>
     *
     * @param handle a {@link jnr.ffi.byref.AddressByReference} object
     * @param name_space a {@link java.lang.String} object
     * @param flags a int
     * @return a int
     */
    int sd_journal_open_namespace(@Out AddressByReference handle, @In String name_space, @In int flags); // sd_journal_open_namespace(sd_journal **ret, const char *name_space, int flags); -- not tested yet

    /**
     * <p>sd_journal_open_files.</p>
     *
     * @param journalHandle a {@link jnr.ffi.byref.AddressByReference} object
     * @param filenameArray a {@link jnr.ffi.Pointer} object
     * @param flags a int
     * @return a int
     */
    int sd_journal_open_files(@Out AddressByReference journalHandle, @In Pointer filenameArray, @In int flags);
    //
    /**
     * <p>sd_journal_open_directory.</p>
     *
     * @param journalHandle a {@link jnr.ffi.byref.AddressByReference} object
     * @param absolutePath a {@link java.lang.String} object
     * @param flags a int
     * @return a int
     */
    int sd_journal_open_directory(@Out AddressByReference journalHandle,  @In String absolutePath, @In int flags);  // int sd_journal_open_directory(sd_journal **ret, const char *path, int flags);
  /* --- not used yet ---
  // see https://www.freedesktop.org/software/systemd/man/sd_journal_open.html#
   "As first argument it takes a pointer to a sd_journal pointer, which, on success, will contain a journal context object."
   So, how do we map this in JNR?
   */
//    int open_directory(@Out AddressByReference handle, @In String dir, @In  int flags); // sd_journal_open_directory(sd_journal **ret, const char *path, int flags); --  not tested yet
//    int open_directory(@Out AddressByReference handle, @In Path path, @In  int flags); // sd_journal_open_directory_fd(sd_journal **ret, int fd, int flags); --  not tested yet
//     int open_files(    @Out AddressByReference handle, @In List<String> dirs, @In int flags); // sd_journal_open_files(sd_journal **ret, const char **paths, int flags); --  not tested yet -- or @Out PointerByReference
    //    Journal open_files(List<Path> paths, int flags); // sd_journal_open_files_fd(sd_journal **ret, int fds[], unsigned n_fds, int flags); collides with the list of strings one.
  // how can we tell them apart properly so the automatic mapping doesn't get confused?

  /*
   * close journal access.
   * @param handle journal handle
   */
    /**
     * <p>sd_journal_close.</p>
     *
     * @param handle a {@link jnr.ffi.Address} object
     */
    void sd_journal_close(@In Address handle); // void sd_journal_close(sd_journal *j);

    // ---- journal metadata ----

    /*
     *
     * quote from <a href="https://www.freedesktop.org/software/systemd/man/sd_journal_enumerate_fields.html#">C API documentation</a>
     * "sd_journal_enumerate_fields() may be used to iterate through all field names used in the opened journal files. On each invocation the next field name is returned. The order of the returned field names is not defined. It takes two arguments: the journal context object, plus a pointer to a constant string pointer where the field name is stored in. The returned data is in a read-only memory map and is only valid until the next invocation of sd_journal_enumerate_fields(). Note that this call is subject to the data field size threshold as controlled by sd_journal_set_data_threshold().
     *
     * sd_journal_restart_fields() resets the field name enumeration index to the beginning of the list. The next invocation of sd_journal_enumerate_fields() will return the first field name again."
     */
    /**
     * <p>sd_journal_enumerate_fields.</p>
     *
     * @param handle a {@link jnr.ffi.Address} object
     * @param fieldName a {@link jnr.ffi.byref.PointerByReference} object
     * @return a int
     */
    int sd_journal_enumerate_fields(@In Address handle, @Out PointerByReference fieldName); // int sd_journal_enumerate_fields(sd_journal *j, const char **field); // or PointerByReference --  not tested yet
    /**
     * <p>sd_journal_restart_fields.</p>
     *
     * @param handle a {@link jnr.ffi.Address} object
     */
    void sd_journal_restart_fields(@In Address handle); // --  not tested yet

    /*
     *  "To retrieve the possible values a specific field can take use sd_journal_query_unique(3)."
     */
    /**
     * <p>sd_journal_query_unique.</p>
     *
     * @param handle a {@link jnr.ffi.Address} object
     * @param field a {@link java.lang.String} object
     * @return a int
     */
    int sd_journal_query_unique(Address handle, String field); // --  not tested yet

// ------ move in a journal ------
    /* returns number of entries advanced/set back on success or a negative errno-style error code.
       When the end or beginning of the journal is reached, a number smaller than requested is returned.
       More specifically, if sd_journal_next() or sd_journal_previous() reach the end/beginning of the journal they will return 0,
       instead of 1 when they are successful. This should be considered an EOF marker.
     */
    // --- order of entries: "The journal is strictly ordered by reception time"
    /* sd_journal_previous() sets the read pointer back one entry.
     * The journal is strictly ordered by reception time, and hence advancing to the next entry guarantees that the entry then pointing to is later in time than then previous one, or has the same timestamp.
     */
    /**
     * <p>sd_journal_previous.</p>
     *
     * @param handle a {@link jnr.ffi.Address} object
     * @return a int
     */
    int sd_journal_previous(@In Address handle); // int sd_journal_previous(sd_journal *j);
    /* sd_journal_next() advances the read pointer into the journal by one entry.  */
    /**
     * <p>sd_journal_next.</p>
     *
     * @param handle a {@link jnr.ffi.Address} object
     * @return a int
     */
    int sd_journal_next(@In Address handle); // int sd_journal_next(sd_journal *j);

    /**
     * <p>sd_journal_previous_skip.</p>
     *
     * @param handle a {@link jnr.ffi.Address} object
     * @param skip a {@link java.lang.Long} object
     * @return a int
     */
    int sd_journal_previous_skip(@In Address handle, @In Long skip); // int sd_journal_previous_skip(sd_journal *j, uint64_t skip);
    /**
     * <p>sd_journal_next_skip.</p>
     *
     * @param handle a {@link jnr.ffi.Address} object
     * @param skip a {@link java.lang.Long} object
     * @return a int
     */
    int sd_journal_next_skip(@In Address handle, @In Long skip); //    int sd_journal_next_skip(sd_journal *j, uint64_t skip);

    /**
     * <p>sd_journal_seek_head.</p>
     *
     * @param handle a {@link jnr.ffi.Address} object
     * @return a int
     */
    int sd_journal_seek_head(@In Address handle); // checked.
    /**
     * <p>sd_journal_seek_tail.</p>
     *
     * @param handle a {@link jnr.ffi.Address} object
     * @return a int
     */
    int sd_journal_seek_tail(@In Address handle); // checked.

//-------------------------------------- cursor-based movement

  /*
   * move cursor to a given position.
   */
    /**
     * <p>sd_journal_seek_cursor.</p>
     *
     * @param handle a {@link jnr.ffi.Address} object
     * @param cursor a {@link java.lang.String} object
     * @return a int
     */
    int sd_journal_seek_cursor(@In Address handle, @In String cursor);

  /*
   * get cursor as string.
   */
    /**
     * <p>sd_journal_get_cursor.</p>
     *
     * @param handle a {@link jnr.ffi.Address} object
     * @param cursorPointerReadOnly a {@link jnr.ffi.byref.PointerByReference} object
     * @return a int
     */
    int sd_journal_get_cursor(@In Address handle, @Out PointerByReference cursorPointerReadOnly);

  /*
   * test whether the current position matches a provided cursor.
   */
  /**
   * <p>sd_journal_test_cursor.</p>
   *
   * @param handle a {@link jnr.ffi.Address} object
   * @param cursor a {@link java.lang.String} object
   * @return a int
   */
  int sd_journal_test_cursor(@In Address handle, @In String cursor);

/// -------------------------- reading data

  /*
   * see https://www.freedesktop.org/software/systemd/man/sd_journal_get_data.html#
   * -99: you forgot to step/move to a valid entry, didn't you? seek head/tail go to invalid ones, you need to move one first.
   * -22: file access fails?
   */
    /**
     * <p>sd_journal_get_data.</p>
     *
     * @param handle a {@link jnr.ffi.Address} object
     * @param field a {@link java.lang.String} object
     * @param data a {@link jnr.ffi.byref.PointerByReference} object
     * @param length a {@link jnr.ffi.byref.IntByReference} object
     * @return a int
     */
    int sd_journal_get_data(@In Address handle, @In String field, @Out PointerByReference data, @Out @size_t IntByReference length); // const void **data, size_t *l

    /**
     * <p>sd_journal_enumerate_data.</p>
     *
     * @param handle a {@link jnr.ffi.byref.AddressByReference} object
     * @param data a {@link jnr.ffi.byref.PointerByReference} object
     * @param length a {@link jnr.ffi.byref.IntByReference} object
     * @return a int
     */
    int sd_journal_enumerate_data(@In AddressByReference handle, @Out PointerByReference data, @Out @size_t IntByReference length);// const void **data, size_t *l  -  not tested yet

    /**
     * <p>sd_journal_enumerate_available_data.</p>
     *
     * @param handle a {@link jnr.ffi.byref.AddressByReference} object
     * @param data a {@link jnr.ffi.byref.PointerByReference} object
     * @param length a {@link jnr.ffi.byref.IntByReference} object
     * @return an int
     */
    int sd_journal_enumerate_available_data(@In AddressByReference handle, @Out  PointerByReference data, @Out @size_t IntByReference length); // const void **data, size_t *l  -  not tested yet
    /**
     * <p>sd_journal_restart_data.</p>
     *
     * @param handle a {@link jnr.ffi.byref.AddressByReference} object
     */
    void sd_journal_restart_data(@In AddressByReference handle);  // -  not tested yet



  //-------- datetime positioning --------
  // "cutoff" is systemd-speak to mean last and first entry times.

  /*
   * get the limits of the current journal.
   * @param handle the journal handle
   * @param from the timestamp of the first (oldest) entry available in the journal
   * @param to the timestamp of the last (latest) entry available in the journal
   * @return library status code
   */
  /**
   * <p>sd_journal_get_cutoff_realtime_usec.</p>
   *
   * @param handle a {@link jnr.ffi.Address} object
   * @param from a {@link jnr.ffi.byref.LongLongByReference} object
   * @param to a {@link jnr.ffi.byref.LongLongByReference} object
   * @return a int
   */
  int sd_journal_get_cutoff_realtime_usec(@In Address handle, @Out LongLongByReference from, @Out LongLongByReference to);  // int sd_journal_get_cutoff_realtime_usec(Address handle, uint64_t *from, uint64_t *to);
  /*
   * it may seem hard to believe, but getting entries from a specific date/time range is done in journalctl by going through *all* entries,
   * and omitting those whose timestamp doesn't match. For this wasteful behaviour, you need this function:
   *
   * @param handle  the journal handle
   * @param time    the timestamp in UTC (?)
   * @return status code
   */
  /**
   * <p>sd_journal_get_realtime_usec.</p>
   *
   * @param handle a {@link jnr.ffi.Address} object
   * @param time a {@link jnr.ffi.byref.LongLongByReference} object
   * @return a int
   */
  int sd_journal_get_realtime_usec(@In Address handle, @Out LongLongByReference time); // uint64_t *ret -- or should we use ByReference<u_int64_t> ?

  /*
   * move journal position (see: cursor) to a given timestamp.
   * @param handle the journal handle
   * @param usec the microsecond timestamp to move (seek) to
   * @return status code
   *
   * https://www.freedesktop.org/software/systemd/man/sd_journal_seek_realtime_usec.html#
   */
  /**
   * <p>sd_journal_seek_realtime_usec.</p>
   *
   * @param handle a {@link jnr.ffi.Address} object
   * @param usec a {@link java.lang.Long} object
   * @return a int
   */
  int sd_journal_seek_realtime_usec(@In Address handle, @In Long usec);


  /* the "monotonic" functions are relating to the boot journal / boot perspective.
     we're not handling these yet.
    int sd_journal_get_monotonic_usec(@In Address handle, NativeLongByReference ret, LongLongByReference ret_boot_id); // uint64_t *ret, sd_id128_t *ret_boot_id);
//  int sd_journal_get_monotonic_usec(@In Address handle, NativeLongByReference ret, LongLongByReference ret_boot_id); // uint64_t *ret, sd_id128_t *ret_boot_id);
    int sd_journal_seek_monotonic_usec(Address handle, sd_id128_t boot_id, u_int64_t usec);
//  int sd_journal_seek_monotonic_usec(Address handle, sd_id128_t boot_id, u_int64_t usec);
    int sd_journal_get_cutoff_monotonic_usec(@In Address handle, const sd_id128_t boot_id,@Out LongLongByReference from, @Out LongLongByReference to);
// int sd_journal_get_cutoff_monotonic_usec(Address handle, const sd_id128_t boot_id, uint64_t *from, uint64_t *to);
*/
    // int sd_journal_set_data_threshold(@In Address handle, @In size_t sz);
    //int sd_journal_get_data_threshold(@In Address handle, @Out ByReference<size_t> sz); // will this work out-of-the-box?


    /**
     * <p>sd_journal_add_match.</p>
     *
     * @param handle a {@link jnr.ffi.Address} object
     * @param data a {@link java.nio.ByteBuffer} object
     * @param length a int
     * @return a int
     */
    int sd_journal_add_match(@In Address handle, @In ByteBuffer data, @Out @size_t int length); //const void *data, size_t size
    /**
     * <p>sd_journal_add_disjunction.</p>
     *
     * @param handle a {@link jnr.ffi.Address} object
     * @return a int
     */
    int sd_journal_add_disjunction(@In Address handle);
    /**
     * <p>sd_journal_add_conjunction.</p>
     *
     * @param handle a {@link jnr.ffi.Address} object
     * @return a int
     */
    int sd_journal_add_conjunction(@In Address handle);
    /**
     * <p>sd_journal_flush_matches.</p>
     *
     * @param handle a {@link jnr.ffi.Address} object
     */
    void sd_journal_flush_matches(@In Address handle);



/*
    @Out @size_t IntByReference  int sd_journal_enumerate_unique(Address handle, const void **data, size_t *l);
    @Out @size_t IntByReference  int sd_journal_enumerate_available_unique(Address handle, const void **data, size_t *l);
    void sd_journal_restart_unique(Address handle);
//    for turning the sd_id128_t into an UUID use "UUID uuid = new UUID(high, low);" on two longs.
*/

// ---- syncs and waits ----
/*
    int sd_journal_get_fd(Address handle);
    int sd_journal_get_events(Address handle);
     int sd_journal_get_timeout(@In Address handle, @Out LongLongByReference timeout_usec); //     int sd_journal_get_timeout(Address handle, uint64_t *timeout_usec);
    int sd_journal_process(@In Address handle);
    int sd_journal_wait(@In  Address handle, u_int64_t timeout_usec); //!! not a pointer. So, LongLong -- that may be trickier...
    int sd_journal_reliable_fd(Address handle);
*/
/* ---- catalog ----
   catalog functionality: these are an ID system to look up further information on messages, using a template pattern and external files.
   not sure how much this is in use.

   There is a risk in using these, creating memory leaks or worse.
    "On successful return, ret points to a new string, which must be freed with free(3)."
    so - we do need to get free() from  #include <stdlib.h> in here, too? Or would the IO ... something MemoryManager be sufficient?
    or is it a different free(), when JNR FFI is involved?

    int sd_journal_get_catalog(Address handle, char **text);  -  not tested yet  @Out PointerByReference
    int sd_journal_get_catalog_for_message_id(sd_id128_t id, char **text);   -  not tested yet

   see
    https://www.freedesktop.org/software/systemd/man/sd_journal_get_catalog.html#
    https://www.freedesktop.org/wiki/Software/systemd/catalog/

     */

  /*
   * get the amount of disk space used by journals.
   * @param handle the journal handle.
   * @param bytes the amount of disk space used by journals, in byte.
   * @return library return code
   */
    /**
     * <p>sd_journal_get_usage.</p>
     *
     * @param handle a {@link jnr.ffi.Address} object
     * @param bytes a {@link jnr.ffi.byref.LongLongByReference} object
     * @return a int
     */
    int sd_journal_get_usage(@In Address handle, @Out LongLongByReference bytes); // uint64_t *bytes)

  /*
   * boolean check: does it have runtime files
   * @param handle handle of the journal to check
   * @return library return code
   */
    /**
     * <p>sd_journal_has_runtime_files.</p>
     *
     * @param handle a {@link jnr.ffi.Address} object
     * @return a int
     */
    int sd_journal_has_runtime_files(@In Address handle);

  /*
   * boolean check: does it have persistent files?
   * this is ot about actual persistency, just regular files that don't get deleted after reboot.
   * @param handle the handle of the journal to check
   * @return library return code
   */
    /**
     * <p>sd_journal_has_persistent_files.</p>
     *
     * @param handle a {@link jnr.ffi.Address} object
     * @return a int
     */
    int sd_journal_has_persistent_files(@In Address handle);

  // writing errors to journal:  int sd_journal_perror(String message);

}
// strerror is in stdlib, not in this library.
