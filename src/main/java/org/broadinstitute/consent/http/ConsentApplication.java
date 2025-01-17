package org.broadinstitute.consent.http;

import com.google.inject.Guice;
import com.google.inject.Injector;
import de.spinscale.dropwizard.jobs.JobsBundle;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.chained.ChainedAuthFilter;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.jdbi.bundles.DBIExceptionsBundle;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import jersey.repackaged.com.google.common.collect.Lists;
import org.broadinstitute.consent.http.authentication.AbstractOAuthAuthenticator;
import org.broadinstitute.consent.http.authentication.BasicAuthenticator;
import org.broadinstitute.consent.http.authentication.BasicCustomAuthFilter;
import org.broadinstitute.consent.http.authentication.DefaultAuthFilter;
import org.broadinstitute.consent.http.authentication.DefaultAuthenticator;
import org.broadinstitute.consent.http.authentication.OAuthAuthenticator;
import org.broadinstitute.consent.http.authentication.OAuthCustomAuthFilter;
import org.broadinstitute.consent.http.cloudstore.GCSHealthCheck;
import org.broadinstitute.consent.http.cloudstore.GCSStore;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.db.ApprovalExpirationTimeDAO;
import org.broadinstitute.consent.http.db.AssociationDAO;
import org.broadinstitute.consent.http.db.ConsentDAO;
import org.broadinstitute.consent.http.db.DACUserDAO;
import org.broadinstitute.consent.http.db.DataSetAssociationDAO;
import org.broadinstitute.consent.http.db.DataSetAuditDAO;
import org.broadinstitute.consent.http.db.DataSetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.HelpReportDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.MailServiceDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.ResearcherPropertyDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.db.WorkspaceAuditDAO;
import org.broadinstitute.consent.http.db.mongo.MongoConsentDB;
import org.broadinstitute.consent.http.mail.AbstractMailServiceAPI;
import org.broadinstitute.consent.http.mail.MailService;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.resources.AllAssociationsResource;
import org.broadinstitute.consent.http.resources.ApprovalExpirationTimeResource;
import org.broadinstitute.consent.http.resources.ConsentAssociationResource;
import org.broadinstitute.consent.http.resources.ConsentCasesResource;
import org.broadinstitute.consent.http.resources.ConsentElectionResource;
import org.broadinstitute.consent.http.resources.ConsentManageResource;
import org.broadinstitute.consent.http.resources.ConsentResource;
import org.broadinstitute.consent.http.resources.ConsentVoteResource;
import org.broadinstitute.consent.http.resources.ConsentsResource;
import org.broadinstitute.consent.http.resources.DACUserResource;
import org.broadinstitute.consent.http.resources.DacResource;
import org.broadinstitute.consent.http.resources.DataAccessAgreementResource;
import org.broadinstitute.consent.http.resources.DataAccessRequestResource;
import org.broadinstitute.consent.http.resources.DataRequestCasesResource;
import org.broadinstitute.consent.http.resources.DataRequestElectionResource;
import org.broadinstitute.consent.http.resources.DataRequestReportsResource;
import org.broadinstitute.consent.http.resources.DataRequestVoteResource;
import org.broadinstitute.consent.http.resources.DataSetAssociationsResource;
import org.broadinstitute.consent.http.resources.DataSetResource;
import org.broadinstitute.consent.http.resources.DataUseLetterResource;
import org.broadinstitute.consent.http.resources.ElectionResource;
import org.broadinstitute.consent.http.resources.ElectionReviewResource;
import org.broadinstitute.consent.http.resources.EmailNotifierResource;
import org.broadinstitute.consent.http.resources.HelpReportResource;
import org.broadinstitute.consent.http.resources.IndexerResource;
import org.broadinstitute.consent.http.resources.MatchResource;
import org.broadinstitute.consent.http.resources.NihAccountResource;
import org.broadinstitute.consent.http.resources.ResearcherResource;
import org.broadinstitute.consent.http.resources.StatusResource;
import org.broadinstitute.consent.http.resources.SwaggerResource;
import org.broadinstitute.consent.http.resources.UserResource;
import org.broadinstitute.consent.http.resources.VersionResource;
import org.broadinstitute.consent.http.resources.WorkspaceResource;
import org.broadinstitute.consent.http.service.AbstractApprovalExpirationTimeAPI;
import org.broadinstitute.consent.http.service.AbstractAuditServiceAPI;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.AbstractDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.AbstractDataSetAPI;
import org.broadinstitute.consent.http.service.AbstractDataSetAssociationAPI;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.AbstractEmailNotifierAPI;
import org.broadinstitute.consent.http.service.AbstractHelpReportAPI;
import org.broadinstitute.consent.http.service.AbstractMatchAPI;
import org.broadinstitute.consent.http.service.AbstractMatchProcessAPI;
import org.broadinstitute.consent.http.service.AbstractMatchingServiceAPI;
import org.broadinstitute.consent.http.service.AbstractPendingCaseAPI;
import org.broadinstitute.consent.http.service.AbstractReviewResultsAPI;
import org.broadinstitute.consent.http.service.AbstractSummaryAPI;
import org.broadinstitute.consent.http.service.AbstractTranslateService;
import org.broadinstitute.consent.http.service.AbstractVoteAPI;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.DatabaseApprovalExpirationTimeAPI;
import org.broadinstitute.consent.http.service.DatabaseAuditServiceAPI;
import org.broadinstitute.consent.http.service.DatabaseConsentAPI;
import org.broadinstitute.consent.http.service.DatabaseDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.DatabaseDataSetAPI;
import org.broadinstitute.consent.http.service.DatabaseDataSetAssociationAPI;
import org.broadinstitute.consent.http.service.DatabaseElectionAPI;
import org.broadinstitute.consent.http.service.DatabaseElectionCaseAPI;
import org.broadinstitute.consent.http.service.DatabaseHelpReportAPI;
import org.broadinstitute.consent.http.service.DatabaseMatchAPI;
import org.broadinstitute.consent.http.service.DatabaseMatchProcessAPI;
import org.broadinstitute.consent.http.service.DatabaseMatchingServiceAPI;
import org.broadinstitute.consent.http.service.DatabaseReviewResultsAPI;
import org.broadinstitute.consent.http.service.DatabaseSummaryAPI;
import org.broadinstitute.consent.http.service.DatabaseVoteAPI;
import org.broadinstitute.consent.http.service.EmailNotifierService;
import org.broadinstitute.consent.http.service.NihAuthApi;
import org.broadinstitute.consent.http.service.NihServiceAPI;
import org.broadinstitute.consent.http.service.TranslateServiceImpl;
import org.broadinstitute.consent.http.service.UseRestrictionConverter;
import org.broadinstitute.consent.http.service.VoteService;
import org.broadinstitute.consent.http.service.ontology.ElasticSearchHealthCheck;
import org.broadinstitute.consent.http.service.ontology.IndexOntologyService;
import org.broadinstitute.consent.http.service.ontology.IndexerService;
import org.broadinstitute.consent.http.service.ontology.IndexerServiceImpl;
import org.broadinstitute.consent.http.service.ontology.StoreOntologyService;
import org.broadinstitute.consent.http.service.users.AbstractDACUserAPI;
import org.broadinstitute.consent.http.service.users.DatabaseDACUserAPI;
import org.broadinstitute.consent.http.service.users.DatabaseUserAPI;
import org.broadinstitute.consent.http.service.users.UserAPI;
import org.broadinstitute.consent.http.service.users.handler.AbstractUserRolesHandler;
import org.broadinstitute.consent.http.service.users.handler.DACUserRolesHandler;
import org.broadinstitute.consent.http.service.users.handler.DatabaseResearcherAPI;
import org.broadinstitute.consent.http.service.users.handler.ResearcherAPI;
import org.broadinstitute.consent.http.service.validate.AbstractUseRestrictionValidatorAPI;
import org.broadinstitute.consent.http.service.validate.UseRestrictionValidator;
import org.dhatim.dropwizard.sentry.logging.SentryBootstrap;
import org.dhatim.dropwizard.sentry.logging.UncaughtExceptionHandlers;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration.Dynamic;
import javax.ws.rs.client.Client;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

/**
 * Top-level entry point to the entire application.
 * <p/>
 * See the Dropwizard docs here:
 * https://dropwizard.github.io/dropwizard/manual/core.html
 */
public class ConsentApplication extends Application<ConsentConfiguration> {

    private static final Logger LOGGER = LoggerFactory.getLogger("ConsentApplication");

    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting Consent Application");
        try {
            String dsn = System.getProperties().getProperty("sentry.dsn");
            if (null != dsn && !dsn.isEmpty()) {
                SentryBootstrap.bootstrap(dsn);
                Thread.currentThread().setUncaughtExceptionHandler(UncaughtExceptionHandlers.systemExit());
            } else {
                LOGGER.error("Unable to boostrap sentry logging.");
            }
        } catch (Exception e) {
            LOGGER.error("Exception loading sentry properties: " + e.getMessage());
        }
        new ConsentApplication().run(args);
        LOGGER.info("Consent Application Started");
    }

    @Override
    public void run(ConsentConfiguration config, Environment env) {

        // TODO: Update all services to use an injector.
        // Previously, this code was working around a dropwizard+Guice issue with singletons and JDBI (see AbstractConsentAPI).
        final Injector injector = Guice.createInjector(new ConsentModule(config, env));

        // Clients
        final DBI jdbi = injector.getProvider(DBI.class).get();
        final MongoConsentDB mongoInstance = injector.getProvider(MongoConsentDB.class).get();
        final Client client = injector.getProvider(Client.class).get();
        final UseRestrictionConverter useRestrictionConverter = injector.getProvider(UseRestrictionConverter.class).get();
        final GCSStore googleStore = injector.getProvider(GCSStore.class).get();

        // DAOs
        // TODO: Eventually, when all services can be constructed with injection, these should all go away.
        final ConsentDAO consentDAO = injector.getProvider(ConsentDAO.class).get();
        final ElectionDAO electionDAO = injector.getProvider(ElectionDAO.class).get();
        final HelpReportDAO helpReportDAO = injector.getProvider(HelpReportDAO.class).get();
        final VoteDAO voteDAO = injector.getProvider(VoteDAO.class).get();
        final DataSetDAO dataSetDAO = injector.getProvider(DataSetDAO.class).get();
        final DataSetAssociationDAO dataSetAssociationDAO = injector.getProvider(DataSetAssociationDAO.class).get();
        final DACUserDAO dacUserDAO = injector.getProvider(DACUserDAO.class).get();
        final UserRoleDAO userRoleDAO = injector.getProvider(UserRoleDAO.class).get();
        final MatchDAO matchDAO = injector.getProvider(MatchDAO.class).get();
        final MailMessageDAO emailDAO = injector.getProvider(MailMessageDAO.class).get();
        final ApprovalExpirationTimeDAO approvalExpirationTimeDAO = injector.getProvider(ApprovalExpirationTimeDAO.class).get();
        final DataSetAuditDAO dataSetAuditDAO = injector.getProvider(DataSetAuditDAO.class).get();
        final MailServiceDAO mailServiceDAO = injector.getProvider(MailServiceDAO.class).get();
        final ResearcherPropertyDAO  researcherPropertyDAO = injector.getProvider(ResearcherPropertyDAO.class).get();
        final WorkspaceAuditDAO workspaceAuditDAO = injector.getProvider(WorkspaceAuditDAO.class).get();
        final AssociationDAO associationDAO = injector.getProvider(AssociationDAO.class).get();

        // Services
        final DacService dacService = injector.getProvider(DacService.class).get();
        final VoteService voteService = injector.getProvider(VoteService.class).get();
        DatabaseAuditServiceAPI.initInstance(workspaceAuditDAO, dacUserDAO, associationDAO);
        DatabaseDataAccessRequestAPI.initInstance(mongoInstance, useRestrictionConverter, electionDAO, consentDAO, voteDAO, dacUserDAO, dataSetDAO, researcherPropertyDAO);
        DatabaseConsentAPI.initInstance(jdbi, consentDAO, electionDAO, associationDAO, mongoInstance, voteDAO, dataSetDAO);
        DatabaseMatchAPI.initInstance(matchDAO, consentDAO);
        DatabaseDataSetAPI.initInstance(dataSetDAO, dataSetAssociationDAO, userRoleDAO, consentDAO, dataSetAuditDAO, electionDAO, config.getDatasets());
        DatabaseDataSetAssociationAPI.initInstance(dataSetDAO, dataSetAssociationDAO, dacUserDAO);

        try {
            MailService.initInstance(config.getMailConfiguration());
            EmailNotifierService.initInstance(voteDAO, mongoInstance, electionDAO, dacUserDAO, emailDAO, mailServiceDAO, new FreeMarkerTemplateHelper(config.getFreeMarkerConfiguration()), config.getServicesConfiguration().getLocalURL(), config.getMailConfiguration().isActivateEmailNotifications(), researcherPropertyDAO);
        } catch (IOException e) {
            LOGGER.error("Mail Notification Service initialization error.", e);
        }

        DatabaseMatchingServiceAPI.initInstance(client, config.getServicesConfiguration());
        DatabaseMatchProcessAPI.initInstance(consentDAO, mongoInstance);
        DatabaseSummaryAPI.initInstance(voteDAO, electionDAO, dacUserDAO, consentDAO, dataSetDAO ,matchDAO, mongoInstance, dataSetDAO);
        DatabaseElectionCaseAPI.initInstance(electionDAO, voteDAO, dacUserDAO, userRoleDAO, consentDAO, mongoInstance, dataSetDAO);
        DACUserRolesHandler.initInstance(dacUserDAO, userRoleDAO, electionDAO, voteDAO, dataSetAssociationDAO, AbstractEmailNotifierAPI.getInstance(), AbstractDataAccessRequestAPI.getInstance());
        DatabaseDACUserAPI.initInstance(dacUserDAO, userRoleDAO, electionDAO, voteDAO, dataSetAssociationDAO, AbstractUserRolesHandler.getInstance(), researcherPropertyDAO);
        DatabaseVoteAPI.initInstance(voteDAO, dacUserDAO, electionDAO, dataSetAssociationDAO);
        DatabaseReviewResultsAPI.initInstance(electionDAO, voteDAO, consentDAO);
        TranslateServiceImpl.initInstance(useRestrictionConverter);
        DatabaseHelpReportAPI.initInstance(helpReportDAO, userRoleDAO);
        DatabaseApprovalExpirationTimeAPI.initInstance(approvalExpirationTimeDAO, dacUserDAO);
        UseRestrictionValidator.initInstance(client, config.getServicesConfiguration(), consentDAO);
        OAuthAuthenticator.initInstance();

        // Mail Services
        DatabaseElectionAPI.initInstance(electionDAO, consentDAO, dacUserDAO, mongoInstance, voteDAO, emailDAO, dataSetDAO);
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        configureCors(env);

        // Health Checks
        env.healthChecks().register("mongodb", new MongoHealthCheck(mongoInstance.getMongoClient(), config.getMongoConfiguration().getDbName()));
        env.healthChecks().register("google-cloud-storage", new GCSHealthCheck(googleStore));
        env.healthChecks().register("elastic-search", new ElasticSearchHealthCheck(config.getElasticSearchConfiguration()));

        final StoreOntologyService storeOntologyService
                = new StoreOntologyService(googleStore,
                config.getStoreOntologyConfiguration().getBucketSubdirectory(),
                config.getStoreOntologyConfiguration().getConfigurationFileName());


        final IndexOntologyService indexOntologyService = new IndexOntologyService(config.getElasticSearchConfiguration());
        final IndexerService indexerService = new IndexerServiceImpl(storeOntologyService, indexOntologyService);
        final ResearcherAPI researcherAPI = new DatabaseResearcherAPI(researcherPropertyDAO, dacUserDAO, AbstractEmailNotifierAPI.getInstance());
        final UserAPI userAPI = new DatabaseUserAPI(dacUserDAO, userRoleDAO, electionDAO, voteDAO, dataSetAssociationDAO, AbstractUserRolesHandler.getInstance(), researcherPropertyDAO);
        final NihAuthApi nihAuthApi = new NihServiceAPI(researcherAPI);

        // Now register our resources.
        env.jersey().register(new IndexerResource(indexerService, googleStore));
        env.jersey().register(new DataAccessRequestResource(DatabaseDACUserAPI.getInstance(), DatabaseElectionAPI.getInstance(), googleStore));
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
        env.jersey().register(new DacResource(dacService));
        env.jersey().register(DACUserResource.class);
        env.jersey().register(ElectionReviewResource.class);
        env.jersey().register(ConsentManageResource.class);
        env.jersey().register(new ElectionResource(voteService));
        env.jersey().register(MatchResource.class);
        env.jersey().register(EmailNotifierResource.class);
        env.jersey().register(HelpReportResource.class);
        env.jersey().register(ApprovalExpirationTimeResource.class);
        env.jersey().register(new UserResource(userAPI));
        env.jersey().register(new ResearcherResource(researcherAPI));
        env.jersey().register(WorkspaceResource.class);
        env.jersey().register(new DataAccessAgreementResource(googleStore, researcherAPI));
        env.jersey().register(new SwaggerResource(config.getGoogleAuthentication()));
        env.jersey().register(new NihAccountResource(nihAuthApi, DatabaseDACUserAPI.getInstance()));
        env.jersey().register(injector.getInstance(VersionResource.class));

        // Authentication filters
        AuthFilter defaultAuthFilter = new DefaultAuthFilter.Builder<AuthUser>()
                .setAuthenticator(new DefaultAuthenticator())
                .setRealm(" ")
                .buildAuthFilter();
        List<AuthFilter> filters = Lists.newArrayList(
                defaultAuthFilter,
                new BasicCustomAuthFilter(new BasicAuthenticator(config.getBasicAuthentication())),
                new OAuthCustomAuthFilter(AbstractOAuthAuthenticator.getInstance(), userRoleDAO));
        env.jersey().register(new AuthDynamicFeature(new ChainedAuthFilter(filters)));
        env.jersey().register(RolesAllowedDynamicFeature.class);
        env.jersey().register(new AuthValueFactoryProvider.Binder<>(AuthUser.class));
        env.jersey().register(new StatusResource(env.healthChecks()));
        env.jersey().register(new DataRequestReportsResource(researcherAPI, DatabaseDACUserAPI.getInstance()));
        // Register a listener to catch an application stop and clear out the API instance created above.
        // For normal exit, this is a no-op, but the junit tests that use the DropWizardAppRule will
        // repeatedly start and stop the application, all within the same JVM, causing the run() method to be
        // called multiple times.
        env.lifecycle().addLifeCycleListener(new AbstractLifeCycle.AbstractLifeCycleListener() {

            @Override
            public void lifeCycleStopped(LifeCycle event) {
                LOGGER.debug("**** ConsentApplication Server Stopped ****");
                AbstractTranslateService.clearInstance();
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
                AbstractOAuthAuthenticator.clearInstance();
                AbstractAuditServiceAPI.clearInstance();
                super.lifeCycleStopped(event);
            }
        });
    }

    @Override
    public void initialize(Bootstrap<ConsentConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets/", "/api-docs", "index.html"));
        bootstrap.addBundle(new MultiPartBundle());
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
        filter.setInitParameter("allowedHeaders", "X-Requested-With,Content-Type,Accept,Origin,Authorization,Content-Disposition,Access-Control-Expose-Headers,Pragma,Cache-Control,Expires,X-App-ID");
        filter.setInitParameter("exposeHeaders", "Content-Type,Pragma,Cache-Control,Expires");
        filter.setInitParameter("allowCredentials", "true");
    }
}