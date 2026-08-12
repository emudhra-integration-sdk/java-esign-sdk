# Framework Integration Guide

This guide shows how to integrate the eSign Java SDK into common Java web frameworks. Each example covers the full flow: initializing the SDK, starting the signing process, redirecting to eMudhra, handling the callback, and retrieving the signed document.

> **Prerequisites:** Read [QUICK_START.md](QUICK_START.md) first for SDK concepts and API details.

## Table of Contents

- [Spring Boot](#spring-boot)
- [Plain Servlet](#plain-servlet)
- [JSP](#jsp)
- [Struts](#struts)
- [Plain Java (Console)](#plain-java-console)
- [Encrypted Aadhaar Flow — Spring Boot Example](#encrypted-aadhaar-flow--spring-boot-example)
- [Common Patterns](#common-patterns)

---

## Spring Boot

### Dependencies

Add the SDK JAR and dependencies to your project. If using Maven with local JARs:

```xml
<dependency>
    <groupId>com.emudhra</groupId>
    <artifactId>eSignASPLibrary</artifactId>
    <version>5.5</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/lib/eSignASPLibrary5_8.jar</systemPath>
</dependency>
<!-- Add all JARs from lib/ similarly, or copy them to a shared lib folder -->
```

Or simply place all JARs in a `lib/` folder and add to classpath in your build config.

### Configuration

**application.properties:**

```properties
esign.asp-id=YOUR_ASP_ID
esign.url=https://esigngateway.emudhra.com/eSignRequest
esign.url-v2=https://esigngateway.emudhra.com/v2/eSignRequest
esign.pfx-path=/path/to/certificate.pfx
esign.pfx-password=pfxPassword
esign.pfx-alias=pfxAlias
esign.signature-contents=21000
esign.temp-folder=/tmp/esign
esign.emudhra-aadhaar-url=https://authenticate.sandbox.emudhra.com/AadhaareSign.jsp
esign.emudhra-pan-url=https://authenticate.sandbox.emudhra.com/index.jsp
```

### Bean Configuration

```java
import com.emudhra.esign.eSign;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ESignConfig {

    @Value("${esign.asp-id}") private String aspId;
    @Value("${esign.url}") private String esignUrl;
    @Value("${esign.url-v2}") private String esignUrlV2;
    @Value("${esign.pfx-path}") private String pfxPath;
    @Value("${esign.pfx-password}") private String pfxPassword;
    @Value("${esign.pfx-alias}") private String pfxAlias;
    @Value("${esign.signature-contents}") private int signatureContents;

    @Bean
    public eSign esignClient() throws Exception {
        return new eSign(aspId, esignUrl, esignUrlV2,
                         pfxPath, pfxPassword, pfxAlias, signatureContents);
    }
}
```

### REST Controller

```java
import com.emudhra.esign.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/esign")
public class ESignController {

    @Autowired private eSign esignClient;
    @Value("${esign.temp-folder}") private String tempFolder;
    @Value("${esign.emudhra-aadhaar-url}") private String aadhaarUrl;
    @Value("${esign.emudhra-pan-url}") private String panUrl;

    /**
     * Phase 1: Initiate signing. Returns the gateway parameter and redirect URL.
     */
    @PostMapping("/initiate")
    public Map<String, String> initiateSigning(
            @RequestParam String pdfBase64,
            @RequestParam String signerName,
            @RequestParam(defaultValue = "V2") String apiVersion,
            HttpSession session) {

        eSignInput input = eSignInputBuilder.init()
            .setDocBase64(pdfBase64)
            .setDocInfo("Document")
            .setDocURL("https://yourapp.com/doc")
            .setAppearanceType(eSign.AppearanceType.StandardSignature)
            .setSignedBy(signerName)
            .setLocation("India")
            .setReason("Digital Signing")
            .setPageTobeSigned(eSign.PageTobeSigned.Last)
            .setCoordinates(eSign.Coordinates.BottomRight)
            .setCoSign(true)
            .setBorderRequired(true)
            .build();

        ArrayList<eSignInput> inputs = new ArrayList<>();
        inputs.add(input);

        String txnId = "TXN-" + System.currentTimeMillis();
        // V2 = Aadhaar signing, V3 = PAN signing
        eSign.eSignAPIVersion version = apiVersion.equals("V3")
            ? eSign.eSignAPIVersion.V3 : eSign.eSignAPIVersion.V2;

        String responseUrl = "https://yourapp.com/api/esign/callback/aadhaar";  // for V2/Aadhaar
        String redirectUrl = "https://yourapp.com/api/esign/callback/pan";      // for V3/PAN

        eSignServiceReturn result = esignClient.getGatewayParameter(
            inputs, "", txnId, responseUrl, redirectUrl,
            tempFolder, version, eSign.AuthMode.OTP
        );

        Map<String, String> response = new HashMap<>();
        if (result.getStatus() == 1) {
            // Store temp file path in session for Phase 2
            session.setAttribute("preSignedTempFile", result.getPreSignedTempFile());
            session.setAttribute("txnId", txnId);

            response.put("status", "success");
            response.put("gatewayParameter", result.getGatewayParameter());
            response.put("redirectUrl", version == eSign.eSignAPIVersion.V2
                ? aadhaarUrl : panUrl);
        } else {
            response.put("status", "error");
            response.put("errorCode", result.getErrorCode());
            response.put("errorMessage", result.getErrorMessage());
        }
        return response;
    }

    /**
     * Phase 2 (Aadhaar/V2): eMudhra POSTs txnref and XML to this endpoint.
     */
    @PostMapping("/callback/aadhaar")
    public String aadhaarCallback(
            @RequestParam("XML") String responseXML,
            @RequestParam("txnref") String txnref,
            HttpSession session) {

        String tempFile = (String) session.getAttribute("preSignedTempFile");
        eSignServiceReturn result = esignClient.getSigedDocument(responseXML, tempFile);

        if (result.getStatus() == 1) {
            for (ReturnDocument doc : result.getReturnDocuments()) {
                String signedPdfBase64 = doc.getSignedDocument();
                // Save or return the signed PDF
            }
            return "Signing successful";
        }
        return "Signing failed: " + result.getErrorMessage();
    }

    /**
     * Phase 2 (PAN/V3): eMudhra redirects user here with txnref query param.
     */
    @GetMapping("/callback/pan")
    public String panCallback(
            @RequestParam("txnref") String txnrefBase64,
            HttpSession session) {

        String decoded = new String(Base64.getDecoder().decode(txnrefBase64));
        String[] parts = decoded.split("\\|");
        String txn = parts[0];

        eSignServiceReturn statusResult = esignClient.getStatus(txn);

        if (statusResult.getStatus() == 1) {
            String tempFile = tempFolder + "/" + txn + ".sig";
            eSignServiceReturn signResult = esignClient.getSigedDocument(
                statusResult.getResponseXML(), tempFile
            );

            if (signResult.getStatus() == 1) {
                for (ReturnDocument doc : signResult.getReturnDocuments()) {
                    String signedPdfBase64 = doc.getSignedDocument();
                    // Save or return the signed PDF
                }
                return "Signing successful";
            }
            return "Signing failed: " + signResult.getErrorMessage();
        }
        return "Status check failed: " + statusResult.getErrorMessage();
    }
}
```

### Frontend (HTML form for redirect)

After your `/initiate` endpoint returns the gateway parameter, submit it via a form:

```html
<form id="esignForm" method="POST" action="">
    <input type="hidden" name="txnref" id="txnref" />
</form>

<script>
fetch('/api/esign/initiate', { method: 'POST', body: formData })
    .then(r => r.json())
    .then(data => {
        if (data.status === 'success') {
            document.getElementById('txnref').value = data.gatewayParameter;
            document.getElementById('esignForm').action = data.redirectUrl;
            document.getElementById('esignForm').submit();
        }
    });
</script>
```

---

## Plain Servlet

### web.xml

```xml
<web-app>
    <servlet>
        <servlet-name>InitiateSigningServlet</servlet-name>
        <servlet-class>com.yourapp.InitiateSigningServlet</servlet-class>
    </servlet>
    <servlet>
        <servlet-name>AadhaarCallbackServlet</servlet-name>
        <servlet-class>com.yourapp.AadhaarCallbackServlet</servlet-class>
    </servlet>
    <servlet>
        <servlet-name>PanCallbackServlet</servlet-name>
        <servlet-class>com.yourapp.PanCallbackServlet</servlet-class>
    </servlet>

    <servlet-mapping>
        <servlet-name>InitiateSigningServlet</servlet-name>
        <url-pattern>/esign/initiate</url-pattern>
    </servlet-mapping>
    <servlet-mapping>
        <servlet-name>AadhaarCallbackServlet</servlet-name>
        <url-pattern>/esign/callback/aadhaar</url-pattern>
    </servlet-mapping>
    <servlet-mapping>
        <servlet-name>PanCallbackServlet</servlet-name>
        <url-pattern>/esign/callback/pan</url-pattern>
    </servlet-mapping>
</web-app>
```

### InitiateSigningServlet.java

```java
import com.emudhra.esign.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.ArrayList;

public class InitiateSigningServlet extends HttpServlet {

    private eSign esignClient;

    @Override
    public void init() {
        try {
            esignClient = new eSign(
                "YOUR_ASP_ID",
                "https://esigngateway.emudhra.com/eSignRequest",
                "https://esigngateway.emudhra.com/v2/eSignRequest",
                "/path/to/certificate.pfx", "pfxPassword", "pfxAlias", 21000
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize eSign", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String pdfBase64 = request.getParameter("pdfBase64");
        String signerName = request.getParameter("signerName");

        eSignInput input = eSignInputBuilder.init()
            .setDocBase64(pdfBase64)
            .setDocInfo("Document")
            .setDocURL("https://yourapp.com/doc")
            .setAppearanceType(eSign.AppearanceType.StandardSignature)
            .setSignedBy(signerName)
            .setLocation("India")
            .setReason("Signing")
            .setPageTobeSigned(eSign.PageTobeSigned.Last)
            .setCoordinates(eSign.Coordinates.BottomRight)
            .setCoSign(true)
            .build();

        ArrayList<eSignInput> inputs = new ArrayList<>();
        inputs.add(input);

        String txnId = "TXN-" + System.currentTimeMillis();

        eSignServiceReturn result = esignClient.getGatewayParameter(
            inputs, "", txnId,
            "https://yourapp.com/esign/callback/aadhaar",
            "https://yourapp.com/esign/callback/pan",
            "/tmp/esign",
            eSign.eSignAPIVersion.V2,       // V2 for Aadhaar signing
            eSign.AuthMode.OTP
        );

        if (result.getStatus() == 1) {
            HttpSession session = request.getSession();
            session.setAttribute("preSignedTempFile", result.getPreSignedTempFile());

            // Render an auto-submit form that POSTs to eMudhra
            response.setContentType("text/html");
            response.getWriter().write(
                "<html><body onload=\"document.forms[0].submit()\">" +
                "<form method=\"POST\" action=\"https://authenticate.sandbox.emudhra.com/AadhaareSign.jsp\">" +
                "<input type=\"hidden\" name=\"txnref\" value=\"" + result.getGatewayParameter() + "\" />" +
                "</form></body></html>"
            );
        } else {
            response.sendError(500, result.getErrorMessage());
        }
    }
}
```

### AadhaarCallbackServlet.java

```java
import com.emudhra.esign.*;
import javax.servlet.http.*;
import java.io.IOException;

public class AadhaarCallbackServlet extends HttpServlet {

    private eSign esignClient;

    @Override
    public void init() {
        try {
            esignClient = new eSign(
                "YOUR_ASP_ID",
                "https://esigngateway.emudhra.com/eSignRequest",
                "https://esigngateway.emudhra.com/v2/eSignRequest",
                "/path/to/certificate.pfx", "pfxPassword", "pfxAlias", 21000
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize eSign", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String responseXML = request.getParameter("XML");
        String tempFile = (String) request.getSession().getAttribute("preSignedTempFile");

        eSignServiceReturn result = esignClient.getSigedDocument(responseXML, tempFile);

        if (result.getStatus() == 1) {
            for (ReturnDocument doc : result.getReturnDocuments()) {
                String signedPdfBase64 = doc.getSignedDocument();
                // Save signed PDF or send to client
            }
            response.getWriter().write("Document signed successfully");
        } else {
            response.getWriter().write("Signing failed: " + result.getErrorMessage());
        }
    }
}
```

### PanCallbackServlet.java

```java
import com.emudhra.esign.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Base64;

public class PanCallbackServlet extends HttpServlet {

    private eSign esignClient;
    private static final String TEMP_FOLDER = "/tmp/esign";

    @Override
    public void init() {
        try {
            esignClient = new eSign(
                "YOUR_ASP_ID",
                "https://esigngateway.emudhra.com/eSignRequest",
                "https://esigngateway.emudhra.com/v2/eSignRequest",
                "/path/to/certificate.pfx", "pfxPassword", "pfxAlias", 21000
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize eSign", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String txnrefBase64 = request.getParameter("txnref");
        String decoded = new String(Base64.getDecoder().decode(txnrefBase64));
        String[] parts = decoded.split("\\|");
        String txn = parts[0];

        eSignServiceReturn statusResult = esignClient.getStatus(txn);

        if (statusResult.getStatus() == 1) {
            String tempFile = TEMP_FOLDER + "/" + txn + ".sig";
            eSignServiceReturn signResult = esignClient.getSigedDocument(
                statusResult.getResponseXML(), tempFile
            );

            if (signResult.getStatus() == 1) {
                for (ReturnDocument doc : signResult.getReturnDocuments()) {
                    String signedPdfBase64 = doc.getSignedDocument();
                    // Save or return signed PDF
                }
                response.getWriter().write("Document signed successfully");
            } else {
                response.getWriter().write("Signing failed: " + signResult.getErrorMessage());
            }
        } else {
            response.getWriter().write("Status check failed: " + statusResult.getErrorMessage());
        }
    }
}
```

---

## JSP

### initiate.jsp

Collects user input and calls the SDK to start signing:

```jsp
<%@ page import="com.emudhra.esign.*" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.io.*, java.nio.file.*" %>
<%
    if ("POST".equalsIgnoreCase(request.getMethod())) {
        String pdfBase64 = request.getParameter("pdfBase64");
        String signerName = request.getParameter("signerName");

        eSign esignClient = new eSign(
            "YOUR_ASP_ID",
            "https://esigngateway.emudhra.com/eSignRequest",
            "https://esigngateway.emudhra.com/v2/eSignRequest",
            "/path/to/certificate.pfx", "pfxPassword", "pfxAlias", 21000
        );

        eSignInput input = eSignInputBuilder.init()
            .setDocBase64(pdfBase64)
            .setDocInfo("Document")
            .setDocURL("https://yourapp.com/doc")
            .setAppearanceType(eSign.AppearanceType.StandardSignature)
            .setSignedBy(signerName)
            .setLocation("India")
            .setReason("Signing")
            .setPageTobeSigned(eSign.PageTobeSigned.Last)
            .setCoordinates(eSign.Coordinates.BottomRight)
            .setCoSign(true)
            .build();

        ArrayList<eSignInput> inputs = new ArrayList<>();
        inputs.add(input);

        String txnId = "TXN-" + System.currentTimeMillis();
        eSignServiceReturn result = esignClient.getGatewayParameter(
            inputs, "", txnId,
            "https://yourapp.com/callback.jsp",
            "https://yourapp.com/pancallback.jsp",
            "/tmp/esign",
            eSign.eSignAPIVersion.V2,       // V2 for Aadhaar signing
            eSign.AuthMode.OTP
        );

        if (result.getStatus() == 1) {
            session.setAttribute("preSignedTempFile", result.getPreSignedTempFile());
            // Forward to redirect page
            request.setAttribute("gatewayParam", result.getGatewayParameter());
            request.getRequestDispatcher("redirect.jsp").forward(request, response);
            return;
        } else {
            request.setAttribute("error", result.getErrorMessage());
        }
    }
%>
<html>
<body>
    <h2>eSign Document</h2>
    <% if (request.getAttribute("error") != null) { %>
        <p style="color:red"><%= request.getAttribute("error") %></p>
    <% } %>
    <form method="POST">
        <label>PDF (Base64):</label><br/>
        <textarea name="pdfBase64" rows="5" cols="60"></textarea><br/>
        <label>Signer Name:</label><br/>
        <input type="text" name="signerName" /><br/><br/>
        <button type="submit">Sign Document</button>
    </form>
</body>
</html>
```

### redirect.jsp

Auto-submits the form to eMudhra:

```jsp
<html>
<body onload="document.forms[0].submit()">
    <p>Redirecting to eMudhra for authentication...</p>
    <form method="POST" action="https://authenticate.sandbox.emudhra.com/AadhaareSign.jsp">
        <input type="hidden" name="txnref" value="<%= request.getAttribute("gatewayParam") %>" />
        <noscript><button type="submit">Click here if not redirected</button></noscript>
    </form>
</body>
</html>
```

### callback.jsp (Aadhaar/V3)

Handles the POST callback from eMudhra:

```jsp
<%@ page import="com.emudhra.esign.*" %>
<%
    String responseXML = request.getParameter("XML");
    String tempFile = (String) session.getAttribute("preSignedTempFile");

    eSign esignClient = new eSign(
        "YOUR_ASP_ID",
        "https://esigngateway.emudhra.com/eSignRequest",
        "https://esigngateway.emudhra.com/v2/eSignRequest",
        "/path/to/certificate.pfx", "pfxPassword", "pfxAlias", 21000
    );

    eSignServiceReturn result = esignClient.getSigedDocument(responseXML, tempFile);
%>
<html>
<body>
<% if (result.getStatus() == 1) { %>
    <h2>Document Signed Successfully</h2>
    <% for (ReturnDocument doc : result.getReturnDocuments()) { %>
        <p>Document ID: <%= doc.getDocId() %></p>
        <!-- doc.getSignedDocument() contains the Base64 signed PDF -->
    <% } %>
<% } else { %>
    <h2>Signing Failed</h2>
    <p>Error: <%= result.getErrorMessage() %> (<%= result.getErrorCode() %>)</p>
<% } %>
</body>
</html>
```

### pancallback.jsp (PAN/V2)

Handles the redirect callback from eMudhra:

```jsp
<%@ page import="com.emudhra.esign.*" %>
<%@ page import="java.util.Base64" %>
<%
    String txnrefBase64 = request.getParameter("txnref");
    String decoded = new String(Base64.getDecoder().decode(txnrefBase64));
    String[] parts = decoded.split("\\|");
    String txn = parts[0];

    eSign esignClient = new eSign(
        "YOUR_ASP_ID",
        "https://esigngateway.emudhra.com/eSignRequest",
        "https://esigngateway.emudhra.com/v2/eSignRequest",
        "/path/to/certificate.pfx", "pfxPassword", "pfxAlias", 21000
    );

    eSignServiceReturn statusResult = esignClient.getStatus(txn);
    eSignServiceReturn signResult = null;

    if (statusResult.getStatus() == 1) {
        String tempFile = "/tmp/esign/" + txn + ".sig";
        signResult = esignClient.getSigedDocument(statusResult.getResponseXML(), tempFile);
    }
%>
<html>
<body>
<% if (signResult != null && signResult.getStatus() == 1) { %>
    <h2>Document Signed Successfully</h2>
    <% for (ReturnDocument doc : signResult.getReturnDocuments()) { %>
        <p>Document ID: <%= doc.getDocId() %></p>
    <% } %>
<% } else { %>
    <h2>Signing Failed</h2>
    <p>Error: <%= statusResult.getErrorMessage() %></p>
<% } %>
</body>
</html>
```

---

## Struts

### struts.xml

```xml
<struts>
    <package name="esign" namespace="/esign" extends="struts-default">
        <action name="initiate" class="com.yourapp.action.InitiateSigningAction">
            <result name="redirect">/WEB-INF/views/esign-redirect.jsp</result>
            <result name="error">/WEB-INF/views/esign-error.jsp</result>
        </action>
        <action name="callback" class="com.yourapp.action.CallbackAction">
            <result name="success">/WEB-INF/views/esign-success.jsp</result>
            <result name="error">/WEB-INF/views/esign-error.jsp</result>
        </action>
    </package>
</struts>
```

### InitiateSigningAction.java

```java
import com.emudhra.esign.*;
import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.interceptor.SessionAware;
import java.util.ArrayList;
import java.util.Map;

public class InitiateSigningAction extends ActionSupport implements SessionAware {

    private String pdfBase64;
    private String signerName;
    private String gatewayParam;
    private String errorMessage;
    private Map<String, Object> sessionMap;

    @Override
    public String execute() {
        try {
            eSign esignClient = new eSign(
                "YOUR_ASP_ID",
                "https://esigngateway.emudhra.com/eSignRequest",
                "https://esigngateway.emudhra.com/v2/eSignRequest",
                "/path/to/certificate.pfx", "pfxPassword", "pfxAlias", 21000
            );

            eSignInput input = eSignInputBuilder.init()
                .setDocBase64(pdfBase64)
                .setDocInfo("Document")
                .setDocURL("https://yourapp.com/doc")
                .setAppearanceType(eSign.AppearanceType.StandardSignature)
                .setSignedBy(signerName)
                .setLocation("India")
                .setReason("Signing")
                .setPageTobeSigned(eSign.PageTobeSigned.Last)
                .setCoordinates(eSign.Coordinates.BottomRight)
                .setCoSign(true)
                .build();

            ArrayList<eSignInput> inputs = new ArrayList<>();
            inputs.add(input);

            String txnId = "TXN-" + System.currentTimeMillis();

            eSignServiceReturn result = esignClient.getGatewayParameter(
                inputs, "", txnId,
                "https://yourapp.com/esign/callback",
                "https://yourapp.com/esign/callback",
                "/tmp/esign",
                eSign.eSignAPIVersion.V2,       // V2 for Aadhaar signing
                eSign.AuthMode.OTP
            );

            if (result.getStatus() == 1) {
                gatewayParam = result.getGatewayParameter();
                sessionMap.put("preSignedTempFile", result.getPreSignedTempFile());
                return "redirect";
            } else {
                errorMessage = result.getErrorMessage();
                return ERROR;
            }
        } catch (Exception e) {
            errorMessage = e.getMessage();
            return ERROR;
        }
    }

    // Getters and setters
    public void setPdfBase64(String pdfBase64) { this.pdfBase64 = pdfBase64; }
    public void setSignerName(String signerName) { this.signerName = signerName; }
    public String getGatewayParam() { return gatewayParam; }
    public String getErrorMessage() { return errorMessage; }
    @Override
    public void setSession(Map<String, Object> session) { this.sessionMap = session; }
}
```

### CallbackAction.java

```java
import com.emudhra.esign.*;
import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.interceptor.SessionAware;
import java.util.Map;

public class CallbackAction extends ActionSupport implements SessionAware {

    private String XML;           // eMudhra POSTs this parameter
    private String signedPdfBase64;
    private String errorMessage;
    private Map<String, Object> sessionMap;

    @Override
    public String execute() {
        try {
            eSign esignClient = new eSign(
                "YOUR_ASP_ID",
                "https://esigngateway.emudhra.com/eSignRequest",
                "https://esigngateway.emudhra.com/v2/eSignRequest",
                "/path/to/certificate.pfx", "pfxPassword", "pfxAlias", 21000
            );

            String tempFile = (String) sessionMap.get("preSignedTempFile");
            eSignServiceReturn result = esignClient.getSigedDocument(XML, tempFile);

            if (result.getStatus() == 1 && result.getReturnDocuments() != null) {
                signedPdfBase64 = result.getReturnDocuments().get(0).getSignedDocument();
                return SUCCESS;
            } else {
                errorMessage = result.getErrorMessage();
                return ERROR;
            }
        } catch (Exception e) {
            errorMessage = e.getMessage();
            return ERROR;
        }
    }

    // Getters and setters
    public void setXML(String XML) { this.XML = XML; }
    public String getSignedPdfBase64() { return signedPdfBase64; }
    public String getErrorMessage() { return errorMessage; }
    @Override
    public void setSession(Map<String, Object> session) { this.sessionMap = session; }
}
```

### esign-redirect.jsp

```jsp
<html>
<body onload="document.forms[0].submit()">
    <p>Redirecting to eMudhra...</p>
    <form method="POST" action="https://authenticate.sandbox.emudhra.com/AadhaareSign.jsp">
        <input type="hidden" name="txnref"
               value="<s:property value='gatewayParam' />" />
        <noscript><button type="submit">Click here</button></noscript>
    </form>
</body>
</html>
```

---

## Plain Java (Console)

For testing and debugging, use a standalone Java application. Note that Phase 2 (callback from eMudhra) requires a web server, so this example only demonstrates Phase 1.

```java
import com.emudhra.esign.*;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;

public class ESignTest {
    public static void main(String[] args) throws Exception {

        // 1. Initialize
        eSign esignClient = new eSign(
            "YOUR_ASP_ID",
            "https://esigngateway.emudhra.com/eSignRequest",
            "https://esigngateway.emudhra.com/v2/eSignRequest",
            "/path/to/certificate.pfx",
            "pfxPassword",
            "pfxAlias",
            21000
        );

        // 2. Read and encode PDF
        byte[] pdfBytes = Files.readAllBytes(new File("/path/to/document.pdf").toPath());
        String pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);

        // 3. Build input
        eSignInput input = eSignInputBuilder.init()
            .setDocBase64(pdfBase64)
            .setDocInfo("Test Document")
            .setDocURL("https://example.com/doc.pdf")
            .setAppearanceType(eSign.AppearanceType.StandardSignature)
            .setSignedBy("Test User")
            .setLocation("Bangalore")
            .setReason("Testing")
            .setPageTobeSigned(eSign.PageTobeSigned.Last)
            .setCoordinates(eSign.Coordinates.BottomRight)
            .setCoSign(true)
            .setBorderRequired(true)
            .build();

        ArrayList<eSignInput> inputs = new ArrayList<>();
        inputs.add(input);

        // 4. Phase 1: Get gateway parameter
        eSignServiceReturn result = esignClient.getGatewayParameter(
            inputs, "", "TEST-" + System.currentTimeMillis(),
            "https://yourapp.com/callback",
            "https://yourapp.com/redirect",
            "/tmp/esign",
            eSign.eSignAPIVersion.V2,       // V2 for Aadhaar signing
            eSign.AuthMode.OTP
        );

        // 5. Check result
        System.out.println("Status: " + result.getStatus());
        if (result.getStatus() == 1) {
            System.out.println("Gateway Parameter: " + result.getGatewayParameter());
            System.out.println("Temp File: " + result.getPreSignedTempFile());
            System.out.println("Transaction ID: " + result.getTransactionID());
            System.out.println("\nRedirect user to eMudhra with this gateway parameter.");
            System.out.println("POST txnref=" + result.getGatewayParameter());
            System.out.println("  to https://authenticate.sandbox.emudhra.com/AadhaareSign.jsp");
        } else {
            System.out.println("Error Code: " + result.getErrorCode());
            System.out.println("Error Message: " + result.getErrorMessage());
        }

        // 6. Validate a PDF
        eSignServiceReturn validResult = esignClient.isValidPdf(pdfBase64);
        System.out.println("\nPDF Valid: " + (validResult.getStatus() == 1));

        // 7. Phase 2 (simulated - requires actual eMudhra response)
        // String responseXML = "..."; // from eMudhra callback
        // eSignServiceReturn signResult = esignClient.getSigedDocument(
        //     responseXML, result.getPreSignedTempFile()
        // );
    }
}
```

---

## Common Patterns

### Thread Safety

The `eSign` constructor sets static fields on `eSignSettings` (ASP ID, URLs, proxy config). This means:

- **Do not create multiple `eSign` instances with different configurations** in the same JVM. The last instance's settings will overwrite previous ones.
- Create a **single `eSign` instance** and reuse it across requests (e.g., as a Spring `@Bean` or servlet instance variable).
- The `getGatewayParameter()` and `getSigedDocument()` methods create new `eSignImplimentation` instances per call, so they are safe to call concurrently.

### Storing Pre-Signed Temp File Path Between Requests

Phase 1 and Phase 2 happen in separate HTTP requests. You must store the `preSignedTempFile` path between them:

```java
// Phase 1: Store in session
session.setAttribute("preSignedTempFile", result.getPreSignedTempFile());

// Phase 2: Retrieve from session
String tempFile = (String) session.getAttribute("preSignedTempFile");
```

For the PAN/V2 flow, the temp file follows the pattern `{tempFolder}/{transactionID}.sig`, so you can reconstruct it from the transaction ID.

### Temp File Cleanup

Pre-signed `.sig` files are created in your temp folder. Clean them up after signing is complete:

```java
// After successful Phase 2
File tempFile = new File(result.getPreSignedTempFile());
if (tempFile.exists()) {
    tempFile.delete();
}
```

Consider a scheduled cleanup job for abandoned transactions.

### Error Handling

Always check the status before accessing results:

```java
eSignServiceReturn result = esignClient.getGatewayParameter(...);

if (result.getStatus() == 0) {
    String errorCode = result.getErrorCode();    // e.g., "ESS-100"
    String errorMsg = result.getErrorMessage();   // human-readable message
    // Log and handle the error
    return;
}

// Safe to use result.getGatewayParameter(), etc.
```

For Phase 2, also check per-document errors:

```java
for (ReturnDocument doc : result.getReturnDocuments()) {
    if (doc.getStatus() == 0) {
        System.err.println("Document " + doc.getDocId() + " failed: " + doc.getErrorMessage());
        continue;
    }
    String signedPdf = doc.getSignedDocument();
}
```

### Positioning Text Inside the Signature Box

By default text is drawn from the top-left corner of the signature box. Use `setTextContentPosition` to change the anchor point. Works with `StandardSignature`, `OneLiner`, and the Aadhaar patch appearance:

```java
eSignInput input = eSignInputBuilder.init()
    .setDocBase64(pdfBase64)
    .setDocInfo("Agreement")
    .setAppearanceType(eSign.AppearanceType.StandardSignature)
    .setPageTobeSigned(eSign.PageTobeSigned.Last)
    .setCoordinates(eSign.Coordinates.BottomRight)
    .setTextContentPosition(eSign.Coordinates.BottomLeft)  // text anchored at bottom-left
    .build();
```

Available positions: `TopLeft` (default), `TopMiddle`, `TopRight`, `CenterLeft`, `CenterMiddle`, `CenterRight`, `BottomLeft`, `BottomMiddle`, `BottomRight`. No Phase 2 changes are needed — the setting is stored in the temp file and applied automatically.

### Password-Protected PDFs

If the PDF is password-protected, provide the password via the builder:

```java
eSignInput input = eSignInputBuilder.init()
    .setDocBase64(pdfBase64)
    .setPdfPassword("pdfOpenPassword")
    // ... other settings
    .build();
```

---

## Encrypted Aadhaar Flow — Spring Boot Example

This example shows how to integrate the Encrypted Aadhaar flow into a Spring Boot application. In this flow the signer's Aadhaar number is encrypted by the SDK and sent pre-filled to the gateway — the user only needs to authenticate (OTP/Fingerprint/IRIS), not type their Aadhaar.

> See [Encrypted Aadhaar Flow](QUICK_START.md#encrypted-aadhaar-flow) in QUICK_START.md for a full concept explanation.

### application.properties additions

```properties
# Path to UIDAI public-key CER file, or supply as base64 in code
esign.uidai-cer-path=/path/to/uidai-public.cer
```

### Controller

```java
import com.emudhra.esign.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.UUID;

@Controller
@RequestMapping("/esign/encrypted-aadhaar")
public class EncryptedAadhaarSignController {

    @Autowired
    private eSign esignObj;

    @Value("${esign.temp-folder}")
    private String tempFolder;

    @Value("${esign.emudhra-aadhaar-url}")
    private String aadhaarGatewayUrl;

    @Value("${esign.uidai-cer-path}")
    private String uidaiCerPath;

    /**
     * Phase 1: build the encrypted Aadhaar request and redirect to eMudhra gateway.
     *
     * The caller must supply the signer's Aadhaar number (from a prior KYC step)
     * and the base64-encoded PDF to sign.
     */
    @PostMapping("/initiate")
    public String initiate(
            @RequestParam("aadhaarNumber") String aadhaarNumber,
            @RequestParam("pdfBase64") String pdfBase64,
            HttpSession session,
            org.springframework.ui.Model model) {

        // Configure the encrypted Aadhaar details
        EncryptedAadhaarConfig aadhaarConfig = new EncryptedAadhaarConfig();
        aadhaarConfig.setAadhaarNumber(aadhaarNumber);
        aadhaarConfig.setCerFilePath(uidaiCerPath);

        // Build the signing input
        eSignInput input = eSignInputBuilder.init()
            .setDocBase64(pdfBase64)
            .setDocInfo("Loan Agreement")
            .setDocURL("https://yourapp.com/docs/agreement.pdf")
            .setSignedBy("") // gateway fills in signer name from Aadhaar record
            .setLocation("India")
            .setReason("Agreement Signing")
            .setAppearanceType(eSign.AppearanceType.StandardSignature)
            .setPageTobeSigned(eSign.PageTobeSigned.Last)
            .setCoordinates(eSign.Coordinates.BottomRight)
            .setCoSign(true)
            .setEncryptedAadhaarFlowEnabled(true)    // must be true to activate encrypted flow
            .setEncryptedAadhaarConfig(aadhaarConfig)
            .build();

        ArrayList<eSignInput> inputs = new ArrayList<>();
        inputs.add(input); // only 1 document per transaction in this flow

        String transactionId = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        eSignServiceReturn result = esignObj.getGatewayParameter(
            inputs,
            "",
            transactionId,
            "https://yourapp.com/esign/encrypted-aadhaar/callback",
            "https://yourapp.com/esign/encrypted-aadhaar/redirect",
            tempFolder,
            eSign.eSignAPIVersion.V2,
            eSign.AuthMode.OTP
        );

        if (result.getStatus() != 1) {
            model.addAttribute("error", result.getErrorCode() + ": " + result.getErrorMessage());
            return "esign-error";
        }

        // Store temp file path in session — needed when the callback arrives
        session.setAttribute("encAadhaarTempFile", result.getPreSignedTempFile());
        session.setAttribute("encAadhaarTxnId", result.getTransactionID());

        // Redirect user to eMudhra gateway
        model.addAttribute("gatewayUrl", aadhaarGatewayUrl);
        model.addAttribute("gatewayParam", result.getGatewayParameter());
        return "esign-redirect"; // renders a page with a POST form to the gateway
    }

    /**
     * Phase 2: eMudhra POSTs the signed response here.
     * Retrieve the signed PDF and process it.
     */
    @PostMapping("/callback")
    @ResponseBody
    public String callback(
            @RequestParam("XML") String responseXml,
            HttpSession session) {

        String tempFilePath = (String) session.getAttribute("encAadhaarTempFile");
        if (tempFilePath == null) {
            return "Session expired or temp file not found.";
        }

        eSignServiceReturn signResult = esignObj.getSigedDocument(responseXml, tempFilePath);

        if (signResult.getStatus() != 1) {
            return "Signing failed: " + signResult.getErrorCode() + " - " + signResult.getErrorMessage();
        }

        ReturnDocument doc = signResult.getReturnDocuments().get(0);
        if (doc.getStatus() != 1) {
            return "Document error: " + doc.getErrorCode() + " - " + doc.getErrorMessage();
        }

        // doc.getSignedDocument() is Base64-encoded signed PDF
        String signedPdfBase64 = doc.getSignedDocument();
        // TODO: decode, save to DB or filesystem, notify user
        session.removeAttribute("encAadhaarTempFile");
        session.removeAttribute("encAadhaarTxnId");

        return "Signing successful. Document ID: " + doc.getDocId();
    }
}
```

### Redirect view (Thymeleaf)

The Encrypted Aadhaar flow uses field name **`XML`** (not `txnref` as in the standard Aadhaar/PAN flows).

**Option A — Form POST (recommended)**

`esign-redirect.html` — auto-submits a hidden form to the eMudhra gateway with field `XML`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head><title>Redirecting to eSign Gateway...</title></head>
<body onload="document.getElementById('esignForm').submit()">
    <p>Redirecting to eSign gateway for authentication...</p>
    <form id="esignForm" name="esignForm" th:action="${gatewayUrl}" method="POST">
        <input type="hidden" id="XML" name="XML" th:value="${gatewayParam}" />
        <noscript><button type="submit">Click here if not redirected</button></noscript>
    </form>
</body>
</html>
```

**Option B — URL redirect (GET)**

Append the `gatewayParameter` as the `XML` query parameter. The SDK already URL-encodes it, so no extra encoding is needed:

```java
// In the controller, instead of returning a view:
String redirectUrl = gatewayUrl + "?XML=" + result.getGatewayParameter();
return "redirect:" + redirectUrl;
```

Or using `HttpServletResponse` directly:
```java
response.sendRedirect(gatewayUrl + "?XML=" + result.getGatewayParameter());
```

### Error handling for encrypted Aadhaar-specific codes

```java
if (result.getStatus() != 1) {
    switch (result.getErrorCode()) {
        case "ESS-130":
            // Aadhaar number is not 12 digits or contains non-digit characters
            model.addAttribute("error", "Invalid Aadhaar number format.");
            break;
        case "ESS-131":
            // CER file path wrong, file unreadable, or not an RSA certificate
            model.addAttribute("error", "UIDAI certificate configuration error. Contact support.");
            break;
        case "ESS-132":
            // Runtime encryption failure — check server logs
            model.addAttribute("error", "Aadhaar encryption failed. Please try again.");
            break;
        default:
            model.addAttribute("error", result.getErrorCode() + ": " + result.getErrorMessage());
    }
    return "esign-error";
}
