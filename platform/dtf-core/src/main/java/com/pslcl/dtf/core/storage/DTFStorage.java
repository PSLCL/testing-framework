package com.pslcl.dtf.core.storage;

import com.pslcl.dtf.core.Core;
import com.pslcl.dtf.core.generator.template.DescribedTemplate;
import com.pslcl.dtf.core.generator.template.TestInstance;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The storage interface for the Distributed Testing Framework.
 */
public interface DTFStorage {

    boolean describedTemplateHasTestInstanceMatch(long pkDescribedTemplate) throws Exception;

    Optional<Core.DBDescribedTemplate> getDBDescribedTemplate(DescribedTemplate.Key matchKey) throws Exception;

}
