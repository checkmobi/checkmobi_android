package com.checkmobi.sdk;


import java.util.Map;

public interface AsyncResponse
{
    void OnRequestCompleted(int httpStatus, Map<String, Object> responseMap, String error);
}
