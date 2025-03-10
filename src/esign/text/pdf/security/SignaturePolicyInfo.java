 package esign.text.pdf.security;
 
 import esign.text.pdf.codec.Base64;
 import org.emcastle.asn1.ASN1Encodable;
 import org.emcastle.asn1.ASN1OctetString;
 import org.emcastle.asn1.DERIA5String;
 import org.emcastle.asn1.DERObjectIdentifier;
 import org.emcastle.asn1.DEROctetString;
 import org.emcastle.asn1.esf.OtherHashAlgAndValue;
 import org.emcastle.asn1.esf.SigPolicyQualifierInfo;
 import org.emcastle.asn1.esf.SigPolicyQualifiers;
 import org.emcastle.asn1.esf.SignaturePolicyId;
 import org.emcastle.asn1.esf.SignaturePolicyIdentifier;
 import org.emcastle.asn1.pkcs.PKCSObjectIdentifiers;
 import org.emcastle.asn1.x509.AlgorithmIdentifier;
 
 
 
 public class SignaturePolicyInfo
 {
   private String policyIdentifier;
   private byte[] policyHash;
   private String policyDigestAlgorithm;
   private String policyUri;
   
   public SignaturePolicyInfo(String policyIdentifier, byte[] policyHash, String policyDigestAlgorithm, String policyUri) {
     if (policyIdentifier == null || policyIdentifier.length() == 0) {
       throw new IllegalArgumentException("Policy identifier cannot be null");
     }
     if (policyHash == null) {
       throw new IllegalArgumentException("Policy hash cannot be null");
     }
     if (policyDigestAlgorithm == null || policyDigestAlgorithm.length() == 0) {
       throw new IllegalArgumentException("Policy digest algorithm cannot be null");
     }
     
     this.policyIdentifier = policyIdentifier;
     this.policyHash = policyHash;
     this.policyDigestAlgorithm = policyDigestAlgorithm;
     this.policyUri = policyUri;
   }
   
   public SignaturePolicyInfo(String policyIdentifier, String policyHashBase64, String policyDigestAlgorithm, String policyUri) {
     this(policyIdentifier, (policyHashBase64 != null) ? Base64.decode(policyHashBase64) : null, policyDigestAlgorithm, policyUri);
   }
   
   public String getPolicyIdentifier() {
     return this.policyIdentifier;
   }
   
   public byte[] getPolicyHash() {
     return this.policyHash;
   }
   
   public String getPolicyDigestAlgorithm() {
     return this.policyDigestAlgorithm;
   }
   
   public String getPolicyUri() {
     return this.policyUri;
   }
   
   SignaturePolicyIdentifier toSignaturePolicyIdentifier() {
     String algId = DigestAlgorithms.getAllowedDigests(this.policyDigestAlgorithm);
     
     if (algId == null || algId.length() == 0) {
       throw new IllegalArgumentException("Invalid policy hash algorithm");
     }
     
     SignaturePolicyIdentifier signaturePolicyIdentifier = null;
     SigPolicyQualifierInfo spqi = null;
     
     if (this.policyUri != null && this.policyUri.length() > 0) {
       spqi = new SigPolicyQualifierInfo(PKCSObjectIdentifiers.id_spq_ets_uri, (ASN1Encodable)new DERIA5String(this.policyUri));
     }
     SigPolicyQualifiers qualifiers = new SigPolicyQualifiers(new SigPolicyQualifierInfo[] { spqi });
     
     signaturePolicyIdentifier = new SignaturePolicyIdentifier(new SignaturePolicyId(DERObjectIdentifier.getInstance(new DERObjectIdentifier(this.policyIdentifier.replace("urn:oid:", ""))), new OtherHashAlgAndValue(new AlgorithmIdentifier(algId), (ASN1OctetString)new DEROctetString(this.policyHash)), qualifiers));
 
     
     return signaturePolicyIdentifier;
   }
 }


/* Location:              D:\test\tp\itextpdf-5.5.10.jar!\com\itextpdf\text\pdf\security\SignaturePolicyInfo.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       1.1.3
 */
