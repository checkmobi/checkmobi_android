
### The CheckMobi SDK for Android

In order to test the sample find and replace the API secret key from `MainActivity.java` with your own key from CheckMobi account settings.

`final private String api_secret_key = "secret_key_here";`

For Reverse CLI (Missed call) you can change from where the call should be closed (client or server) by changing `use_client_hangup` value. Use `true` to close
the call from client and `false` to close it from server. 

```java
final private boolean use_client_hangup = true;
```

Some carriers might not send correctly the CLI so missed call validation method might not work for that destination. In order to avoid the call beeing answered by your user 
you can set the `close_call_for_failed_cli` to `true`.

```java
final private boolean close_call_for_failed_cli = true;
```  

You can consult the documentation to see what are the advantages/disadvantages from closing the call from client or server.

Also in production you might want to change in the same `MainActivity.java` the `HandleValidationServiceError` implementation.



