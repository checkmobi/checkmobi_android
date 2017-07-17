package com.checkmobi.sdk;

import java.util.HashMap;

public class CheckMobiService extends RestServiceBase
{
    private static final String PLATFORM = "android";

    //HTTP status codes

    public static final int STATUS_SUCCESS = 200;
    public static final int STATUS_SUCCESS_NO_CONTENT = 204;
    public static final int STATUS_BAD_REQUEST = 400;
    public static final int STATUS_UNAUTHORISED = 401;
    public static final int STATUS_NOT_FOUND = 404;
    public static final int STATUS_INTERNAL_SERVER_ERROR = 500;

    //properties

    private String notificationUrl = null;
    private String ivrLanguage = null;
    private String smsLanguage = null;
    private boolean useServerHangup = true;

    //URLs

    private static final String kBaseUrl = "https://api.checkmobi.com";
    private static final String kRequestValidationResource = "/v1/validation/request";
    private static final String kValidationStatusResource = "/v1/validation/status";
    private static final String kValidationPinVerifyResource = "/v1/validation/verify";
    private static final String kValidationCheckNumberResource = "/v1/checknumber";
    private static final String kSendSmsResource = "/v1/sms/send";
    private static final String kGetSmsDetailsResource = "/v1/sms";
    private static final String kCallResource = "/v1/call";
    private static final String kGetCountriesListResource = "v1/countries";

    private static class LazyHolder
    {
        private static final CheckMobiService INSTANCE = new CheckMobiService();
    }

    public static CheckMobiService getInstance()
    {
        return LazyHolder.INSTANCE;
    }

    private CheckMobiService()
    {
        super(kBaseUrl);
    }

    //public methods

    public void SetNotificationUrl(String value)
    {
        this.notificationUrl = value;
    }

    public void SetIVRLanguage(String value)
    {
        this.ivrLanguage = value;
    }

    public void SetSMSLanguage(String value)
    {
    	this.smsLanguage = value;
	}

    public void SetUseServerHangup(boolean value) { this.useServerHangup = value; }

    public void RequestValidation(ValidationType type, String e164_number, AsyncResponse response)
    {
        HashMap<String, Object> map = new HashMap<>();
        map.put("type", type.getValue());
        map.put("number", e164_number);
        map.put("platform", CheckMobiService.PLATFORM);

        if(this.notificationUrl != null)
            map.put("notification_callback", this.notificationUrl);

        if(type == ValidationType.IVR && this.ivrLanguage != null)
            map.put("language", this.ivrLanguage);
        else if(type == ValidationType.SMS && this.smsLanguage != null)
            map.put("language", this.smsLanguage);

        PerformRequest(kRequestValidationResource, Method.POST, map, response);
    }

    public void CheckValidationStatus(String requestId, AsyncResponse response)
    {
        String resource = kValidationStatusResource +"/"+requestId;
        PerformRequest(resource, Method.GET, null, response);
    }

    public void VerifyPin(ValidationType type, String request_id, String pin, AsyncResponse response)
    {
        HashMap<String, Object> map = new HashMap<>();
        map.put("id", request_id);
        map.put("pin", pin);

        if(type == ValidationType.REVERSE_CLI)
            map.put("use_server_hangup", this.useServerHangup);

        PerformRequest(kValidationPinVerifyResource, Method.POST, map, response);
    }

    public void SendSms(String to, String text, String callback, AsyncResponse response)
    {
        HashMap<String, Object> map = new HashMap<>();
        map.put("to", to);
        map.put("text", text);
        map.put("platform", CheckMobiService.PLATFORM);

        if(callback != null)
            map.put("notification_callback", callback);

        PerformRequest(kSendSmsResource, Method.POST, map, response);
    }

    public void GetSmsDetails(String id, AsyncResponse response)
    {
        String resource = kGetSmsDetailsResource +"/"+id;
        PerformRequest(resource, Method.GET, null, response);
    }

    public void PlaceCall(String from, String to, String events, String callback, AsyncResponse response)
    {
        HashMap<String, Object> map = new HashMap<>();

        if(from != null)
            map.put("from", from);

        map.put("to", to);
        map.put("events", events);
        map.put("platform", CheckMobiService.PLATFORM);

        if(callback != null)
            map.put("notification_callback", callback);

        PerformRequest(kCallResource, Method.POST, map, response);
    }

    public void GetCallDetails(String id, AsyncResponse response)
    {
        String resource = kCallResource +"/"+id;
        PerformRequest(resource, Method.GET, null, response);
    }

    public void HangupCall(String id, AsyncResponse response)
    {
        String resource = kCallResource +"/"+id;
        PerformRequest(resource, Method.DELETE, null, response);
    }

    public void CheckNumber(String e164_number, AsyncResponse response)
    {
        HashMap<String, Object> map = new HashMap<>();
        map.put("number", e164_number);

        PerformRequest(kValidationCheckNumberResource, Method.POST, map, response);
    }

    public void GetCountriesList(AsyncResponse response)
    {
        PerformRequest(kGetCountriesListResource, Method.GET, null, response);
    }

}
