package com.pslcl.dtf.runner;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.daemon.DaemonInitException;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.dtf.core.runner.config.RunnerConfig;
import com.pslcl.dtf.core.runner.resource.ResourceNames;
import com.pslcl.dtf.core.util.StrH;

public class DBConnPool {
    private volatile String host;
    private volatile String user;
    private volatile String password;
    private boolean read_only; // a status indicator, reflecting setup of the MySQLServer we connect to
	private BasicDataSource pool;
    private final Logger log;
    private final String simpleName;
	
	/**
	 * 
	 */
	public DBConnPool() {
        this.log = LoggerFactory.getLogger(getClass());
        this.simpleName = getClass().getSimpleName() + " ";
		pool = new BasicDataSource();
	}
	
	public Connection getConnection() throws SQLException {
		return pool.getConnection();
	}
	
	public boolean getReadOnly() {
		return this.read_only;
	}
	
	/**
	 * Determine the DB host, user, and password, and setup the MySQL connection pool.
	 * 
     * Note: Host configuration is required.
     * Note: User and password configurations are optional, and if not specified then a guest account is used. This sets the 'read_only' flag, which disables all database modifications.
	 * Note: This init() call must be paired with the destroy() call
	 * 
	 * @param config Must not be null
	 */
	void init(RunnerConfig config) throws DaemonInitException {
		Properties properties = config.properties;
		if (properties != null) {
			// config from file
			host = properties.getProperty(ResourceNames.DbHostKey);
            host = StrH.trim(host);
			user = properties.getProperty(ResourceNames.DbUserKey);
			user = StrH.trim(user);
			password = properties.getProperty(ResourceNames.DbPassKey);
			password = StrH.trim(password);
			if(password == null)
			    password = "";
		}

		if (host==null || host.isEmpty())
			throw new DaemonInitException("incomplete runner db config");
		
		if (user!=null && password!=null) {
			read_only = false;
		} else {
			log.warn(simpleName + "configured for guest database user with readonly access");
            user = "guest";
            password = "";
		}

		// setup connection pool
        pool.setDriverClassName("java.sql.DriverManager");
        String dbUrl = "jdbc:mysql://"+host+"/qa_portal?user="+user+"&password="+password;
        pool.setUrl(dbUrl);
		pool.setUsername(user);
		pool.setPassword(password);
		pool.setInitialSize(1);
	}
	
	
	
	/**
	 *
	 */
	void destroy() {
	}
	
}