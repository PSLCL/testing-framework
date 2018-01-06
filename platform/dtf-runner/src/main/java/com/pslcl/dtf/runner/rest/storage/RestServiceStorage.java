package com.pslcl.dtf.runner.rest.storage;

import com.cstkit.metadata.objects.KeyspacePage;
import com.pslcl.dtf.core.runner.config.RestServiceStorageConfig;
import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.rest.module.Modules;
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

    CompletableFuture<RunRates> getRunRates(Long from, Long to, Long bucket);
    /**
     * Return the starting, running and complete counts.
     * @param from starting timestamp in ms from epoch.  Maybe null, defaults to current time minus 1 week.
     * @param to ending timestamp in ms from epoch. Maybe null, defaults to current time.
     * @param bucket The modulus in seconds to apply to and subtract from the ‘from’ and ‘to’ values. Maybe null, defaults to 3600 (1hr).
     * @return RunRates for the given time range and bucket size.
     */
    /**
     * Return User Tests for given owner.
     * @param owner The owner to return tests for.  Maybe null.
     * @return All user tests if owner is null, otherwise user tests for given owner.
     */
    CompletableFuture<UserTests> getUserTests(String owner);

    /**
     * Return the modules.
     * @param filter a 'like' filter that will be or searched against name, organization, attributes, version and sequence.  May be null.
     * @return Modules
     */
    CompletableFuture<Modules> getModules(String filter, String order, String limit, Integer offset);
}
