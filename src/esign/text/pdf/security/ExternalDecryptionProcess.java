package esign.text.pdf.security;

import org.emcastle.cms.Recipient;
import org.emcastle.cms.RecipientId;

public interface ExternalDecryptionProcess {
  RecipientId getCmsRecipientId();
  
  Recipient getCmsRecipient();
}

