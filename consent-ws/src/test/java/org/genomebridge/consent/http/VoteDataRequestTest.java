package org.genomebridge.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.genomebridge.consent.http.enumeration.ElectionStatus;
import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.models.PendingCase;
import org.genomebridge.consent.http.models.Vote;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class VoteDataRequestTest extends ElectionVoteServiceTest {

    public static final int CREATED = Response.Status.CREATED
            .getStatusCode();
    public static final int OK = Response.Status.OK.getStatusCode();
    private static final String DATA_REQUEST_ID = "1";
    private static final Integer DAC_USER_ID = 2;
    private static final String RATIONALE = "Test";

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Test
    public void testCreateDataRequestVote() {
        // should exist an election for specified data request
        Integer electionId = createElection();
        Client client = ClientBuilder.newClient();
        List<Vote> votes = getJson(client, voteDataRequestPath(DATA_REQUEST_ID)).readEntity(new GenericType<List<Vote>>() {
        });
        Vote vote = new Vote();
        vote.setVote(false);
        vote.setRationale(RATIONALE);
        checkStatus(OK,
                post(client, voteDataRequestIdPath(DATA_REQUEST_ID, votes.get(0).getVoteId()), vote));
        //describe vote
        Vote created = retrieveVote(client, voteDataRequestIdPath(DATA_REQUEST_ID, votes.get(0).getVoteId()));
        assertThat(created.getElectionId()).isEqualTo(electionId);
        assertThat(created.getRationale()).isEqualTo(RATIONALE);
        assertThat(created.getUpdateDate()).isNull();
        assertThat(created.getVote()).isFalse();
        assertThat(created.getVoteId()).isNotNull();
        testDataRequestPendingCase(DAC_USER_ID);
        updateVote(created.getVoteId(), created);
        deleteVotes(votes);
        delete(client, electionDataRequestPathById(DATA_REQUEST_ID, electionId));
    }


    public void deleteVotes(List<Vote> votes) {
        Client client = ClientBuilder.newClient();
        for (Vote vote : votes) {
            checkStatus(OK,
                    delete(client, voteDataRequestIdPath(DATA_REQUEST_ID, vote.getVoteId())));
        }
    }

    public void updateVote(Integer id, Vote vote) {
        Client client = ClientBuilder.newClient();
        vote.setVote(true);
        vote.setRationale(null);
        checkStatus(OK,
                put(client, voteDataRequestIdPath(DATA_REQUEST_ID, id), vote));
        vote = retrieveVote(client, voteDataRequestIdPath(DATA_REQUEST_ID, id));
        assertThat(vote.getRationale()).isNull();
        assertThat(vote.getUpdateDate()).isNotNull();
        assertThat(vote.getVote()).isTrue();
    }

    private Integer createElection() {
        Client client = ClientBuilder.newClient();
        Election election = new Election();
        election.setStatus(ElectionStatus.OPEN.getValue());
        post(client, electionDataRequestPath(DATA_REQUEST_ID), election);
        election = getJson(client, electionDataRequestPath(DATA_REQUEST_ID))
                .readEntity(Election.class);
        return election.getElectionId();
    }


    public void testDataRequestPendingCase(Integer dacUserId) {
        Client client = ClientBuilder.newClient();
        List<PendingCase> pendingCases = getJson(client, dataRequestPendingCasesPath(dacUserId)).readEntity(new GenericType<List<PendingCase>>() {});
        assertThat(pendingCases).isNotNull();
        assertThat(pendingCases.size()).isEqualTo(1);
        assertThat(pendingCases.get(0).getLogged()).isEqualTo("1/4");
        assertThat(pendingCases.get(0).getReferenceId()).isEqualTo(DATA_REQUEST_ID);
    }

    @Test
    public void testDataRequestPendingCaseWithInvalidUser() {
        Client client = ClientBuilder.newClient();
        List<PendingCase> pendingCases = getJson(client, dataRequestPendingCasesPath(789)).readEntity(new GenericType<List<PendingCase>>() {});
        assertThat(pendingCases).isEmpty();
    }

}
