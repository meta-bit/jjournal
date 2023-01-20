[![Maven Package](https://github.com/meta-bit/jjournal/actions/workflows/maven-publish.yml/badge.svg)](https://github.com/meta-bit/jjournal/actions/workflows/maven-publish.yml)
![CodeQL](https://github.com/meta-bit/jjournal/workflows/CodeQL/badge.svg?branch=main)

# jjournal
Java API for reading systemd-journald logfiles

## Overview

jjournal (AKA libjournal-java-reader) is a library for reading **systemd journals** with Java.

systemd has become a standard base component in many Linux distributions.
Its logging is not using files in /var/log anymore, it uses a specific, internal log database format. Accessing the logs, called "journals", can be done via the commandline tool `journalctl`.
For programmatic access to these journals, though, there wasn't much to be found, save for the systemd C library itself.

Hence this library, which allows you to access these logs/journals from Java.
It provides a number of filtering and accessing facilities, retaining the specific structure and model used by the systemd journals.

## Dependencies
* JNR FFI
* JDK 8
# Getting Started
## Installation

### Maven
The maven artifact vector is

```maven
<dependency>
	<groupId>org.metabit.platform.interfacing</groupId>
	<artifactId>jjournal</artifactId>
	<version>0.3.4</version>
</dependency>
```

Latest version:
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.meta-bit/jjournal/badge.svg)](https://search.maven.org/artifact/org.metabit.platform.interfacing/jjournal)

## Prerequisites

The runtime system has to have `libsystemd` installed and accessible via Java library path.

## Examples

print messages for most recent minute from the journal of the current user:

```java
 import org.metabit.platform.interfacing.jjournal.Journal;
 ...
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

## Troubleshooting

jjournal is implemented as a JNR wrapper for the standard C systemd-library interface.
Because of that, the systemd library must be present and accessible for the Java code using this library.
Should this not be the case, the Journal() constructor will throw a RuntimeException.

# License

Use of jjournal is free of cost.

* GPL3: For non-commercial use, it is placed under GNU Public License 3.
* APL2: For commercial use, or if you prefer this license, you get an Apache 2.0 license after registration, at no cost.
