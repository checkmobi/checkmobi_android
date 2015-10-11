package com.checkmobi.sdk;

import java.util.EnumSet;
import java.util.Map;

public enum ErrorCode
{
    ErrorCodeNone(0),
    ErrorCodeInvalidApiKey(1),
    ErrorCodeInvalidPhoneNumber(2),
    ErrorCodeInvalidRequestId(3),
    ErrorCodeInvalidValidationType(4),
    ErrorCodeInsufficientFounds(5),
    ErrorCodeInsufficientCLIValidations(6),
    ErrorCodeInvalidRequestPayload(7),
    ErrorCodeValidationMethodNotAvailableInRegion(8),
    ErrorCodeInvalidNotificationUrl(9),
    ErrorCodeInvalidEventPayload(10);

    private final int value;
    private static final Map<Integer, ErrorCode> lookup = new DefaultHashMap<>(ErrorCodeNone);

    static
    {
        for (ErrorCode s : EnumSet.allOf(ErrorCode.class))
            lookup.put(s.getValue(), s);
    }

    public int getValue()
    {
        return value;
    }

    private ErrorCode(int value)
    {
        this.value = value;
    }

    public static ErrorCode get(int code)
    {
        return lookup.get(code);
    }
}