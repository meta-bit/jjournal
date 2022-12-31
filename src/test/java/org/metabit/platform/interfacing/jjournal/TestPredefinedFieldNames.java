package org.metabit.platform.interfacing.jjournal;

import org.junit.jupiter.api.Test;

public class TestPredefinedFieldNames
{
 @Test
 void testFieldnames()
     {
      for (JournalField fieldValue : JournalField.values())
          {
          final String valueName = fieldValue.getValue();
          // this will throw an exception if a name is invalid
          Journal.validateFieldName(valueName);
          }
     }
}
