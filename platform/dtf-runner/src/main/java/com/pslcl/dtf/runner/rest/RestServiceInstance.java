package com.pslcl.dtf.runner.rest;

import com.cstkit.common.web.handler.BadMethodHandler;
import com.pslcl.dtf.core.runner.config.RestServiceConfig;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.rest.RestVersion;
import com.pslcl.dtf.runner.rest.handlers.ArtifactsHandler;
import com.pslcl.dtf.runner.rest.handlers.InstancesHandler;
import com.pslcl.dtf.runner.rest.handlers.ModulesHandler;
import com.pslcl.dtf.runner.rest.handlers.RunRatesHandler;
import com.pslcl.dtf.runner.rest.handlers.StatisticsHandler;
import com.pslcl.dtf.runner.rest.handlers.TemplateHandler;
import com.pslcl.dtf.runner.rest.handlers.TestPlansHandler;
import com.pslcl.dtf.runner.rest.handlers.UserTestsHandler;
import com.pslcl.dtf.runner.rest.handlers.VersionsHandler;
import com.pslcl.dtf.runner.rest.storage.RestServiceStorage;
import com.pslcl.dtf.runner.rest.storage.mysql.MySQLRestServiceStorage;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HttpHeaders;
import io.vertx.core.http.HttpMethod;

public class RestServiceInstance
{
    public static final String Utf8 = "UTF-8";
    private static final String ServiceVersion = "1.0.0";
    private static final String ApiVersion = "v1";
    private static final String ApiReportVersion = "1.0";
    private static final String VersionPrefix = "/" + ApiVersion;
    public static final char PathSeparator = '/';
    private static final String PrefixPath = "/api/" + ApiVersion + "/";

    public static final String RestVersionPath = "/api/version";

    // Note: endpoints that return a collection should end in / however we will be supporting both, thus the basically duplicate *NoFinalSlash* registrations
    public static final String StatsPath = PrefixPath + "stats";
    public static final String RunratesPath = PrefixPath + "runrates";
    public static final String UserTestsPath = PrefixPath + "user_tests";
    public static final String ModulesPath = PrefixPath + "modules";
    public static final String ModulePath = ModulesPath + "/:id";
    public static final String Artifacts = "artifacts";
    public static final String ModuleArtifactsPath = ModulePath + "/"+Artifacts;
    public static final String Report = "report";
    public static final String ModuleReportPath = ModulePath + "/"+Report;
    public static final String TestPlansPath = PrefixPath + "test_plans";
    public static final String TestPlanPath = TestPlansPath + "/:plan";
    public static final String TestPlanTestsPath = TestPlanPath + "/tests";
    public static final String TestPlanTestPath = TestPlanTestsPath + "/:id";
    public static final String VersionsPath = PrefixPath + "versions";
    public static final String InstancesPath = PrefixPath + "instances";
    public static final String InstancePath = InstancesPath + "/:id";
    public static final String TemplatesPath = PrefixPath + "template";
    public static final String TemplatePath = TemplatesPath + "/:id";
    public static final String ArtifactsPath = PrefixPath + "artifacts";
    public static final String UntestedArtifactsPath = ArtifactsPath + "/untested";

    private final RunnerConfig config;
    private volatile RestServiceConfig restConfig;
    private final RestServiceStorage storage;
    private final Vertx vertx;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public RestServiceInstance(RunnerConfig config) throws Exception
    {
        if(config == null)
            throw new IllegalArgumentException("config == null");
        this.config = config;
//        Core core = new Core();
//        dtfStorage = core.getStorage();
        storage = new MySQLRestServiceStorage();
        this.vertx = Vertx.vertx();
    }

    public void init(RestServiceConfig config) throws Exception
    {
        restConfig = config;
        storage.init(this.config);
    }

    public void start() throws Exception
    {
        storage.start();
        String identityURL = restConfig.identityServiceUrl;

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        //@formatter:off
        CorsHandler getCors = CorsHandler.create("*")
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.OPTIONS)
                .allowedHeader(HttpHeaders.CONTENT_TYPE)
                .allowedHeader(HttpHeaders.AUTHORIZATION);

        CorsHandler postCors = CorsHandler.create("*")
                .allowedMethod(HttpMethod.POST)
                .allowedMethod(HttpMethod.OPTIONS)
                .allowedHeader(HttpHeaders.CONTENT_TYPE)
                .allowedHeader(HttpHeaders.AUTHORIZATION);

        CorsHandler getPutDeleteCors = CorsHandler.create("*")
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.PUT)
                .allowedMethod(HttpMethod.DELETE)
                .allowedMethod(HttpMethod.OPTIONS)
                .allowedHeader(HttpHeaders.CONTENT_TYPE)
                .allowedHeader(HttpHeaders.AUTHORIZATION);
        //@formatter:on

        String configPrefix = "";
        if(restConfig.prefix != null)
        {
            if(!restConfig.prefix.startsWith("/"))
                throw new Exception("Configuration was given a URI prefix that does not start with '/'");
            configPrefix = restConfig.prefix;
        }
        StatisticsHandler statisticsHandler = new StatisticsHandler(storage, new RestVersion(ServiceVersion, ApiReportVersion));
        router.route(configPrefix + RestVersionPath).handler(getCors);
        router.route(configPrefix + RestVersionPath).handler(BadMethodHandler.create(HttpMethod.GET, HttpMethod.OPTIONS));
        router.route(configPrefix + RestVersionPath).handler(statisticsHandler);

        router.route(configPrefix + StatsPath).handler(getCors);
        router.route(configPrefix + StatsPath).handler(BadMethodHandler.create(HttpMethod.GET, HttpMethod.OPTIONS));
        router.route(configPrefix + StatsPath).handler(statisticsHandler);

        RunRatesHandler runratesHandler = new RunRatesHandler(storage);
        router.route(configPrefix + RunratesPath).handler(getCors);
        router.route(configPrefix + RunratesPath).handler(BadMethodHandler.create(HttpMethod.GET, HttpMethod.OPTIONS));
        router.route(configPrefix + RunratesPath).handler(runratesHandler);

        UserTestsHandler userTestsHandler = new UserTestsHandler(storage);
        router.route(configPrefix + UserTestsPath).handler(getCors);
        router.route(configPrefix + UserTestsPath).handler(BadMethodHandler.create(HttpMethod.GET, HttpMethod.OPTIONS));
        router.route(configPrefix + UserTestsPath).handler(userTestsHandler);

        ModulesHandler modulesHandler = new ModulesHandler(storage);
        router.route(configPrefix + ModulesPath).handler(getCors);
        router.route(configPrefix + ModulesPath).handler(BadMethodHandler.create(HttpMethod.GET, HttpMethod.OPTIONS));
        router.route(configPrefix + ModulesPath).handler(modulesHandler);

        router.route(configPrefix + ModulePath).handler(getCors);
        router.route(configPrefix + ModulePath).handler(BadMethodHandler.create(HttpMethod.GET, HttpMethod.OPTIONS));
        router.route(configPrefix + ModulePath).handler(modulesHandler);

        router.route(configPrefix + ModuleArtifactsPath).handler(getCors);
        router.route(configPrefix + ModuleArtifactsPath).handler(BadMethodHandler.create(HttpMethod.GET, HttpMethod.OPTIONS));
        router.route(configPrefix + ModuleArtifactsPath).handler(modulesHandler);

        router.route(configPrefix + ModuleReportPath).handler(getCors);
        router.route(configPrefix + ModuleReportPath).handler(BadMethodHandler.create(HttpMethod.GET, HttpMethod.OPTIONS));
        router.route(configPrefix + ModuleReportPath).handler(modulesHandler);

        TestPlansHandler testPlansHandler = new TestPlansHandler(storage);
        router.route(configPrefix + TestPlansPath).handler(getCors);
        router.route(configPrefix + TestPlansPath).handler(BadMethodHandler.create(HttpMethod.GET, HttpMethod.OPTIONS));
        router.route(configPrefix + TestPlansPath).handler(testPlansHandler);

        router.route(configPrefix + TestPlanPath).handler(getCors);
        router.route(configPrefix + TestPlanPath).handler(BadMethodHandler.create(HttpMethod.GET, HttpMethod.OPTIONS));
        router.route(configPrefix + TestPlanPath).handler(testPlansHandler);

        router.route(configPrefix + TestPlanTestsPath).handler(getCors);
        router.route(configPrefix + TestPlanTestsPath).handler(BadMethodHandler.create(HttpMethod.GET, HttpMethod.OPTIONS));
        router.route(configPrefix + TestPlanTestsPath).handler(testPlansHandler);

        router.route(configPrefix + TestPlanTestPath).handler(getCors);
        router.route(configPrefix + TestPlanTestPath).handler(BadMethodHandler.create(HttpMethod.GET, HttpMethod.OPTIONS));
        router.route(configPrefix + TestPlanTestPath).handler(testPlansHandler);

        VersionsHandler versionsHandler = new VersionsHandler(config.blockingExecutor, storage);
        router.route(configPrefix + VersionsPath).handler(getCors);
        router.route(configPrefix + VersionsPath).handler(BadMethodHandler.create(HttpMethod.GET, HttpMethod.OPTIONS));
        router.route(configPrefix + VersionsPath).handler(versionsHandler);

        InstancesHandler instancesHandler = new InstancesHandler(storage);
        router.route(configPrefix + InstancesPath).handler(getCors);
        router.route(configPrefix + InstancesPath).handler(BadMethodHandler.create(HttpMethod.GET, HttpMethod.OPTIONS));
        router.route(configPrefix + InstancesPath).handler(instancesHandler);

        router.route(configPrefix + InstancePath).handler(getCors);
        router.route(configPrefix + InstancePath).handler(BadMethodHandler.create(HttpMethod.GET, HttpMethod.OPTIONS));
        router.route(configPrefix + InstancePath).handler(instancesHandler);

        TemplateHandler templateHandler = new TemplateHandler(storage);
        router.route(configPrefix + TemplatePath).handler(getCors);
        router.route(configPrefix + TemplatePath).handler(BadMethodHandler.create(HttpMethod.GET, HttpMethod.OPTIONS));
        router.route(configPrefix + TemplatePath).handler(templateHandler);

        ArtifactsHandler artifactsHandler = new ArtifactsHandler(storage);
        router.route(configPrefix + UntestedArtifactsPath).handler(getCors);
        router.route(configPrefix + UntestedArtifactsPath).handler(BadMethodHandler.create(HttpMethod.GET, HttpMethod.OPTIONS));
        router.route(configPrefix + UntestedArtifactsPath).handler(artifactsHandler);

//        router.route(configPrefix + MetadataPath).handler(getCors);
//        router.route(configPrefix + MetadataPath).handler(BadMethodHandler.create(HttpMethod.GET, HttpMethod.OPTIONS));
//        router.route(configPrefix + MetadataPath).handler(TokenValidationHandler.create(identityURL).requiredScopes(MetadataScopes.METADATA_READ));
//        router.route(configPrefix + MetadataPath).handler(metadataHandler);
//        router.route(configPrefix + VersionPrefix + MetadataPath).handler(getCors);
//        router.route(configPrefix + VersionPrefix + MetadataPath).handler(BadMethodHandler.create(HttpMethod.GET, HttpMethod.OPTIONS));
//        router.route(configPrefix + VersionPrefix + MetadataPath).handler(TokenValidationHandler.create(identityURL).requiredScopes(MetadataScopes.METADATA_READ));
//        router.route(configPrefix + VersionPrefix + MetadataPath).handler(metadataHandler);
//        router.route(configPrefix + MetadataNoFinalSlashPath).handler(getCors);
//        router.route(configPrefix + MetadataNoFinalSlashPath).handler(BadMethodHandler.create(HttpMethod.GET, HttpMethod.OPTIONS));
//        router.route(configPrefix + MetadataNoFinalSlashPath).handler(TokenValidationHandler.create(identityURL).requiredScopes(MetadataScopes.METADATA_READ));
//        router.route(configPrefix + MetadataNoFinalSlashPath).handler(metadataHandler);
//        router.route(configPrefix + VersionPrefix + MetadataNoFinalSlashPath).handler(getCors);
//        router.route(configPrefix + VersionPrefix + MetadataNoFinalSlashPath).handler(BadMethodHandler.create(HttpMethod.GET, HttpMethod.OPTIONS));
//        router.route(configPrefix + VersionPrefix + MetadataNoFinalSlashPath).handler(TokenValidationHandler.create(identityURL).requiredScopes(MetadataScopes.METADATA_READ));
//        router.route(configPrefix + VersionPrefix + MetadataNoFinalSlashPath).handler(metadataHandler);

        String host = restConfig.host.split(":")[0];
        int port = Integer.parseInt(restConfig.host.split(":")[1]);
        vertx.createHttpServer().requestHandler(router::accept).listen(port, host);
        logger.info(getClass().getSimpleName() + " RestServiceInstance started");
    }

    public static String getModuleIdFromRequestPath(String requestPath)
    {
        // /api/v1/modules/29
        if(!requestPath.startsWith(ModulesPath))
            return null;
        int idx = ModulesPath.length();
        if(requestPath.length() == idx)
            return null;
        if(requestPath.charAt(idx) != PathSeparator)
            return null;
        String moduleId = requestPath.substring(++idx);
        idx = moduleId.indexOf(PathSeparator);
        if(idx != -1)
            moduleId = moduleId.substring(0, idx);
        return moduleId;
    }

    public static boolean isArtifactsFromModule(String requestPath)
    {
        // /api/v1/modules/29/artifacts
        if(!requestPath.startsWith(ModulesPath))
            return false;
        int idx = ModulesPath.length();
        if(requestPath.length() == idx)
            return false;
        if(requestPath.charAt(idx) != PathSeparator)
            return false;
        String remainder = requestPath.substring(++idx);
        idx = remainder.indexOf(PathSeparator);
        if(idx == -1)
            return false;
        remainder = remainder.substring(++idx);
        if(remainder.equals(Artifacts))
            return true;
        return false;
    }

    public void destroy()
    {
        if(vertx != null)
            vertx.close();
        if(storage != null)
            storage.destroy();
    }
}
