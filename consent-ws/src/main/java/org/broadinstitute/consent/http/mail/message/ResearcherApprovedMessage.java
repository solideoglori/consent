package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.Collection;
import java.util.Set;

public class ResearcherApprovedMessage extends MailMessage {

    private final String APPROVED_DAR = "%s was approved.";

    public Collection<Mail> researcherApprovedMessage(Set<String> toAddresses, String fromAddress, Writer template, String darCode) throws MessagingException {
        return generateEmailMessages(toAddresses, fromAddress, template, darCode, null);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return String.format(APPROVED_DAR, referenceId);
    }
}
