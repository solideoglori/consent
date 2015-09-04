package org.genomebridge.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static org.junit.Assert.assertTrue;

public class DataSetResourceTest extends DataSetServiceTest {

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    @Test
    public void testCreateDataSetWrongType() throws Exception {
        Client client = ClientBuilder.newBuilder()
                .register(MultiPartFeature.class).build();
        WebTarget webTarget = client.target(postDataSetFile(false));
        MultiPart mp = createFormData("wrongExt.pdf");

        Response response = webTarget.request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(mp, mp.getMediaType()));
        ArrayList<String> result = response.readEntity(ArrayList.class);
        assertTrue(result.size() == 2);
        assertTrue(response.getStatus() == (BAD_REQUEST));
        assertTrue(result.get(0).equals("A problem has ocurred while uploading datasets - Contact Support"));
        assertTrue(result.get(1).equals("The file type is not the expected one. Please download the sample .txt from your console."));
    }

    @Test
    public void testCreateMissingHeaders() throws Exception {
        // No matter other errors in the file, if the headers doesn't match, it will not try to parse.
        Client client = ClientBuilder.newBuilder()
                .register(MultiPartFeature.class).build();
        WebTarget webTarget = client.target(postDataSetFile(false));
        MultiPart mp = createFormData("missingHeader.txt");

        Response response = webTarget.request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(mp, mp.getMediaType()));
        ArrayList<String> result = response.readEntity(ArrayList.class);
        assertTrue(result.size() == 2);
        assertTrue(response.getStatus() == (BAD_REQUEST));
        assertTrue(result.get(0).equals("Your file has more/less columns than expected. Expected quantity: 10"));
        assertTrue(result.get(1).equals("Please, download the sample file from your console."));
    }

    @Test
    public void testCreateCorrectFile() throws Exception {
        Client client = ClientBuilder.newBuilder()
                .register(MultiPartFeature.class).build();
        WebTarget webTarget = client.target(postDataSetFile(false));
        MultiPart mp = createFormData("correctFile.txt");
        Response response = webTarget.request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(mp, mp.getMediaType()));
        ArrayList<String> result = response.readEntity(ArrayList.class);
        assertTrue(response.getStatus() == (OK));
    }

    @Ignore
    public void testDownloadDataSets() throws Exception {

    }

    private MultiPart createFormData(String name) throws URISyntaxException, FileNotFoundException {
        MultiPart multiPart = new MultiPart();
        multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
        URI uri = Thread.currentThread().getContextClassLoader().getResource("dataset/" + name).toURI();
        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("data",
                new File(uri.getPath()),
                MediaType.valueOf("text/plain"));
        multiPart.bodyPart(fileDataBodyPart);
        return multiPart;
    }
}