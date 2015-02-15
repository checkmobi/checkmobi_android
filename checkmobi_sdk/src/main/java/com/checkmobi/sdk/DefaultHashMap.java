package com.checkmobi.sdk;

import java.util.HashMap;

class DefaultHashMap<K, V> extends HashMap<K, V>
{
    private V default_value_ = null;

    public DefaultHashMap(V defaultValue)
    {
        this.default_value_ = defaultValue;
    }

    @Override
    public V get(Object k)
    {
        V value = super.get(k);

        if(value == null)
            return default_value_;

        return value;
    }
}