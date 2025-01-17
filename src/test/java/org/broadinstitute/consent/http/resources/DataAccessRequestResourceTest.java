package org.broadinstitute.consent.http.resources;

import com.mongodb.MongoClient;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.models.DataAccessRequestManage;
import org.broadinstitute.consent.http.models.dto.UseRestrictionDTO;
import org.broadinstitute.consent.http.service.DatabaseDataAccessRequestAPI;
import org.broadinstitute.consent.http.util.DarConstants;
import org.broadinstitute.consent.http.util.DarUtil;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

public class DataAccessRequestResourceTest extends DataAccessRequestServiceTest {

    private static final String TEST_DATABASE_NAME = "TestConsent";

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Before
    public void setUp() throws IOException {
        MongoClient mongo = setUpMongoClient();
        MongoConsentDB mongoi = new MongoConsentDB(mongo, TEST_DATABASE_NAME);
        MongoConsentDB spiedMongoi = Mockito.spy(mongoi);
        DatabaseDataAccessRequestAPI.getInstance().setMongoDBInstance(spiedMongoi);
        doReturn("TESTDAR-COUNTER").when(spiedMongoi).getNextSequence(DarConstants.PARTIAL_DAR_CODE_COUNTER);
        mongoi.getDataAccessRequestCollection().drop();
        mongoi.getPartialDataAccessRequestCollection().drop();
    }

    @After
    public void teardown() {
        shutDownMongo();
    }


    @Test
    public void testDarOperations() throws IOException {
        Client client = ClientBuilder.newClient();
        Document sampleDar = DataRequestSamplesHolder.getSampleDar();
        long mongoDocuments = retrieveDars(client, darPath()).size();
        checkStatus(CREATED, post(client, darPath(), sampleDar));
        List<Document> created = retrieveDars(client, darPath());
        assertTrue(created.size() == ++mongoDocuments);
    }

    @Test
    public void testPartialDarOperations() throws IOException {
        Client client = ClientBuilder.newClient();
        int partialDocumentsCount = retrieveDars(client, partialsPath()).size();
        Document partialDar = DataRequestSamplesHolder.getSampleDar();
        checkStatus(CREATED, post(client, partialPath(), partialDar));
        List<Document> created = retrieveDars(client, partialsPath());
        assertTrue(created.size() == ++partialDocumentsCount);
    }

    @Test
    public void testInvalidDars() throws IOException {
        Client client = ClientBuilder.newClient();
        List<UseRestrictionDTO> dtos = getJson(client, invalidDarsPath()).readEntity(new GenericType<List<UseRestrictionDTO>>() {});
        assertTrue(dtos.size() == 0);
    }

    @Test
    public void testManageDars() throws IOException {
        Client client = ClientBuilder.newClient();
        Document sampleDar = DataRequestSamplesHolder.getSampleDar();
        checkStatus(CREATED, post(client, darPath(), sampleDar));
        List<DataAccessRequestManage> manageDars = getJson(client, darManagePath("1")).readEntity(new GenericType<List<DataAccessRequestManage>>() {
        });
        assertTrue(manageDars.size() == 1);

    }

    @Test
    public void testManagePartialDars() throws IOException {
        Client client = ClientBuilder.newClient();
        Document partialDar = DataRequestSamplesHolder.getSampleDar();
        checkStatus(CREATED, post(client, partialPath(), partialDar));
        List<Document> manageDars = getJson(client, partialsManagePath("1")).readEntity(new GenericType<List<Document>>() {
        });
        assertTrue(manageDars.size() == 1);
    }

    @Test
    public void testRestrictionFromQuestions() throws IOException {
        Client client = ClientBuilder.newClient();
        Response response = checkStatus(OK, put(client, restrictionFromQuestionsUrl(), DataRequestSamplesHolder.getSampleDar()));
        String res = response.readEntity(String.class);
        assertTrue(res.equals("{\"useRestriction\":\"Manual Review\"}"));
    }

    @Test
    public void testDarFound() throws Exception {
        // Create a DAR
        Client client = ClientBuilder.newClient();
        post(client, darPath(), DataRequestSamplesHolder.getSampleDar());
        Document created = retrieveDars(client, darPath()).get(0);
        ObjectId objectId = DarUtil.getObjectIdFromDocument(created);

        // Make sure the DAR can be retrieved using the ID
        Response response = getJson(client, darPath(objectId.toHexString()));
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testInvalidDARFormat() throws Exception {
        Client client = ClientBuilder.newClient();
        Response response = getJson(client, darPath("invalid-5998-id"));
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testDARNotFound() throws Exception {
        ObjectId id = new ObjectId();
        Client client = ClientBuilder.newClient();
        Response response = getJson(client, darPath(id.toHexString()));
        assertEquals(404, response.getStatus());
    }

    private List<Document> retrieveDars(Client client, String url) throws IOException {
        return getJson(client, url).readEntity(new GenericType<List<Document>>() {});
    }

}
