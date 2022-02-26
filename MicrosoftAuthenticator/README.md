# Session Changer
Login to your Microsoft Account from your MCP client.

__IMPORTANT__: Use this solution only for testing, not for production, as you need to save your client secret!

## How to use
1. Open https://portal.azure.com and login with your Microsoft Account
2. Search for Azure Active Directory
3. Under Manage click on Add and select App registrations
4. Enter a display Name for your application
5. Select Personal Microsoft accounts
6. Choose Web and enter http://localhost/api
7. Click on register
```java
MicrosoftAuthenticator authenticator= new MicrosoftAuthenticator(clientSeec, "JYS7Q~kkgx6TmJR8KEL66j9a2LbLH8tkh5ooa");
authenticator.login();
```
## Notes
You will get a error when you paste this class in. I am not going to spoon feed you, but look at the hints your IDE gives you! It will tell you exactly what the issue is. Hint: It is with Minecraft.getMinecraft().session variable
