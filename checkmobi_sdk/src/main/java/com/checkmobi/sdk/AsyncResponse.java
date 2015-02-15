package com.checkmobi.sdk;


import java.util.Map;

public interface AsyncResponse
{
    public void OnRequestCompleted(int httpStatus, Map<String, Object> responseMap, String error);
}
