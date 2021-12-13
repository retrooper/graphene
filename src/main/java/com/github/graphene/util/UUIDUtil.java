package com.github.graphene.util;

import java.util.UUID;
import java.util.regex.Pattern;

public class UUIDUtil {
    private static final Pattern PATTERN = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");
    public static UUID fromStringWithoutDashes(String uuid) {
        String correctedUUID = PATTERN.matcher(uuid).replaceAll("$1-$2-$3-$4-$5");
        return UUID.fromString(correctedUUID);
    }

    public static UUID fromString(String uuid) {
        return UUID.fromString(uuid);
    }

    public static String toString(UUID uuid) {
        return uuid.toString();
    }

    public static String toStringWithoutDashes(UUID uuid) {
        return uuid.toString().replace('-', ' ');
    }
}
