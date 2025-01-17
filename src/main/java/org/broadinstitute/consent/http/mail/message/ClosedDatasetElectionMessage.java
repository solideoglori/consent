package org.broadinstitute.consent.http.mail.message;

import com.sendgrid.Mail;

import javax.mail.MessagingException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ClosedDatasetElectionMessage extends MailMessage {

    private final String CLOSED_DATASET_ELECTIONS = "Report of closed Dataset elections.";

    public Collection<Mail> closedDatasetElectionMessage(Set<String> toAddresses, String fromAddress, Writer template, String referenceId, String type) throws MessagingException {
        return generateEmailMessages(toAddresses, fromAddress, template, referenceId, type);
    }

    @Override
    String assignSubject(String referenceId, String type) {
        return CLOSED_DATASET_ELECTIONS;
    }
}
