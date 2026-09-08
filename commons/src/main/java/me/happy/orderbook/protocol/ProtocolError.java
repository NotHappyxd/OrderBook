package me.happy.orderbook.protocol;

public enum ProtocolError {
    UNKNOWN(0),
    TOO_FEW_BYTES(1),
    TOO_MANY_BYTES(2),
    UNKNOWN_OPERATION(3),
    INVALID_FIELD(4);

    private static final ProtocolError[] BY_CODE = values();

    private final int code;

    ProtocolError(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static ProtocolError fromCode(int code) {
        if (code < 0 || code >= BY_CODE.length) {
            return UNKNOWN;
        }

        return BY_CODE[code];
    }
}
