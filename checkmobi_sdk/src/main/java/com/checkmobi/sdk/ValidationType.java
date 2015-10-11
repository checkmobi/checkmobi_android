package com.checkmobi.sdk;

import java.util.EnumSet;
import java.util.Map;

public enum ValidationType
{
    CLI("cli"),
    SMS("sms"),
    IVR("ivr"),
    REVERSE_CLI("reverse_cli");

    private final String value;
    private static final Map<String, ValidationType> lookup = new DefaultHashMap<>(null);

    static
    {
        for (ValidationType s : EnumSet.allOf(ValidationType.class))
            lookup.put(s.getValue(), s);
    }

    public String getValue()
    {
        return value;
    }

    ValidationType(String value)
    {
        this.value = value;
    }

    public static ValidationType get(String code)
    {
        return lookup.get(code);
    }

}
