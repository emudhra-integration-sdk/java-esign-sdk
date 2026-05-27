# eSign Logging Configuration Guide

## Overview

The eSign SDK uses a **two-phase logging strategy** that works reliably on every platform — Android, Spring Boot, plain Java, and WebLogic — without any extra configuration.

### Phase 1 — Console logging (immediate, always works)
When `new eSign(...)` is called, a `ConsoleHandler` is attached right away.
- **Android**: output appears in **logcat** (tag `esign`)
- **Server / Desktop**: output goes to **stderr**

No file system access is required at this stage.

### Phase 2 — File logging (lazy, triggered by `getGatewayParameter`)
When `getGatewayParameter(...)` is called, the SDK writes the file to:

```
<tempFolder>/logs/eSign.log
```

`tempFolder` is the directory you already pass to `getGatewayParameter` — it is guaranteed to be writable by your app.  
Once the file handler is attached, the console handler is removed so there is no duplicate output.

If the file cannot be created for any reason, the console handler stays active and no log is silently lost.

---

## Log File Details

| Property | Value |
|---|---|
| **File name** | `eSign.log` |
| **Location** | `{tempFolder}/logs/eSign.log` |
| **Max size** | 10 MB per file |
| **Rotation** | Up to 100 backup files (`eSign.log.0`, `eSign.log.1`, …) |
| **Format** | `YYYY-MM-DD HH:mm:ss [LEVEL] [ClassName] Message` |
| **Time zone** | IST (Asia/Kolkata) |

---

## Log Levels

| `eSignSettings.LogType` | What is logged |
|---|---|
| `AllLog` *(default)* | Everything — INFO, WARNING, SEVERE |
| `NoDebugLog` | Warnings and errors only (WARNING + SEVERE) |
| `NoLog` | Logging completely disabled |

The log level is set in the `eSign` constructor. Constructors that do not include a `logType` parameter default to `AllLog`.

---

## Usage Examples

### Example 1 — Basic setup (default: `AllLog`)

```java
eSign esignObj = new eSign(
    "yourASPID",
    "https://esign.example.com/v1",
    "https://esign.example.com/v2",
    "path/to/certificate.pfx",
    "password",
    "alias"
);
// Log file will be created at:  <tempFolder>/logs/eSign.log
// when getGatewayParameter() is first called.
```

### Example 2 — Warnings and errors only (`NoDebugLog`)

```java
eSign esignObj = new eSign(
    "yourASPID",
    "https://esign.example.com/v1",
    "https://esign.example.com/v2",
    "path/to/certificate.pfx",
    "password",
    "alias",
    false,                              // proxyreq
    "",                                 // proxyIp
    0,                                  // proxyPort
    0,                                  // sessionTimeout
    eSignSettings.LogType.NoDebugLog,   // log level
    21000                               // SignatureContents
);
```

### Example 3 — Disable logging entirely (`NoLog`)

```java
eSign esignObj = new eSign(
    "yourASPID",
    "https://esign.example.com/v1",
    "https://esign.example.com/v2",
    "path/to/certificate.pfx",
    "password",
    "alias",
    false,
    "",
    0,
    0,
    eSignSettings.LogType.NoLog,        // disables all logging
    21000
);
```

### Example 4 — Full control with proxy

```java
eSign esignObj = new eSign(
    "yourASPID",
    "https://esign.example.com/v1",
    "https://esign.example.com/v2",
    "path/to/certificate.pfx",
    "password",
    "alias",
    true,                               // proxyreq
    "192.168.1.1",                      // proxyIp
    8080,                               // proxyPort
    30000,                              // sessionTimeout (ms)
    eSignSettings.LogType.AllLog,       // log level
    "proxyUser",                        // ProxyUserID
    "proxyPass",                        // ProxyUserPassword
    null,                               // pdfViewerLicence
    21000                               // SignatureContents
);
```

---

## Android — recommended setup

On Android, pass the app's internal files directory as `tempFolder`.  
No extra permissions are required.

```java
// In your Activity / ViewModel:
String tempFolder = getFilesDir().getAbsolutePath() + "/esign_temp";

eSign esignObj = new eSign(
    "yourASPID",
    "https://esign.example.com/v1",
    "https://esign.example.com/v2",
    "path/to/certificate.pfx",
    "password",
    "alias",
    false, "", 0, 0,
    eSignSettings.LogType.AllLog,
    21000
);

// Pass the same writable tempFolder to getGatewayParameter:
eSignServiceReturn result = esignObj.getGatewayParameter(
    inputs, signerID, transactionID,
    responseUrl, redirectUrl,
    tempFolder,                         // ← writable on Android; log file goes here
    eSign.eSignAPIVersion.V3,
    eSign.AuthMode.OTP,
    1440
);
// Log file is now at: <getFilesDir()>/esign_temp/logs/eSign.log
// Until getGatewayParameter is called, logs appear in logcat (tag: esign).
```

---

## Troubleshooting

### No log file created
- The file is only created on the **first call** to `getGatewayParameter`. Verify that call completes.
- Check that `tempFolder` exists and the app has write permission to it.
- If `LogType.NoLog` is set, logging is intentionally disabled.

### Seeing only console / logcat output, no file
- Expected until `getGatewayParameter` is called for the first time.
- After the call, look in `{tempFolder}/logs/eSign.log`.

### Exception details missing from error message
- All exception messages are now set with `e.toString()`, which includes the exception class and the message. Full stack traces are written to the log file.
