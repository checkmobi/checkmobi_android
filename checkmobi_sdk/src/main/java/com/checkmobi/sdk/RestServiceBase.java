package com.checkmobi.sdk;


import com.turbomanage.httpclient.AsyncCallback;
import com.turbomanage.httpclient.HttpResponse;
import com.turbomanage.httpclient.android.AndroidHttpClient;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Map;

public class RestServiceBase
{
    private String secretKey;
    private String baseUrl;

    //auth header & content type

    private static final String kAuthorizationHeader = "Authorization";
    private static final String kContentType = "application/json";

    //http methods

    protected enum Method {GET, POST, DELETE }


    RestServiceBase(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    public void SetSecretKey(String secretKey)
    {
        this.secretKey = secretKey;
    }
    public void SetBaseUrl(String baseUrl) {this.baseUrl = baseUrl;}

    void PerformRequest(String resource, Method method, Map<String, Object> params, final AsyncResponse response)
    {
        AndroidHttpClient httpClient = new AndroidHttpClient(this.baseUrl);
        httpClient.addHeader(kAuthorizationHeader, this.secretKey);
        httpClient.setMaxRetries(3);
        httpClient.setConnectionTimeout(10000);
        httpClient.setReadTimeout(10000);

        final AsyncCallback callback = new AsyncCallback()
        {
            @Override
            public void onComplete(HttpResponse httpResponse)
            {
                response.OnRequestCompleted(httpResponse.getStatus(), ParseJsonResponseBody(httpResponse.getBodyAsString()), null);
            }

            @Override
            public void onError(Exception e)
            {
                response.OnRequestCompleted(0, null, e.getMessage());
            }
        };

        if(method == Method.POST)
            httpClient.post(resource, kContentType, MapToJsonStringBytes(params), callback);
        else if(method == Method.GET)
            httpClient.get(resource, null, callback);
        else if (method == Method.DELETE)
            httpClient.delete(resource, null, callback);
        else
            response.OnRequestCompleted(0, null, "Unsupported HTTP method");
    }

    //private methods

    private byte[] MapToJsonStringBytes(Map<String, Object> params)
    {
        try
        {
            if(params == null || params.isEmpty())
                return null;

            return new JSONObject(params).toString().getBytes();
        }
        catch (Exception ex)
        {
            System.out.println(Arrays.toString(ex.getStackTrace()));
            return null;
        }
    }

    private Map<String, Object> ParseJsonResponseBody(String response)
    {
        try
        {
            if(response == null || response.isEmpty())
                return null;

            JSONObject obj = new JSONObject(response);
            return JsonHelper.toMap(obj);
        }
        catch (Exception ex)
        {
            System.out.println(Arrays.toString(ex.getStackTrace()));
            return null;
        }
    }

}
