package org.metabit.platform.interfacing.jjournal;

/**
 * consumer interface for e.g. streaming.
 * interface needed so consumer functions can properly throw regular JJournalException,
 * and other exception classes
 *
 * @param <Journal> the journal instance handle
 * @param <T> the checked exception or similar to be thrown; e.g. JJournalException.
 * @author jwilkes
 * @version $Id: $Id
 */
@FunctionalInterface
public interface JournalConsumer<Journal, T extends Throwable>
{
 /**
  * <p>accept and process given input.</p>
  *
  * @param journal a {@link org.metabit.platform.interfacing.jjournal.Journal} object
  * @throws T if any.
  */
 void accept(Journal journal) throws T;
}
