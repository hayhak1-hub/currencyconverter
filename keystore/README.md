# Release signing

1. Copy `keystore.properties.example` to `keystore.properties`
2. Generate an upload keystore (once):

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v `
  -keystore currencyconverter-upload.jks -alias upload `
  -keyalg RSA -keysize 2048 -validity 10000
```

3. Fill `keystore.properties` with store file name and passwords (`storePassword` and `keyPassword` must match for PKCS12 keystores)
4. Build: `gradlew bundleRelease`

Files in this folder (except `.example` and this README) are **gitignored**. Keep a secure backup of the `.jks` file and passwords.
