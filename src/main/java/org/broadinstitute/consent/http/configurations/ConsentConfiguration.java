package org.broadinstitute.consent.http.configurations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsentConfiguration extends Configuration {

    public ConsentConfiguration() {
    }

    @Valid
    @NotNull
    @JsonProperty
    private final DataSourceFactory database = new DataSourceFactory();

    @Valid
    @NotNull
    @JsonProperty
    private final StoreConfiguration googleStore = new StoreConfiguration();

    @Valid
    @NotNull
    @JsonProperty
    private final MongoConfiguration mongo = new MongoConfiguration();

    @Valid
    @NotNull
    @JsonProperty
    private final ServicesConfiguration services = new ServicesConfiguration();

    @Valid
    @NotNull
    private JerseyClientConfiguration httpClient = new JerseyClientConfiguration();

    @Valid
    @NotNull
    private MailConfiguration mailConfiguration = new MailConfiguration();

    @Valid
    @NotNull
    private FreeMarkerConfiguration freeMarkerConfiguration = new FreeMarkerConfiguration();

    @Valid
    @NotNull
    private GoogleOAuth2Config googleAuthentication = new GoogleOAuth2Config();

    @Valid
    @NotNull
    private BasicAuthConfig basicAuthentication = new BasicAuthConfig();

    @JsonProperty("datasets")
    private List<String> datasets = new ArrayList<>();

    @JsonProperty("httpClient")
    public JerseyClientConfiguration getJerseyClientConfiguration() {
        return httpClient;
    }


    @Valid
    @NotNull
    @JsonProperty
    private final ElasticSearchConfiguration elasticSearch = new ElasticSearchConfiguration();

    @Valid
    @NotNull
    @JsonProperty
    private final StoreOntologyConfiguration storeOntology = new StoreOntologyConfiguration();


    public DataSourceFactory getDataSourceFactory() {
        return database;
    }

    public StoreConfiguration getCloudStoreConfiguration() {
        return googleStore;
    }

    public MongoConfiguration getMongoConfiguration() {
        return mongo;
    }

    public ServicesConfiguration getServicesConfiguration() {
        return services;
    }

    public MailConfiguration getMailConfiguration() {
        return mailConfiguration;
    }

    public FreeMarkerConfiguration getFreeMarkerConfiguration() {
        return freeMarkerConfiguration;
    }

    public BasicAuthConfig getBasicAuthentication() {
        return basicAuthentication;
    }

    public void setBasicAuthentication(BasicAuthConfig basicAuthentication) {
        this.basicAuthentication = basicAuthentication;
    }

    public GoogleOAuth2Config getGoogleAuthentication() {
        return googleAuthentication;
    }

    public void setGoogleAuthentication(GoogleOAuth2Config googleAuthentication) {
        this.googleAuthentication = googleAuthentication;
    }

    public ElasticSearchConfiguration getElasticSearchConfiguration() {
        return elasticSearch;
    }

    public StoreOntologyConfiguration getStoreOntologyConfiguration() {
        return storeOntology;
    }

    public List<String> getDatasets() {
        return datasets;
    }

}
