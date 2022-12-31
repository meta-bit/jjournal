# jjournal
Java API for reading systemd-journald logfiles

## Overview

libjournal-java-reader is a library for accessing **systemd-journals** from Java.

It is implemented as a JNR wrapper for the standard C systemd-library interface.

## Dependencies
* JNR FFI
* JDK 8
# Getting Started
## Maven
### For use with Java 8+
The maven artifact vector is
```
<dependency>
	<groupId>org.metabit.platform.interfacing</groupId>
	<artifactId>jjournal</artifactId>
</depencency>
```
As value for <version>, use most recent version.

## Download
Download links
* SSH clone URL: ssh://git@git.jetbrains.space/metabit/jjournal/jjournal.git
* HTTPS clone URL: https://git.jetbrains.space/metabit/jjournal/jjournal.git

## Prerequisites

The runtime system has to have `libsystemd` installed and accessible via Java library path.

## Examples
```
 import org.metabit.platform.interfacing.jjournal.Journal;
 ...
 // print messages for most recent minute from the journal of the current user
 Instant now = Instant.now();
 Instant oneMinuteAgo = now.minusSeconds(60);
 
 try (Journal journal = new Journal(EnumSet.of(OpenFlags.CURRENT_USER)))
    {
    journal.foreachInTimerange(oneMinuteAgo,now,jj ->
        {
        final String msg = jj.readMessageField();
        System.err.println(msg);
        });
    }
```

## Deployment

Make sure libsystemd is installed and accessible via Java library path on the runtime system the code is executed on.

(e.g. `apt-get install libsystemd0`)

