# eSign Logging Configuration Guide

## Overview
The eSign library supports configurable logging with different log levels.

## Log Location

### Default Behavior
Logs will be created in:
```
<current_working_directory>/logs/eSign.log
```

## Usage Examples

### SIMPLE USAGE (Recommended)

### Example 1: Basic Setup with Default Logging
```java
eSign esignObj = new eSign(
    "yourASPID",
    "https://esign.example.com/v1",
    "https://esign.example.com/v2",
    "path/to/certificate.pfx",
    "password",
    "alias"
);
```

### Example 2: With SignatureContents
```java
eSign esignObj = new eSign(
    "yourASPID",
    "https://esign.example.com/v1",
    "https://esign.example.com/v2",
    "path/to/certificate.pfx",
    "password",
    "alias",
    21000                               // SignatureContents
);
```

### Example 3: With PDF Viewer License
```java
eSign esignObj = new eSign(
    "yourASPID",
    "https://esign.example.com/v1",
    "https://esign.example.com/v2",
    "path/to/certificate.pfx",
    "password",
    "alias",
    "pdfViewerLicense",                 // PDF Viewer License
    21000                               // SignatureContents
);
```

### Example 4: With Log Type Control
```java
eSign esignObj = new eSign(
    "yourASPID",
    "https://esign.example.com/v1",
    "https://esign.example.com/v2",
    "path/to/certificate.pfx",
    "password",
    "alias",
    false,           // proxyreq
    "",              // proxyIp
    0,               // proxyPort
    0,               // sessionTimeout
    eSignSettings.LogType.NoDebugLog,   // Only warnings/errors
    21000                               // SignatureContents
);
```

### Example 5: Disable Logging
```java
eSign esignObj = new eSign(
    "yourASPID",
    "https://esign.example.com/v1",
    "https://esign.example.com/v2",
    "path/to/certificate.pfx",
    "password",
    "alias",
    false,           // proxyreq
    "",              // proxyIp
    0,               // proxyPort
    0,               // sessionTimeout
    eSignSettings.LogType.NoLog,        // Disable logging
    21000                               // SignatureContents
);
```

---

## ADVANCED USAGE (Full Control)

### Example 6: Full Control with Proxy Settings
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
    300,                                // sessionTimeout
    eSignSettings.LogType.AllLog,       // logType
    "proxyUser",                        // ProxyUserID
    "proxyPass",                        // ProxyUserPassword
    "pdfViewerLicense",
    21000                               // SignatureContents
);
```

## Log Types

| LogType | Description | Log Levels |
|---------|-------------|------------|
| `AllLog` | All logs including INFO, WARNING, SEVERE | INFO + WARNING + SEVERE |
| `NoDebugLog` | Only important logs | WARNING + SEVERE |
| `NoLog` | Completely disable logging | None |

## Log File Details

- **File Name**: `eSign.log`
- **Location**: `<working_directory>/logs/eSign.log`
- **Max Size**: 10 MB per file
- **Rotation**: 100 backup files (eSign.log.0, eSign.log.1, etc.)
- **Format**: `YYYY-MM-DD HH:mm:ss [LEVEL] [ClassName] Message`
- **Time Zone**: IST (Asia/Kolkata)

## Log Output Example

```
2025-11-12 10:30:15	[INFO]	[eSignImplimentation]	getGatewayParameterPrivate() - Entry: transactionID=abc123, startTime=1731398415000
2025-11-12 10:30:15	[INFO]	[eSignImplimentation]	getGatewayParameterPrivate() - PDF base64 decoded in 45ms for document #1
2025-11-12 10:30:15	[INFO]	[eSignImplimentation]	getGatewayParameterPrivate() - PDF reader created in 120ms for document #1
2025-11-12 10:30:16	[INFO]	[eSignImplimentation]	getGatewayParameterPrivate() - Exit: Success, totalDuration=2985ms
```

## Important Notes

1. **Folder Creation**: The library automatically creates the log folder if it doesn't exist
2. **Permissions**: Ensure your application has write permissions to the log folder
3. **First Log**: When the eSign object is created, it prints the log file location to console
4. **NoSuchAlgorithmException**: All constructors throw `NoSuchAlgorithmException` which must be handled

## Troubleshooting

### Log File Not Found
- Look for console message: `eSign Log file location: <path>`
- Check if logging is enabled (not using `LogType.NoLog`)
- Verify disk space is available

### Constructor Parameters
- `ASPID`: Your Application Service Provider ID
- `eSignURL`: The eSign service URL (v1)
- `eSignURLV2`: The eSign service URL (v2)
- `pfxpath`: Path to your PFX certificate file
- `password`: Certificate password
- `pfxAlias`: Certificate alias
