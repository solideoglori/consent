package org.broadinstitute.consent.http;

import com.github.fakemongo.Fongo;
import com.mongodb.MongoClient;
import de.spinscale.dropwizard.jobs.JobsBundle;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.chained.ChainedAuthFilter;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import jersey.repackaged.com.google.common.collect.Lists;
import org.broadinstitute.consent.http.authentication.*;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.configurations.MongoConfiguration;
import org.broadinstitute.consent.http.configurations.StoreConfiguration;
import org.broadinstitute.consent.http.db.*;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.mail.AbstractMailServiceAPI;
import org.broadinstitute.consent.http.mail.MailService;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.resources.*;
import org.broadinstitute.consent.http.service.*;
import org.broadinstitute.consent.http.service.ontologyIndexer.IndexOntologyService;
import org.broadinstitute.consent.http.service.ontologyIndexer.IndexerService;
import org.broadinstitute.consent.http.service.ontologyIndexer.IndexerServiceImpl;
import org.broadinstitute.consent.http.service.ontologyIndexer.StoreOntologyService;
import org.broadinstitute.consent.http.service.users.AbstractDACUserAPI;
import org.broadinstitute.consent.http.service.users.DatabaseDACUserAPI;
import org.broadinstitute.consent.http.service.users.handler.AbstractUserRolesHandler;
import org.broadinstitute.consent.http.service.users.handler.DACUserRolesHandler;
import org.broadinstitute.consent.http.service.validate.AbstractUseRestrictionValidatorAPI;
import org.broadinstitute.consent.http.service.validate.UseRestrictionValidator;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.EnumSet;
import java.util.List;
import javax.servlet.FilterRegistration.Dynamic;
import org.eclipse.jetty.servlets.CrossOriginFilter;

/**
 * Top-level entry point to the entire application.
 * <p/>
 * See the Dropwizard docs here:
 * https://dropwizard.github.io/dropwizard/manual/core.html
 */
public class ConsentApplication extends Application<ConsentConfiguration> {

    public static final Logger LOGGER = LoggerFactory.getLogger("ConsentApplication");

    public static void main(String[] args) throws Exception {
        new ConsentApplication().run(args);
    }

    @Override
    public void run(ConsentConfiguration config, Environment env) {

        LOGGER.debug("ConsentApplication.run called.");
        // Client to consume another services
        final javax.ws.rs.client.Client client = new JerseyClientBuilder(env).using(config.getJerseyClientConfiguration())
                .build(getName());
        // Set up the ConsentAPI and the ConsentDAO.  We are working around a dropwizard+Guice issue
        // with singletons and JDBI (see AbstractConsentAPI).

        final MongoConfiguration mongoConfiguration = config.getMongoConfiguration();
        final MongoClient mongoClient;
        GCSStore googleStore;

        if (mongoConfiguration.isTestMode()) {
            Fongo fongo = new Fongo("TestServer");
            mongoClient = fongo.getMongo();
        } else {
            mongoClient = mongoConfiguration.getMongoClient();
        }

        final MongoConsentDB mongoInstance = new MongoConsentDB(mongoClient, mongoConfiguration.getDbName());
        mongoInstance.configureMongo();

        final DBIFactory factory = new DBIFactory();
        final DBI jdbi = factory.build(env, config.getDataSourceFactory(), "db");
        final ConsentDAO consentDAO = jdbi.onDemand(ConsentDAO.class);
        final ElectionDAO electionDAO = jdbi.onDemand(ElectionDAO.class);
        final HelpReportDAO helpReportDAO = jdbi.onDemand(HelpReportDAO.class);
        final VoteDAO voteDAO = jdbi.onDemand(VoteDAO.class);
        final DataSetDAO dataSetDAO = jdbi.onDemand(DataSetDAO.class);
        final DataSetAssociationDAO dataSetAssociationDAO = jdbi.onDemand(DataSetAssociationDAO.class);
        final DACUserDAO dacUserDAO = jdbi.onDemand(DACUserDAO.class);
        final DACUserRoleDAO dacUserRoleDAO = jdbi.onDemand(DACUserRoleDAO.class);
        final MatchDAO matchDAO = jdbi.onDemand(MatchDAO.class);
        final MailMessageDAO emailDAO = jdbi.onDemand(MailMessageDAO.class);
        final ApprovalExpirationTimeDAO approvalExpirationTimeDAO = jdbi.onDemand(ApprovalExpirationTimeDAO.class);
        final DataSetAuditDAO dataSetAuditDAO = jdbi.onDemand(DataSetAuditDAO.class);
        final MailServiceDAO mailServiceDAO = jdbi.onDemand(MailServiceDAO.class);

        UseRestrictionConverter structResearchPurposeConv = new UseRestrictionConverter(config.getUseRestrictionConfiguration());
        DatabaseDataAccessRequestAPI.initInstance(mongoInstance, structResearchPurposeConv, electionDAO, consentDAO, voteDAO, dacUserDAO, dataSetDAO);

        DatabaseConsentAPI.initInstance(jdbi, consentDAO, electionDAO, mongoInstance);

        DatabaseMatchAPI.initInstance(matchDAO, consentDAO);
        DatabaseDataSetAPI.initInstance(dataSetDAO, dataSetAssociationDAO, dacUserRoleDAO, consentDAO, dataSetAuditDAO, electionDAO);
        DatabaseDataSetAssociationAPI.initInstance(dataSetDAO, dataSetAssociationDAO, dacUserDAO);

        try {
            MailService.initInstance(config.getMailConfiguration());
            EmailNotifierService.initInstance(voteDAO, mongoInstance, electionDAO, dacUserDAO, emailDAO, mailServiceDAO, new FreeMarkerTemplateHelper(config.getFreeMarkerConfiguration()), config.getServicesConfiguration().getLocalURL(), config.getMailConfiguration().isActivateEmailNotifications());
        } catch (IOException e) {
            LOGGER.error("Error on Mail Notificacion Service initialization. Service won't work.", e);
        }

        DatabaseMatchingServiceAPI.initInstance(client, config.getServicesConfiguration());
        DatabaseMatchProcessAPI.initInstance(consentDAO, mongoInstance);
        DatabaseSummaryAPI.initInstance(voteDAO, electionDAO, dacUserDAO, consentDAO, dataSetDAO, matchDAO, mongoInstance);
        DatabaseElectionCaseAPI.initInstance(electionDAO, voteDAO, dacUserDAO, dacUserRoleDAO, consentDAO, mongoInstance, dataSetDAO);
        DACUserRolesHandler.initInstance(dacUserDAO, dacUserRoleDAO, electionDAO, voteDAO, dataSetAssociationDAO);
        DatabaseDACUserAPI.initInstance(dacUserDAO, dacUserRoleDAO, electionDAO, voteDAO, dataSetAssociationDAO);
        DatabaseVoteAPI.initInstance(voteDAO, dacUserDAO, electionDAO, dataSetAssociationDAO);
        DatabaseReviewResultsAPI.initInstance(electionDAO, voteDAO, consentDAO);
        DatabaseTranslateServiceAPI.initInstance(client, config.getServicesConfiguration(), structResearchPurposeConv);
        DatabaseHelpReportAPI.initInstance(helpReportDAO, dacUserRoleDAO);
        DatabaseApprovalExpirationTimeAPI.initInstance(approvalExpirationTimeDAO, dacUserDAO);

        UseRestrictionValidator.initInstance(client, config.getServicesConfiguration(), consentDAO);

        // Mail Services
        DatabaseElectionAPI.initInstance(electionDAO, consentDAO, dacUserDAO, mongoInstance, voteDAO, emailDAO, dataSetDAO);
//        final FilterRegistration.Dynamic cors = env.servlets().addFilter("crossOriginRequests", CrossOriginFilter.class);

        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
//        cors.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, env.getApplicationContext().getContextPath() + "/*");

        // Configure CORS parameters
//        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
//        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin,Authorization,Content-Disposition,Access-Control-Expose-Headers,Pragma,Cache-Control,Expires");
//        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "OPTIONS,GET,PUT,POST,DELETE,HEAD");
//        cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER, "OPTIONS,GET,PUT,POST,DELETE,HEAD");
//        cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*");
//        cors.setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_HEADERS_HEADER, "X-Requested-With,Content-Type,Accept,Origin,Authorization,Content-Disposition,Access-Control-Expose-Headers,Expires,Pragma,Cache-Control");

        // Add URL mapping
//        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
        configureCors(env);
        googleStore = getGoogleStore(config.getCloudStoreConfiguration());

        //Manage Ontologies dependencies
        final ElasticSearchConfiguration elasticConfiguration = config.getElasticSearchConfiguration();

        TransportClient eSearchClient = new TransportClient(ImmutableSettings.settingsBuilder()
                .put("cluster.name", elasticConfiguration.getClusterName()));
        elasticConfiguration.getServers().stream().forEach((server) -> {
            eSearchClient.addTransportAddress(new InetSocketTransportAddress(server, 9300));
        });

        final StoreOntologyService storeOntologyService
                = new StoreOntologyService(googleStore,
                        config.getStoreOntologyConfiguration().getBucketSubdirectory(),
                        config.getStoreOntologyConfiguration().getConfigurationFileName());

        final IndexOntologyService indexOntologyService = new IndexOntologyService(eSearchClient, config.getElasticSearchConfiguration().getIndexName());
        final IndexerService indexerService = new IndexerServiceImpl(storeOntologyService, indexOntologyService);
        env.jersey().register(new IndexerResource(indexerService, googleStore));

        // How register our resources.
        env.jersey().register(DataAccessRequestResource.class);
        env.jersey().register(DataSetResource.class);
        env.jersey().register(DataSetAssociationsResource.class);
        env.jersey().register(ConsentResource.class);
        env.jersey().register(ConsentsResource.class);
        env.jersey().register(ConsentAssociationResource.class);
        env.jersey().register(new DataUseLetterResource(googleStore));
        env.jersey().register(AllAssociationsResource.class);
        env.jersey().register(ConsentElectionResource.class);
        env.jersey().register(DataRequestElectionResource.class);
        env.jersey().register(ConsentVoteResource.class);
        env.jersey().register(DataRequestVoteResource.class);
        env.jersey().register(ConsentCasesResource.class);
        env.jersey().register(DataRequestCasesResource.class);
        env.jersey().register(DACUserResource.class);
        env.jersey().register(ElectionReviewResource.class);
        env.jersey().register(ConsentManageResource.class);
        env.jersey().register(ElectionResource.class);
        env.jersey().register(MatchResource.class);
        env.jersey().register(EmailNotifierResource.class);
        env.jersey().register(HelpReportResource.class);
        env.jersey().register(ApprovalExpirationTimeResource.class);

        //Authentication filters
        AuthFilter defaultAuthFilter = new DefaultAuthFilter.Builder<User>()
                .setAuthenticator(new DefaultAuthenticator())
                .setRealm(" ")
                .buildAuthFilter();

        List<AuthFilter> filters = Lists.newArrayList(defaultAuthFilter, new BasicCustomAuthFilter(new BasicAuthenticator(config.getBasicAuthentication())), new OAuthCustomAuthFilter(new OAuthAuthenticator(config.getGoogleAuthentication()), dacUserRoleDAO));
        env.jersey().register(new AuthDynamicFeature(new ChainedAuthFilter(filters)));
        env.jersey().register(RolesAllowedDynamicFeature.class);
        env.jersey().register(new AuthValueFactoryProvider.Binder<>(User.class));

        // Register a listener to catch an application stop and clear out the API instance created above.
        // For normal exit, this is a no-op, but the junit tests that use the DropWizardAppRule will
        // repeatedly start and stop the application, all within the same JVM, causing the run() method to be
        // called multiple times.
        env.lifecycle().addLifeCycleListener(new AbstractLifeCycle.AbstractLifeCycleListener() {

            @Override
            public void lifeCycleStopped(LifeCycle event) {
                LOGGER.debug("**** ConsentApplication Server Stopped ****");
                AbstractTranslateServiceAPI.clearInstance();
                AbstractConsentAPI.clearInstance();
                AbstractElectionAPI.clearInstance();
                AbstractVoteAPI.clearInstance();
                AbstractPendingCaseAPI.clearInstance();
                AbstractDataSetAssociationAPI.clearInstance();
                AbstractDACUserAPI.clearInstance();
                AbstractSummaryAPI.clearInstance();
                AbstractReviewResultsAPI.clearInstance();
                AbstractDataSetAPI.clearInstance();
                AbstractDataAccessRequestAPI.clearInstance();
                AbstractMatchingServiceAPI.clearInstance();
                AbstractMatchAPI.clearInstance();
                AbstractMatchProcessAPI.clearInstance();
                AbstractMailServiceAPI.clearInstance();
                AbstractEmailNotifierAPI.clearInstance();
                AbstractHelpReportAPI.clearInstance();
                AbstractApprovalExpirationTimeAPI.clearInstance();
                AbstractUseRestrictionValidatorAPI.clearInstance();
                AbstractUserRolesHandler.clearInstance();
                super.lifeCycleStopped(event);
            }
        });
    }

    private GCSStore getGoogleStore(StoreConfiguration config) {
        GCSStore googleStore;
        try {
            googleStore = new GCSStore(config);
        } catch (GeneralSecurityException | IOException e) {
            LOGGER.error("Couldn't connect to to Google Cloud Storage.", e);
            throw new IllegalStateException(e);
        }
        return googleStore;
    }

    @Override
    public void initialize(Bootstrap<ConsentConfiguration> bootstrap) {

        bootstrap.addBundle(new MultiPartBundle());
        bootstrap.addBundle(new AssetsBundle("/assets/", "/site"));
        bootstrap.addBundle(new MigrationsBundle<ConsentConfiguration>() {
            @Override
            public DataSourceFactory getDataSourceFactory(ConsentConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        });
        bootstrap.addBundle(new DBIExceptionsBundle());
        bootstrap.addBundle(new JobsBundle());
    }

    private void configureCors(Environment environment) {
        Dynamic filter = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
        filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
        filter.setInitParameter("allowedOrigins", "*");
        filter.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD,PATCH");
        filter.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin,Authorization,Content-Disposition,Access-Control-Expose-Headers,Pragma,Cache-Control,Expires");
        filter.setInitParameter("exposeHeaders", "Content-Type,Pragma,Cache-Control,Expires");
        filter.setInitParameter("allowCredentials", "true");
    }
}