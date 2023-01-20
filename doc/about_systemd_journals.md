# about systemd journals

a short primer, from jjournal library.

## concepts

In the background, systemd maintains a special type of database for logs / journals.
The format is intentionally marked as "subject to change, do not access directly".

The interface available on it, however, uses these concepts:

### entries

Like the lines in log files, the systemd journals have entries. 

### fields

Each entry consists of several fields, like timestamp, severity, and so on.
Logfiles had only some general notion of what to expect, and variied in format, which required separate
parsing depending on logfile source. In contrast, the fields in systemd journals are standardised,
but extensible.

There is a list of predefined field names (see JournalField enum).

Fields may contain either text or binary values.

### units

In /var/log, each program kept its own file or files, under different names.
In systemd journals, the systemd "units" are the source of journal entries;
so filtering by which unit caused a journal entry is used very often.

### journals

Access to the journals is limited, and there are separate namespaces; local or remote sources,
system or current user, persistent or runtime only - and, in addition to the standard system journals,
one may also specify a directory or set of files to use, e.g. from backups.

The Journal() constructor OpenFlags and optional namespace parameter allow to specify which 
journal(s) are to be used.

Filtering by unit is optional; specifying journal access is mandatory.

### cursor

When a journal has been opened, a "cursor" is maintained, like a line reference in a text file, or a database cursor.

It can be moved forwards, backwards; it can skip entries, move to start or end of the "visible" set of entries.

It can be also exported or imported as a String; treat it as opaque, though.

## combined

Given these concepts, reading systemd journals goes like this:

1. open the journal, e.g. CURRENT_USER. Trying to open journals the current process owner is allowed to access 
may cause an exception to be thrown, or the journal to appear empty.
2. optional: set filters, to limit which entries are show. e.g. specifying which unit
3. move the cursor
4. read fields from the current cursor position



