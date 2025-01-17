package org.broadinstitute.consent.http.service;

import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.ResearcherProperty;

import javax.ws.rs.BadRequestException;
import java.util.List;

public interface NihAuthApi {

    List<ResearcherProperty> authenticateNih(NIHUserAccount nihAccount, Integer userId) throws BadRequestException;

    void deleteNihAccountById(Integer userId);
}
