# Session Changer
Login to your Microsoft Account from your MCP client.

__IMPORTANT__: Use this solution only for testing, not for production!! 
- You will share your Azure App's secret and
- Your refresh token gets saved in a plain file. Everyone knowing your refresh key can get information about your account without any password!!! 
Recommend solution:
If you want to publish your client, it is recommended to host a server, with an own api, that can save your secret and every refresh token safely. 
(Similar how Eric Golde uses a server which handles the database connection, so that the database password is not saved in the client, but at the server he wrote
I highly recommend to do something similar with the Microsoft Authenticator! ) 
## How to use
### First you nee to set up a __Microsoft Azure application__
1. Open https://portal.azure.com and login with your Microsoft Account
2. Search for Azure Active Directory
3. Under __Manage__ click on __Add__ and select __App registrations__
4. Enter a display Name for your application
5. Select __Personal Microsoft accounts__
6. Choose __Web__ and enter __http://localhost/api__
7. Click on __register__
8. Under __Overview__ copy __Application (client) id__ (you will need it later)
9. Under __Certificates & secrets__ click on __New client ecret__
10. Choose a display name and select a lifetime and click on __Add__ (you need to replace the secret when its lifetime is over)
11. Copy the __Value__. You can get this value only now, after refreshing the page the value will get obfuscated (you will need it later) 

### Now copy the following lines into your client and replace __clientId__ and __clientSecret__ with your values
```java
MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator(clientId, clientSecret);
authenticator.login();
```
You need to change `private final Session session;` in __Minecraft.java__ to `public Session session;`
## How this works
This class uses the Microsoft Authentication Scheme to login to your Xbox Life Accounz and to get your access token / session id to change the session of your client.
On the first startup it opens a browser page in which you need to login to your Microsoft account. It then saves a refresh token in the .minecraft/ClientName/token.json  file so theres no need to open the browser to login in the future.

## Notes
It can happen, that the saved refresh token gets invalid and you can't login anymore. Try to remove the token.json file and restart to game to freshly login to your account and to get a new refresh token.

## Ressources:
https://wiki.vg/Microsoft_Authentication_Scheme<br /> 
https://docs.microsoft.com/de-de/azure/active-directory/develop/v2-oauth2-auth-code-flow<br /> 
https://docs.microsoft.com/en-us/azure/active-directory/develop/quickstart-register-app
