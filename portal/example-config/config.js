// Common environment configuration variables

// STEPS FOR SETUP:
//   * Configure MySQL parameters.
//   * If desired, change the artifacts_dir and generators_dir.
//   * Generate a random string for cookiesecret, replace below.
//   * Change the generator schedule if desired.
//   * If using oauth, configure below. You will need to configure JIRA with an application.
//   * Generate your certificate and put the value below.
//   * Enable development https by setting development.https_port if desired.

if ( process != null )
	env = process.env;

var E = env.NODE_ENV || 'production';

var all = {
  // MySQL configuration
  mysql: {
    host: env.DTF_TEST_DB_HOST || 'localhost',
    port: env.DTF_TEST_DB_PORT || 3306,
    user: env.DTF_TEST_DB_USER || 'root',
    password: env.DTF_TEST_DB_PASSWORD || '',
    db: env.DTF_TEST_DB_SCHEMA || 'qa_portal'
  },
  
  artifacts_dir: env.DTF_TEST_ARTIFACTS || 'artifacts',
  generators_dir: env.DTF_TEST_GENERATORS || 'generators',
  shell: env.DTF_TEST_SHELL || '/bin/bash',
  
  // Page size for infinite scrolling default
  page_limit: 200,

  // This should be randomly generated for your site.
  cookiesecret: 'randomly-generate-me',
  synchronize_schedule: '*/15 * * * *'
};

var oauth = {
	applicationURL: "https://oath-hostname",
	callbackURL: "https://this-hostname/auth/atlassian-oauth/callback",
	consumerKey: "key",
	consumerSecret: "-----BEGIN RSA PRIVATE KEY-----\n" +
		"lines-of-key-text\n" +
		"-----END RSA PRIVATE KEY-----"
};

var certificate_passphrase = 'password';

var configs = {
  development: {
    enable_livereload: true,
    mysql: all.mysql,
    page_limit: all.page_limit,
    cookiesecret: all.cookiesecret,
	'synchronize_schedule': all.synchronize_schedule,
	'certificate_passphrase': certificate_passphrase,
	listen_ip: '0.0.0.0',
	http_port: 80,
	https_port: null,
	'oauth': oauth,
	artifacts_dir: all.artifacts_dir,
	generators_dir: all.generators_dir,
	shell: all.shell
  },
  production: {
    enable_livereload: false,
    mysql: all.mysql,
    page_limit: all.page_limit,
    cookiesecret: all.cookiesecret,
	'synchronize_schedule': all.synchronize_schedule,
	'certificate_passphrase': certificate_passphrase,
	listen_ip: '0.0.0.0',
	http_port: 80,
	https_port: 443,
	'oauth': oauth,
	artifacts_dir: all.artifacts_dir,
	generators_dir: all.generators_dir,
	shell: all.shell
  }
};

var config = configs[E];
module.exports = config;