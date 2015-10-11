
### The CheckMobi SDK for Android

In order to test the sample find and replace the API secret key from `MainActivity.java` with your own key from CheckMobi account settings.

`final private String api_secret_key = "secret_key_here";`

For Reverse CLI (Missed call) you can change from where the call should be closed (client or server):

```java
final private boolean use_client_hangup = true;
final private boolean use_server_hangup = true;
```

You can consult the documentation to see what are the advantages/disadvantages from closing the call from client or server.

Also in production you might want to change in the same `MainActivity.java` the `HandleValidationServiceError` implementation.



