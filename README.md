
### The CheckMobi SDK for Android

In order to test the sample you need to change the secret key into the Settings window and save it.

All sample settings are in `Settings` class:

- `useClientHangup` - For Reverse CLI (Missed call) you can change from where the call should be closed (client or server) by changing 
this property default value. Use `true` to close the call from client and `false` to close it from server. (default `false` ) 
- `closeCallForFailedAni` - Some carriers might not send correctly the CLI so missed call validation method might not work for that destination. 
In order to avoid the call being answered by your user you can set this property to `true`. (default `true`)

You can consult the documentation to see what are the advantages/disadvantages from closing the call from client or server.

Also in production you might want to change in the same `MainActivity.java` the `HandleValidationServiceError` implementation.

