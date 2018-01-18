package com.pslcl.dtf.runner.rest.storage;

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.rest.module.Artifacts;
import com.pslcl.dtf.core.runner.rest.module.ModuleDetail;
import com.pslcl.dtf.core.runner.rest.module.Modules;
import com.pslcl.dtf.core.runner.rest.module.Reports;
import com.pslcl.dtf.core.runner.rest.runRates.RunRates;
import com.pslcl.dtf.core.runner.rest.stats.Statistics;
import com.pslcl.dtf.core.runner.rest.userTest.UserTests;

import java.util.concurrent.CompletableFuture;

public interface RestServiceStorage
{
    void init(RunnerConfig config) throws Exception;
    void start();
    void destroy();
    CompletableFuture<Statistics> getStatistics();

    /**
     * Return the starting, running and complete counts.
     * @param from starting timestamp in ms from epoch.  Maybe null, defaults to current time minus 1 week.
     * @param to ending timestamp in ms from epoch. Maybe null, defaults to current time.
     * @param bucket The modulus in seconds to apply to and subtract from the ‘from’ and ‘to’ values. Maybe null, defaults to 3600 (1hr).
     * @return RunRates for the given time range and bucket size.
     */
    CompletableFuture<RunRates> getRunRates(Long from, Long to, Long bucket);

    /**
     * Return User Tests for given owner.
     * @param owner The owner to return tests for.  Maybe null.
     * @return All user tests if owner is null, otherwise user tests for given owner.
     */
    CompletableFuture<UserTests> getUserTests(String owner);

    /**
     * Return the modules.
     * @param filter a 'like' filter that will be or searched against name, organization, attributes, version and sequence.  May be null.
     * @param order Order by where the value must be one of 'plans', 'tests' or 'name'.  May be null.
     *              The value may be preceeded by a '<' or '>'  If '<' order is ascending.  If '>' order is decending.  Defaults to "<name".
     *              If ordering by 'name', the following are ordered in this order; 'organization', 'name', 'attributes', ' version', 'sequence' and 'pk_module'.
     *              Note that pk_module is always returned in ascending order regardless of the '>' flag.
     * @param limit Maximum number of modules to return. Can be equal to "all". Defaults to the portals configuration ‘page_limit’ parameter (200).
     *              If limit is given and set to “all” and the ‘offset’ parameter is also given, the limit is set to 100000.
     * @param offset The offset in the order to list from.
     * @return the requested modules list
     */
    CompletableFuture<Modules> getModules(String filter, String order, String limit, Integer offset);

    /**
     * Return the requested module
     * @param moduleId the desired module ID.
     * @return the requested module
     */
    CompletableFuture<ModuleDetail> getModule(String moduleId);

    /**
     * Return the requested module
     * @param moduleId the desired module ID.  Must not be null.
     * @param filter A ‘like’ filter that will be or searched against name and configuration if given.  May be null.
     * @param order The value must be one of 'plans’, 'tests’, ‘configuration’ or 'name’.  May be null.
     *              The value may be preceeded by a ‘<’ or ‘>’ If ‘<’ order is ascending. If ‘>’ order is decending. Defaults to "<name".
     *              Note that pk_artifact is always returned in ascending order regardless of the ‘>’ flag.
     * @param limit Maximum number of artifacts to return. Can be equal to "all". Maybe null. Defaults to the portals configuration ‘page_limit’ parameter (200).
     *              If limit is given and set to “all” and the ‘offset’ parameter is also given, the limit is set to 100000.
     * @param offset The offset in the order to list from.  May be null.
     * @return the requested artifacts list
     */
    CompletableFuture<Artifacts> getArtifactsForModule(String moduleId, String filter, String order, String limit, Integer offset);

    /**
     * Return the requested module
     * @param moduleId the desired module ID.
     * @return the report structure for the requested module
     */
    CompletableFuture<Reports> getReportsForModule(String moduleId);
}
