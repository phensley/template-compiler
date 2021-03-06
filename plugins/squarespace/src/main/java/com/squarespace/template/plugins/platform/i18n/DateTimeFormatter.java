/**
 * Copyright (c) 2017 SQUARESPACE, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.squarespace.template.plugins.platform.i18n;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.squarespace.cldr.CLDR;
import com.squarespace.cldr.dates.CalendarFormat;
import com.squarespace.cldr.dates.CalendarFormatOptions;
import com.squarespace.cldr.dates.CalendarFormatter;
import com.squarespace.cldr.dates.CalendarSkeleton;
import com.squarespace.cldr.dates.SkeletonType;
import com.squarespace.cldr.dates._CalendarUtils;
import com.squarespace.template.Arguments;
import com.squarespace.template.ArgumentsException;
import com.squarespace.template.BaseFormatter;
import com.squarespace.template.CodeExecuteException;
import com.squarespace.template.Context;
import com.squarespace.template.Variable;
import com.squarespace.template.Variables;
import com.squarespace.template.plugins.PluginDateUtils;


/**
 * DATETIME - Locale formatted dates according to Unicode CLDR rules. http://cldr.unicode.org/
 *
 * This formatter takes up to 3 arguments of the following types:
 *
 *     Named date format:   'date:short', 'date:medium', 'date:long', 'date:full'
 *     Named time format:   'time:short', 'time:medium', 'time:long', 'time:full'
 *  Skeleton date format:   'date:yMMMd', 'date:yQQQQ', 'date:yM', etc.
 *  Skeleton time format:   'time:Hmv', 'time:hms', etc.
 *        Wrapper format:   'wrap:short', 'wrap:medium', 'wrap:long', 'wrap:full'
 *
 * You can provide a date or time name/skeleton argument, and an optional
 * wrapper argument:
 *
 *     {t|datetime date:long time:short wrap:full}
 *     "November 2, 2017 at 2:26 PM"
 *
 *     {t|datetime date:short time:full wrap:short}
 *     "11/2/17, 2:26:57 PM"
 *
 * If no wrapper argument is specified, it is based on the date format. The
 * following will use "wrap:medium" since "date:medium" is present:
 *
 *     {t|datetime date:medium time:short}
 *     "Nov 2, 2017, 2:26 PM"
 *
 * You can also pass a date or time skeleton:
 *
 *     {t|datetime date:yMMMd time:hm wrap:long}
 *     "Nov 2, 2017 at 2:26 PM"
 *
 * Order of arguments doesn't matter, so this is equivalent to the above:
 *
 *     {t|datetime wrap:long time:hm date:yMMMd}
 *     "Nov 2, 2017 at 2:26 PM"
 *
 * If you provide no arguments you get the equivalent of "date:yMd":
 *
 *     {t|datetime}
 *     "11/2/2017"
 *
 */
public class DateTimeFormatter extends BaseFormatter {

  public DateTimeFormatter() {
    super("datetime", false);
  }

  @Override
  public void validateArgs(Arguments args) throws ArgumentsException {
    args.atMost(3);
    CalendarFormatOptions options = new CalendarFormatOptions();
    setCalendarFormatOptions(options, args);
    args.setOpaque(options);
  }

  @Override
  public void apply(Context ctx, Arguments args, Variables variables) throws CodeExecuteException {
    Variable var = variables.first();
    long instant = var.node().asLong();

    String tzName = PluginDateUtils.getTimeZoneNameFromContext(ctx);
    ZonedDateTime datetime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(instant), ZoneId.of(tzName));
    CLDR.Locale locale = ctx.cldrLocale();
    CalendarFormatter formatter = CLDR.get().getCalendarFormatter(locale);
    CalendarFormatOptions options = (CalendarFormatOptions) args.getOpaque();
    StringBuilder buffer = new StringBuilder();
    formatter.format(datetime, options, buffer);
    var.set(buffer);
  }

  private static void setCalendarFormatOptions(CalendarFormatOptions options, Arguments args) {
    for (String arg : args.getArgs()) {
      int i = arg.indexOf(':');
      if (i == -1) {
        switch (arg) {
          case "date":
            options.setDateFormat(CalendarFormat.SHORT);
            break;

          case "time":
            options.setTimeFormat(CalendarFormat.SHORT);
            break;

          default:
            CalendarFormat format = calendarFormat(arg);
            if (format != null) {
              options.setDateFormat(format);
              options.setTimeFormat(format);
            } else {
              // Check if argument is a bare skeleton and set its type.
              SkeletonType type = _CalendarUtils.skeletonType(arg);
              CalendarSkeleton skeleton = CalendarSkeleton.fromString(arg);
              if (type == SkeletonType.DATE) {
                options.setDateSkeleton(skeleton);
              } else {
                options.setTimeSkeleton(skeleton);
              }
            }
            break;
        }
        continue;
      }

      String key = arg.substring(0, i);
      String val = arg.substring(i + 1);

      switch (key) {
        case "date":
        {
          CalendarFormat format = calendarFormat(val);
          if (format == null) {
            options.setDateSkeleton(CalendarSkeleton.fromString(val));
          } else {
            options.setDateFormat(format);
          }
          break;
        }

        case "time":
        {
          CalendarFormat format = calendarFormat(val);
          if (format == null) {
            options.setTimeSkeleton(CalendarSkeleton.fromString(val));
          } else {
            options.setTimeFormat(format);
          }
          break;
        }

        case "datetime":
        case "wrap":
        case "wrapper":
        case "wrapped":
        {
          options.setWrapperFormat(calendarFormat(val));
          break;
        }

        default:
          break;
      }
    }
  }

  private static CalendarFormat calendarFormat(String arg) {
    if (arg == null) {
      return null;
    }
    switch (arg) {
      case "short":
        return CalendarFormat.SHORT;

      case "medium":
        return CalendarFormat.MEDIUM;

      case "long":
        return CalendarFormat.LONG;

      case "full":
        return CalendarFormat.FULL;

      default:
        return null;
    }
  }

}