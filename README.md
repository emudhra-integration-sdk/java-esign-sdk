# eSign Java SDK

A Java SDK for integrating eMudhra's Aadhaar-based and PAN-based eSign service into your applications. Implements **two-phase remote digital signing** — PDFs are pre-signed locally (SHA-256 hash computed), sent to eMudhra's eSign gateway for user authentication (OTP/Fingerprint/IRIS/Face), and the returned PKCS7 signature is injected back into the PDF.

## Important Notice

> **This repository is a reference implementation published by eMudhra as open source.**
>
> eMudhra publishes this source code to demonstrate how hash-based digital signing can be integrated with the eMudhra eSign API. **eMudhra's responsibility is limited to the eSign API and gateway services.** This code is provided as-is for the community to study, build, and integrate into their own applications.
>
> - eMudhra does **not** distribute or maintain a pre-built JAR from this repository.
> - You are expected to **build this code yourself** from source and integrate it into your application.
> - Any modifications, redistribution, or production use of this code are solely at your own discretion and responsibility, subject to the [AGPL-3.0 license](LICENSE).
> - For API access, gateway URLs, ASP ID, and PFX certificates, contact [eMudhra](https://www.emudhra.com).

[![Build](https://github.com/emudhra-integration-sdk/java-esign-sdk/actions/workflows/build.yml/badge.svg)](https://github.com/emudhra-integration-sdk/java-esign-sdk/actions/workflows/build.yml)

---

## Features

- **Aadhaar Signing (V2 API)** — OTP, Fingerprint, IRIS, and Face authentication
- **PAN Signing (V3 API)** — Username, Mobile, or PAN-based authentication
- **Multiple Signature Appearances** — Standard, Image, OneLiner, Advanced, ColoredGraphic, BackgroundImage
- **Multi-Document Signing** — Sign up to 5 documents in a single request
- **Hash-Based Signing** — Sign using pre-computed SHA-256 hashes without sending the full PDF
- **Flexible Placement** — Named coordinates, page-level coordinates, or content-search-based positioning
- **Co-Signing Support** — Add multiple signatures to the same document
- **Bank KYC** — Perform Bank KYC verification through eMudhra
- **Configurable Logging** — File-based logging with rotation and multiple log levels
- **Proxy Support** — HTTP proxy with optional authentication
- **Signature Appearance Patching** — Automatically updates the visual appearance of signed signature fields with the signer's name and masked Aadhaar number extracted from the gateway-returned certificate
- **LATEST: Customisable Aadhaar Appearance** — Define the signature block yourself with `AadhaarSignatureAppearance`: placeholder-based custom content (`{name}`, `{aadhaar}`, `{reason}`, `{location}`, `{date}`), an on/off switch for the Aadhaar number, per-field labels and ordering, date format and timezone, italic/bold, colour, size and margins
- **LATEST: Signer Certificate Details** — `eSignServiceReturn.getSignerCertificateInfo()` exposes the parsed gateway certificate: signer CN, Aadhaar number, issuer, serial, validity, algorithm, key size and SHA-256 thumbprint

## Prerequisites

- **Java 8** or higher
- **PFX certificate** (.pfx) provided by eMudhra for XML signing
- **ASP ID** (Application Service Provider ID) from eMudhra
- **eSign gateway URLs** (v1 and v2 endpoints) from eMudhra

## Quick Start

```java
import com.emudhra.esign.*;
import java.util.ArrayList;

// 1. Initialize the SDK
eSign esignObj = new eSign(
    "YOUR_ASP_ID",
    "https://esigngateway.emudhra.com/eSignRequest",
    "https://esigngateway.emudhra.com/v2/eSignRequest",
    "/path/to/certificate.pfx", "pfxPassword", "pfxAlias", 21000
);

// 2. Build the signing input
eSignInput input = eSignInputBuilder.init()
    .setDocBase64(pdfBase64)
    .setDocInfo("Contract Agreement")
    .setDocURL("https://yourapp.com/doc.pdf")
    .setSignedBy("John Doe")
    .setLocation("Bangalore")
    .setReason("Agreement Signing")
    .setAppearanceType(eSign.AppearanceType.StandardSignature)
    .setPageTobeSigned(eSign.PageTobeSigned.Last)
    .setCoordinates(eSign.Coordinates.BottomRight)
    .setCoSign(true)
    .build();

ArrayList<eSignInput> inputs = new ArrayList<>();
inputs.add(input);

// 3. Phase 1: Get the gateway parameter
eSignServiceReturn result = esignObj.getGatewayParameter(
    inputs, "", "TXN-" + System.currentTimeMillis(),
    "https://yourapp.com/callback", "https://yourapp.com/redirect",
    "/tmp/esign", eSign.eSignAPIVersion.V2, eSign.AuthMode.OTP
);

// 4. Redirect user to eMudhra for authentication
// 5. Phase 2: Handle callback and get signed document
```

See the [full Quick Start guide](documentation/QUICK_START.md) for the complete two-phase flow with detailed explanations.

## Documentation

| Guide | Description |
|-------|-------------|
| [Quick Start](documentation/QUICK_START.md) | SDK overview, signing flows, API reference, enums, and error codes |
| [Framework Integration](documentation/FRAMEWORK_INTEGRATION.md) | Ready-to-use examples for Spring Boot, Servlet, JSP, Struts, and plain Java |
| [Logging Configuration](documentation/LOGGING_USAGE.md) | Log levels, file location, rotation, and configuration examples |

## Building from Source

This repository does not provide a downloadable JAR. You are expected to build it yourself and embed it in your application.

This is an Apache Ant / NetBeans project targeting Java 8.

```bash
# Clean and build the JAR
ant clean jar

# Compile only
ant compile

# Clean build artifacts
ant clean
```

Output: `dist/eSignASPLibrary5_8.jar`

> The build badge above confirms the source compiles correctly on every commit. A passing build means the code is ready for you to compile and use.

## Dependencies

The SDK requires the following libraries (included in `lib/`):

| Library | Version | License |
|---------|---------|---------|
| [Apache Batik](https://xmlgraphics.apache.org/batik/) | 1.13 | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| [Apache Commons IO](https://commons.apache.org/proper/commons-io/) | 2.4 | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| [Apache XML Security](https://santuario.apache.org/) | 2.3.0 | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| [Apache XMLGraphics Commons](https://xmlgraphics.apache.org/commons/) | 2.4 | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| [Woodstox](https://github.com/FasterXML/woodstox) | 5.2.1 | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) |
| [Stax2 API](https://github.com/FasterXML/stax2-api) | 4.2 | [BSD 2-Clause](https://opensource.org/licenses/BSD-2-Clause) |
| [SLF4J](https://www.slf4j.org/) | 1.7.32 | [MIT](https://opensource.org/licenses/MIT) |
| [W3C SVG DOM](https://www.w3.org/Graphics/SVG/) | 1.1.0 | [W3C License](https://www.w3.org/Consortium/Legal/2015/copyright-software-and-document) |

The SDK also embeds repackaged versions of the following libraries:

| Library | Repackaged As | License |
|---------|---------------|---------|
| [iText](https://itextpdf.com/) | `esign.text.*` | [AGPL-3.0](https://www.gnu.org/licenses/agpl-3.0.html) |
| [Bouncy Castle](https://www.bouncycastle.org/) | `org.emcastle.*` | [MIT](https://opensource.org/licenses/MIT) |

See [NOTICE](NOTICE) for full third-party attribution details.

## Contributing

Contributions are welcome. Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -m 'Add your feature'`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

Please ensure your changes compile with `ant clean jar` before submitting.

## License

This project is licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0)** — see the [LICENSE](LICENSE) file for details.

This license is required due to the inclusion of iText (repackaged as `esign.text.*`), which is distributed under AGPL-3.0. If you require a different license for commercial or proprietary use, you must obtain a commercial license from [iText](https://itextpdf.com/pricing).

### Third-Party Licenses

This project includes third-party libraries under various open-source licenses. See [NOTICE](NOTICE) for complete attribution and license details.
