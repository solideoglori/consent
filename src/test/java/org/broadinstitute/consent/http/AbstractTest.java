package org.broadinstitute.consent.http;

import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.config.Storage;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.runtime.Network;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.authentication.OAuthAuthenticator;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.ConsentBuilder;
import org.broadinstitute.consent.http.models.DataUseDTO;
import org.broadinstitute.consent.http.models.grammar.UseRestriction;
import org.broadinstitute.consent.http.models.validate.ValidateResponse;
import org.broadinstitute.consent.http.service.validate.UseRestrictionValidator;
import org.mockito.Mockito;
import org.skife.jdbi.v2.DBI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An abstract superclass for Tests that involve the BOSS API, includes helper methods for setting up
 * a Dropwizard Configuration as well as for all the standard calls (createObject, etc) into the API
 * through the Jersey client API.
 */
abstract public class AbstractTest extends ResourcedTest {

    private final Logger logger = Logger.getLogger(AbstractTest.class);
    public static final int CREATED = Response.Status.CREATED.getStatusCode();
    static final int CONFLICT = Response.Status.CONFLICT.getStatusCode();
    static final int NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
    public static final int OK = Response.Status.OK.getStatusCode();
    public static final int BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();
    abstract public DropwizardAppRule<ConsentConfiguration> rule();
    private MongodExecutable mongodExe;
    private MongodProcess mongod;

    /*
     * Some utility methods for interacting with HTTP-services.
     */

    public <T> Response post(Client client, String path, T value) throws IOException {
        mockValidateTokenResponse();
        return client.target(path)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", "Bearer api_token")
                .post(Entity.json(value), Response.class);
    }

    public <T> Response put(Client client, String path, T value) throws IOException {
        mockValidateTokenResponse();
        return client.target(path)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", "Bearer api_token")
                .put(Entity.json(value), Response.class);
    }

    public Response delete(Client client, String path) throws IOException {
        mockValidateTokenResponse();
        return client.target(path)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", "Bearer api_token")
                .delete(Response.class);
    }

    public Response getJson(Client client, String path) throws IOException {
        mockValidateTokenResponse();
        return getWithMediaType(client, path, MediaType.APPLICATION_JSON_TYPE);
    }

    private Response getWithMediaType(Client client, String path, MediaType mediaType) {
        return client.target(path)
                .request(mediaType)
                .header("Authorization", "Bearer api_token")
                .get(Response.class);
    }

    void check200(Response response) {
        checkStatus(200, response);
    }

    public Response checkStatus(int status, Response response) {
        if (response.getStatus() != status) {
            logger.error("Incorrect response status: " + response.toString());
        }
        assertThat(response.getStatus()).isEqualTo(status);
        return response;
    }

    public String checkHeader(Response response, String header) {
        MultivaluedMap<String, Object> map = response.getHeaders();
        assertThat(map).describedAs(String.format("header \"%s\"", header)).containsKey(header);
        return map.getFirst(header).toString();
    }

    public String path2Url(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1, path.length());
        }
        return String.format("http://localhost:%d/api/%s", rule().getLocalPort(), path);
    }

    void mockTranslateResponse(){
        final Invocation.Builder builderMock = Mockito.mock(Invocation.Builder.class);
        final WebTarget webTargetMock = Mockito.mock(WebTarget.class);
        String mockString = "TranslateMock";
        InputStream stream = new ByteArrayInputStream(mockString.getBytes(StandardCharsets.UTF_8));
        Response responseMock = Response.status(Response.Status.OK).entity(stream).build();
        Mockito.when(builderMock.post(Entity.json(Mockito.anyString()))).thenReturn(responseMock);
        Mockito.when(webTargetMock.request(MediaType.APPLICATION_JSON)).thenReturn(builderMock);
        final Client clientMock= Mockito.mock(Client.class);
        Mockito.when(clientMock.target(Mockito.anyString())).thenReturn(webTargetMock);
        Mockito.when(webTargetMock.queryParam(Mockito.anyString(), Mockito.anyString())).thenReturn(webTargetMock);
    }

    void mockValidateResponse(){
        final Invocation.Builder builderMock = Mockito.mock(Invocation.Builder.class);
        final WebTarget webTargetMock = Mockito.mock(WebTarget.class);
        ValidateResponse entity = new ValidateResponse(true, "mockedValidatedRestriction");


        final Response responseMock = Mockito.mock(Response.class);
        Mockito.when(responseMock.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        Mockito.when(responseMock.readEntity(ValidateResponse.class)).thenReturn(entity);

        Mockito.when(builderMock.post(Entity.json(Mockito.anyString()))).thenReturn(responseMock);
        Mockito.when(webTargetMock.request(MediaType.APPLICATION_JSON)).thenReturn(builderMock);
        final Client clientMock= Mockito.mock(Client.class);
        Mockito.when(clientMock.target(Mockito.anyString())).thenReturn(webTargetMock);
        Mockito.when(webTargetMock.queryParam(Mockito.anyString(), Mockito.anyString())).thenReturn(webTargetMock);
        UseRestrictionValidator.getInstance().setClient(clientMock);
    }


    public void mockValidateTokenResponse() throws IOException {
        String result = "{ " +
                "\"azp\": \"1234564ko.apps.googleusercontent.com\", " +
                "\"aud\": \"clientId\", " +
                "\"sub\": \"1234564\", " +
                "\"scope\": \"https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile https://www.googleapis.com/auth/plus.me\", " +
                "\"exp\": \"1472235144\", " +
                "\"expires_in\": \"3567\", " +
                "\"email\": \"oauthuser@broadinstitute.org\", " +
                "\"email_verified\": \"true\", " +
                "\"access_type\": \"online\" " +
                " }";
        InputStream inputStream = IOUtils.toInputStream(result, Charset.defaultCharset());
        HttpClient client = Mockito.mock(HttpClient.class);
        HttpResponse mockResponse = Mockito.mock(HttpResponse.class);
        HttpEntity entity = Mockito.mock(HttpEntity.class);
        Mockito.when(client.execute(Mockito.any())).thenReturn(mockResponse);
        Mockito.when(mockResponse.getEntity()).thenReturn(entity);
        Mockito.when(entity.getContent()).thenReturn(inputStream);
        OAuthAuthenticator.getInstance().setHttpClient(client);
    }

    Consent generateNewConsent(UseRestriction useRestriction, DataUseDTO dataUse) {
        Timestamp createDate = new Timestamp(new Date().getTime());
        return new ConsentBuilder().
                setRequiresManualReview(false).
                setUseRestriction(useRestriction).
                setDataUse(dataUse).
                setName(UUID.randomUUID().toString()).
                setCreateDate(createDate).
                setLastUpdate(createDate).
                setSortDate(createDate).
                build();
    }

    protected MongoClient setUpMongoClient() throws IOException {
        int port = 27017;
        String bindIp = "localhost";
        // Creating Mongodbruntime instance
        // Make flapdoodle/mongo respect dropwizard's logging framework
        // See https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo#usage---custom-mongod-process-output
        org.slf4j.Logger slf4jLogger = org.slf4j.LoggerFactory.getLogger(getClass().getName());
        IRuntimeConfig runtimeConfig = new RuntimeConfigBuilder()
                .defaultsWithLogger(Command.MongoD, slf4jLogger)
                .processOutput(ProcessOutput.getDefaultInstanceSilent())
                .build();
        MongodStarter starter = MongodStarter.getInstance(runtimeConfig);
        // Creating MongodbExecutable
        Storage replication = new Storage("target/mongo",null,0);
        IMongodConfig config = new MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .replication(replication)
                .net(new Net(bindIp, port, Network.localhostIsIPv6()))
                .build();
        mongodExe = starter.prepare(config);
        // Starting Mongodb
        mongod = mongodExe.start();
        return new MongoClient(bindIp, port);
    }

    protected void shutDownMongo() {
        mongod.stop();
        mongodExe.stop();
        // Force cleanup of any leftover mongo db files. Only deletes the `mongo` directory itself.
        try {
            FileUtils.deleteDirectory(new File("target/mongo"));
        } catch (IOException e) {
            logger.error("Unable to remove target/mongo directory");
        }
    }

    protected DBI getApplicationJdbi() {
        ConsentConfiguration configuration = rule().getConfiguration();
        Environment environment = rule().getEnvironment();
        return new DBIFactory().build(environment, configuration.getDataSourceFactory(), "mysql");
    }

}
