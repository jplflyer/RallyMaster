package org.showpage.rallyserver.util;

import org.showpage.rallyserver.exception.ValidationException;

import java.time.LocalDate;

/**
 * This will validate non-null data while providing a nice messages instead of something generic.
 */
public class DataValidator {
    public static void validate(Object...args) throws ValidationException {
        for (int index = 0; index < args.length; index += 2) {
            Object arg = args[index];
            Object name = args[index + 1];

            boolean shouldThrow = switch (arg) {
                case null -> true;
                case String str -> str.trim().isEmpty();
                default -> false;
            };
            if (shouldThrow) {
                throw new ValidationException(name.toString() +  " may not be null/empty");
            }
        }
    }

    public static void validDate(LocalDate date, LocalDate earliest, LocalDate last, String name) throws ValidationException {
        if (date == null || date.isBefore(earliest) || date.isAfter(last)) {
            throw new ValidationException("Invalid date: " + name);
        }
    }

    public static boolean nonEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }
}
