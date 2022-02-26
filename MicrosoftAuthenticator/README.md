# Session Changer
Login to your Minecraft account inside of Eclipse

## How to use
```java
MicrosoftAuthenticator authenticator= new MicrosoftAuthenticator(clientSeec, "JYS7Q~kkgx6TmJR8KEL66j9a2LbLH8tkh5ooa");
authenticator.login();
```
## Notes
You will get a error when you paste this class in. I am not going to spoon feed you, but look at the hints your IDE gives you! It will tell you exactly what the issue is. Hint: It is with Minecraft.getMinecraft().session variable
