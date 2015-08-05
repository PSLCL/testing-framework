// Common environment configuration variables

// STEPS FOR SETUP:
//   * Configure MySQL parameters.
//   * Generate a random string for cookiesecret, replace below.
//   * If using oauth, configure below. You will need to configure JIRA with an application.
//   * Generate your certificate and put the value below.
//   * Enable development https by setting development.https_port if desired.

var all = {
  // MySQL configuration
  /** @namespace process.env.MYSQL_PORT */
  /** @namespace process.env.MYSQL_USER */
  /** @namespace process.env.MYSQL_PASS */
  /** @namespace process.env.MYSQL_DB */
  /** @namespace process.env.MYSQL_HOST */
  mysql: {
    host: process.env.MYSQL_HOST || 'localhost',
    port: process.env.MYSQL_PORT || 3306,
    user: process.env.MYSQL_USER || 'root',
    password: process.env.MYSQL_PASS || '',
    db: process.env.MYSQL_DB || 'qa_portal'
  },
  // Page size for infinite scrolling default
  page_limit: 200,

  // This should be randomly generated for your site.
  cookiesecret: 'randomly-generate-me'
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

module.exports = {
  development: {
    enable_livereload: true,
    mysql: all.mysql,
    page_limit: all.page_limit,
    cookiesecret: all.cookiesecret,
	'certificate_passphrase': certificate_passphrase,
	http_port: 80,
	https_port: null,
	'oauth': oauth
  },
  production: {
    enable_livereload: false,
    mysql: all.mysql,
    page_limit: all.page_limit,
    cookiesecret: all.cookiesecret,
	'certificate_passphrase': certificate_passphrase,
	http_port: 80,
	https_port: 443,
	'oauth': oauth
  }
};