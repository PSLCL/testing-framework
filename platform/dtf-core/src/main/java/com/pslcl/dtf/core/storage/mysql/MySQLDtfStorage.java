package com.pslcl.dtf.core.storage.mysql;

import com.pslcl.dtf.core.Core;
import com.pslcl.dtf.core.PortalConfig;
import com.pslcl.dtf.core.generator.template.DescribedTemplate;
import com.pslcl.dtf.core.storage.DTFStorage;

import java.util.Optional;

public class MySQLDtfStorage implements DTFStorage {
    private PortalConfig config;

    public MySQLDtfStorage(PortalConfig config){
        this.config = config;
        this.openDatabase();
    }

    private void openDatabase(){
        //TODO Create connection pool.
    }

    @Override
    public boolean describedTemplateHasTestInstanceMatch(long pkDescribedTemplate) throws Exception {
        return false;
    }

    @Override
    public Optional<Core.DBDescribedTemplate> getDBDescribedTemplate(DescribedTemplate.Key matchKey) throws Exception {
        return Optional.empty();
    }
}
