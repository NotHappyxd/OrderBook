package me.happy.orderbook;

public class TickerUtils {

    public static String unpack(long value) {
        StringBuilder result = new StringBuilder(8);

        for (int i = 7; i >= 0; i--) {
            char code = (char) ((value >> (i * 8)) & 0xFF);

            if (code == 0)
                break;

            result.append(code);
        }

        return result.toString();
    }

    public static long packString(String s) {
        if (s.length() > 8) {
            throw new IllegalArgumentException("Max 8 chars");
        }

        long value = 0;

        for (char c : s.toCharArray()) {
            value <<= 8;
            value |= c;
        }

        return value;
    }
}
