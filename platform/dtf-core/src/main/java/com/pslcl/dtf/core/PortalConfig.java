package com.pslcl.dtf.core;

import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;

public class PortalConfig {
    private String db_host = null;
    private Integer db_port = null;
    private String db_user = null;
    private String db_password = null;
    private String db_schema = null;
    private String artifacts_dir = null;
    private String generators_dir = null;
    private String shell = null;
    private String sqs_endpoint = null;
    private String sqs_queue_name = null;
    private String sqs_access_key_id = null;
    private String sqs_secret_access_key = null;

    PortalConfig() {
        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("JavaScript");
            Bindings binding = engine.createBindings();
            binding.put("process", null);
            binding.put("env", System.getenv());
            binding.put("module", new NodeModule());
            binding.put("home_dir", Paths.get("").toAbsolutePath().normalize().toString());

            File fs = new File("portal/config/config.js");
            engine.eval(new FileReader(fs), binding);
            this.db_host = (String) engine.eval("config.mysql.host;", binding);
            this.db_port = (Integer) engine.eval("config.mysql.port;", binding);
            this.db_user = (String) engine.eval("config.mysql.user;", binding);
            this.db_password = (String) engine.eval("config.mysql.password;", binding);
            this.db_schema = (String) engine.eval("config.mysql.db;", binding);
            this.artifacts_dir = (String) engine.eval("config.artifacts_dir;", binding);
            this.generators_dir = (String) engine.eval("config.generators_dir;", binding);
            this.shell = (String) engine.eval("config.shell;", binding);
            this.sqs_endpoint = (String) engine.eval("config.sqs.endpoint;", binding);
            this.sqs_queue_name = (String) engine.eval("config.sqs.queue_name;", binding);
            this.sqs_access_key_id = (String) engine.eval("config.sqs.access_key_id;", binding);
            this.sqs_secret_access_key = (String) engine.eval("config.sqs.secret_access_key;", binding);
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass()).warn("<internal> Core.Config constructor exception, msg: " + e);
        }
    }

    String dbHost() {
        return db_host;
    }

    Integer dbPort() {
        return db_port;
    }

    String dbUser() {
        return db_user;
    }

    String dbPassword() {
        return db_password;
    }

    String dbSchema() {
        return db_schema;
    }

    String dirArtifacts() {
        return artifacts_dir;
    }

    String dirGenerators() {
        return generators_dir;
    }

    String shell() {
        return shell;
    }

    String sqsEndpoint() {
        return sqs_endpoint;
    }

    String sqsQueueName() {
        return sqs_queue_name;
    }

    String sqsAccessKeyID() {
        return sqs_access_key_id;
    }

    String sqsSecretAccessKey() {
        return sqs_secret_access_key;
    }

    private static class NodeModule {
        //TODO: Is this still needed?
    }
}
