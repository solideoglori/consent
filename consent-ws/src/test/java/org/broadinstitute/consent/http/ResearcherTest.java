package org.broadinstitute.consent.http;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;


public class ResearcherTest extends AbstractTest {

    public static final int CREATED = Response.Status.CREATED.getStatusCode();

    private final Integer USER_ID = 1;

    private final String RESEARCHER_REGISTER_URL = "/researcher/%s?validate=%s";
    private final String RESEARCHER_DAR_URL = "/researcher/%s/dar";
    private final String INSTITUTION_NAME = "Broad Institute";
    private final String DEPARTMENT = "Department";
    private final String DIVISION = "Division";
    private final String STREET_ADDRESS_1 = "Belgrano 252";
    private final String STREET_ADDRESS_2 = "San Martin 8545";
    private final String CITY = "City";
    private final String ZIP_POSTAL_CODE = "0000";
    private final String COUNTRY = "Country";
    private final String STATE = "State";
    private final String EMAIL = "test@gmail.com";
    private final String ERA_COMMONS_ID = "DUOS-153-TEST";
    private final String PUBMED_ID_PUBLICATION = "Id publication";
    private final String URL_SCIENTIFIC_PUBLICATION = "www.scientificpublication.com";
    private final String PI_NAME = "Adam Wader";

    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, resourceFilePath("consent-config.yml"));

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    /**
     * Invalid user
     */
    @Test
    public void testRegisterResearcherInvalidUser() {
        Client client = ClientBuilder.newClient();
        checkStatus(NOT_FOUND, post(client, path2Url(String.format(RESEARCHER_REGISTER_URL, 8531, false)), createResearcherPropertiesRequiredFields()));
    }

    /**
     * Invalid properties
     */
    @Test
    public void testRegisterResearcherWithInvalidProperties(){
        Client client = ClientBuilder.newClient();
        checkStatus(BAD_REQUEST, post(client, path2Url(String.format(RESEARCHER_REGISTER_URL, USER_ID, false)), createResearcherPropertiesInvalidProperties()));
    }

    /**
     * Incomplete fields
     */
    @Test
    public void testRegisterResearcherIncompleteData(){
        Client client = ClientBuilder.newClient();
        checkStatus(BAD_REQUEST, post(client, path2Url(String.format(RESEARCHER_REGISTER_URL, USER_ID, true)), createResearcherPropertiesIncomplete()));
    }

    /**
     * Register researcher with required fields
     */
    @Test
    public void testRegisterResearcherRequiredFields(){
        Client client = ClientBuilder.newClient();
        checkStatus(CREATED, post(client, path2Url(String.format(RESEARCHER_REGISTER_URL, USER_ID, true)), createResearcherPropertiesRequiredFields()));
        check200(delete(client,  path2Url(String.format(RESEARCHER_REGISTER_URL, USER_ID, true))));
    }


    /**
     * Register researcher with all fields
     */
    @Test
    public void testRegisterResearcherAllFields(){
        Client client = ClientBuilder.newClient();
        checkStatus(CREATED, post(client, path2Url(String.format(RESEARCHER_REGISTER_URL, USER_ID, true)), createResearcherPropertiesWithAllFields()));
        check200(delete(client, path2Url(String.format(RESEARCHER_REGISTER_URL, USER_ID, true))));
    }


    /**
     * Invalid properties
     */
    @Test
    public void testUpdateResearcherWithInvalidProperties(){
        Client client = ClientBuilder.newClient();
        checkStatus(BAD_REQUEST, put(client,path2Url(String.format(RESEARCHER_REGISTER_URL, USER_ID, false)), createResearcherPropertiesInvalidProperties()));
    }

    /**
     * Incomplete fields
     */
    @Test
    public void testUpdateResearcherIncompleteData(){
        Client client = ClientBuilder.newClient();
        checkStatus(BAD_REQUEST, put(client, path2Url(String.format(RESEARCHER_REGISTER_URL, USER_ID, true)), createResearcherPropertiesIncomplete()));
    }

    /**
     * Invalid user
     */
    @Test
    public void testUpdateResearcherInvalidUser() {
        Client client = ClientBuilder.newClient();
        checkStatus(NOT_FOUND, put(client,  path2Url(String.format(RESEARCHER_REGISTER_URL, 8531, true)), createResearcherPropertiesRequiredFields()));
    }


    @Test
    public void testGetResearcherPropertiesForDAR() {
        Client client = ClientBuilder.newClient();
        checkStatus(CREATED, post(client, path2Url(String.format(RESEARCHER_REGISTER_URL, USER_ID, true)), createResearcherPropertiesWithAllFields()));
        Response response = checkStatus(OK, getJson(client, path2Url(String.format(RESEARCHER_DAR_URL, USER_ID))));
        Map<String, String> properties = response.readEntity(Map.class);
        assertTrue(properties.get(ResearcherFields.INVESTIGATOR.getValue()).equals(PI_NAME));
        assertTrue(properties.get(ResearcherFields.INSTITUTION.getValue()).equals(INSTITUTION_NAME));
        assertTrue(properties.get(ResearcherFields.DEPARTMENT.getValue()).equals(DEPARTMENT));
        assertTrue(properties.get(ResearcherFields.STREET_ADDRESS_1.getValue()).equals(STREET_ADDRESS_1));
        assertTrue(properties.get(ResearcherFields.CITY.getValue()).equals(CITY));
        assertTrue(properties.get(ResearcherFields.ZIP_POSTAL_CODE.getValue()).equals(ZIP_POSTAL_CODE));
        assertTrue(properties.get(ResearcherFields.COUNTRY.getValue()).equals(COUNTRY));
        assertTrue(properties.get(ResearcherFields.STATE.getValue()).equals(STATE));
        assertTrue(properties.get(ResearcherFields.STREET_ADDRESS_2.getValue()).equals(STREET_ADDRESS_2));
        assertTrue(properties.get(ResearcherFields.DIVISION.getValue()).equals(DIVISION));
        check200(delete(client, path2Url(String.format(RESEARCHER_REGISTER_URL, USER_ID, true))));
    }

    private Map<String, String> createResearcherPropertiesRequiredFields(){
        Map<String, String> properties =  new HashMap<>();
        properties.put(ResearcherFields.ACADEMIC_BUSINESS_EMAIL.getValue(), EMAIL);
        properties.put(ResearcherFields.INSTITUTION.getValue(), INSTITUTION_NAME);
        properties.put(ResearcherFields.DEPARTMENT.getValue(), DEPARTMENT);
        properties.put(ResearcherFields.STREET_ADDRESS_1.getValue(), STREET_ADDRESS_1);
        properties.put(ResearcherFields.CITY.getValue(), CITY);
        properties.put(ResearcherFields.ZIP_POSTAL_CODE.getValue(), ZIP_POSTAL_CODE);
        properties.put(ResearcherFields.ARE_YOU_PRINCIPAL_INVESTIGATOR.getValue(), "false");
        properties.put(ResearcherFields.COUNTRY.getValue(), COUNTRY);
        return properties;
    }

    private Map<String, String> createResearcherPropertiesIncomplete(){
        Map<String, String> properties =  new HashMap<>();
        properties.put(ResearcherFields.ACADEMIC_BUSINESS_EMAIL.getValue(), EMAIL);
        properties.put(ResearcherFields.INSTITUTION.getValue(), INSTITUTION_NAME);
        properties.put(ResearcherFields.DEPARTMENT.getValue(), DEPARTMENT);
        return properties;
    }

    private Map<String, String> createResearcherPropertiesInvalidProperties(){
        Map<String, String> properties =  new HashMap<>();
        properties.put(ResearcherFields.ACADEMIC_BUSINESS_EMAIL.getValue(), EMAIL);
        properties.put(ResearcherFields.INSTITUTION.getValue(), INSTITUTION_NAME);
        properties.put("test", DEPARTMENT);
        return properties;
    }

    private Map<String, String> createResearcherPropertiesWithAllFields(){
        Map<String, String> properties =  new HashMap<>();
        properties.put(ResearcherFields.ACADEMIC_BUSINESS_EMAIL.getValue(), EMAIL);
        properties.put(ResearcherFields.INSTITUTION.getValue(), INSTITUTION_NAME);
        properties.put(ResearcherFields.DEPARTMENT.getValue(), DEPARTMENT);
        properties.put(ResearcherFields.STREET_ADDRESS_1.getValue(), STREET_ADDRESS_1);
        properties.put(ResearcherFields.CITY.getValue(), CITY);
        properties.put(ResearcherFields.ZIP_POSTAL_CODE.getValue(), ZIP_POSTAL_CODE);
        properties.put(ResearcherFields.ARE_YOU_PRINCIPAL_INVESTIGATOR.getValue(), "true");
        properties.put(ResearcherFields.COUNTRY.getValue(), COUNTRY);
        properties.put(ResearcherFields.DIVISION.getValue(), DIVISION);
        properties.put(ResearcherFields.STREET_ADDRESS_2.getValue(), STREET_ADDRESS_2);
        properties.put(ResearcherFields.STATE.getValue(), STATE);
        properties.put(ResearcherFields.ERA_COMMONS_ID.getValue(), ERA_COMMONS_ID);
        properties.put(ResearcherFields.PUBMED_ID.getValue(), PUBMED_ID_PUBLICATION);
        properties.put(ResearcherFields.SCIENTIFIC_URL.getValue(), URL_SCIENTIFIC_PUBLICATION);
        properties.put(ResearcherFields.DO_YOU_HAVE_PI.getValue(), "false");
        properties.put(ResearcherFields.PI_NAME.getValue(), PI_NAME);
        properties.put(ResearcherFields.PI_EMAIL.getValue(), EMAIL);
        properties.put(ResearcherFields.PI_eRA_COMMONS_ID.getValue(), ERA_COMMONS_ID);
        return properties;
    }



}