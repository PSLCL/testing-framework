// Common environment configuration variables
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
  //TODO: Should this be defined here, or forced to be external?
  cookiesecret: '098f6bcd4621d373c0d34e832627b4f6'
};

module.exports = {
  development: {
    enable_livereload: true,
    mysql: all.mysql,
    page_limit: all.page_limit,
    cookiesecret: all.cookiesecret
  },
  production: {
    enable_livereload: false,
    // LDAP configuration uses environment variables or defaults
    /** @namespace process.env.LDAP_ADN */
    /** @namespace process.env.LDAP_PASS */
    /** @namespace process.env.LDAP_BASE */
    /** @namespace process.env.LDAP_FILTER */
    /** @namespace process.env.LDAP_HOST */
    ldap: {
      url:           process.env.LDAP_HOST   || 'ldap://localhost',
      adminDn:       process.env.LDAP_ADN    || 'cn=Manager,dc=example,dc=com',
      adminPassword: process.env.LDAP_PASS   || 'secret',
      searchBase:    process.env.LDAP_BASE   || 'ou=people,dc=example,dc=com',
      searchFilter:  process.env.LDAP_FILTER || '(uid={{username}})'
    },
    mysql: all.mysql,
    page_limit: all.page_limit,
    cookiesecret: all.cookiesecret
  }
};