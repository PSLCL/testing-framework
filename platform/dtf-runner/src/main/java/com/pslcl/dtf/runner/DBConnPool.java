package com.pslcl.dtf.runner;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.daemon.DaemonInitException;
import org.apache.commons.dbcp.BasicDataSource;

import com.pslcl.dtf.core.runner.config.RunnerConfig;

public class DBConnPool {
	
    public static final String RunnerDBHostKey = "pslcl.dtf.runner.dbconn.host";
    public static final String RunnerDBUserKey = "pslcl.dtf.runner.dbconn.user";
    public static final String RunnerDBPasswordKey = "pslcl.dtf.runner.dbconn.password";
    public static final String RunnerDBPasswordDefault = "";
    
    private volatile String host;
    private volatile String user;
    private volatile String password;
    private boolean read_only; // a status indicator, reflecting setup of the MySQLServer we connect to
	private BasicDataSource pool;
	
	/**
	 * 
	 */
	public DBConnPool() {
		pool = new BasicDataSource();
	}
	
	public Connection getConnection() throws SQLException {
		return pool.getConnection();
	}
	
	/**
	 * Determine the DB host, user, and password, and setup the MySQL connection pool.
	 * 
     * @note Host configuration is required.
     * @note User and password configurations are optional, and if not specified then a guest account is used. This sets the 'read_only' flag, which disables all database modifications.
	 * @note This init() call must be paired with the destroy() call
	 * 
	 * @param config Must not be null
	 */
	void init(RunnerConfig config) throws DaemonInitException {
		Properties properties = config.properties;
		if (properties != null) {
			// config from file
			host = properties.getProperty(RunnerDBHostKey);
			user = properties.getProperty(RunnerDBUserKey);
			password = properties.getProperty(RunnerDBPasswordKey, RunnerDBPasswordDefault);
		}
		
		if (false) // temporarily, and only if needed (something is still null), replace with db access with local machine environment variables
		{
			if (host==null || user==null) { // password config is not required
				// config from environment variables
		        host = System.getenv("DTF_TEST_DB_HOST");
		        user = System.getenv("DTF_TEST_DB_USER");
		        password = System.getenv("DTF_TEST_DB_PASSWORD");
			}
		}

		if (host==null || host.isEmpty())
			throw new DaemonInitException("incomplete runner db config");
		
		if (user!=null && password!=null) {
			read_only = false;
		} else {
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