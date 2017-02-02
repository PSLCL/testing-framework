// Common environment configuration variables

// STEPS FOR SETUP:
//   * Configure MySQL parameters.
//   * If desired, change the artifacts_dir and generators_dir.
//   * Generate a random string for cookiesecret, replace below.
//   * Change the generator schedule if desired.
//   * If using oauth, configure below. You will need to configure JIRA with an application.
//   * Generate your certificate and put the value below.
//   * Enable development https by setting development.https_port if desired.

var env, home_dir;
if ( process != null ){
  env = process.env;
  home_dir = env.DTF_EXEC_HOME || process.cwd() + "/..";
}

var E = env.NODE_ENV || 'production';

var all = {
  // MySQL configuration
  mysql: {
    host: env.DTF_DB_HOST || 'localhost',
    port: env.DTF_DB_PORT || 3306,
    user: env.DTF_DB_USER || 'root',
    password: env.DTF_DB_PASSWORD || '',
    db: env.DTF_DB_SCHEMA || 'qa_portal'
  },

  // AWS SQS configuration
  sqs: {
    endpoint: env.DTF_SQS_ENDPONT || 'https://sqs.us-east-1.amazonaws.com',
	queue_name: env.DTF_SQS_QUEUE_NAME || '',
	access_key_id: env.AWS_ACCESS_KEY_ID || '',
	secret_access_key: env.AWS_SECRET_ACCESS_KEY || ''
  },

  home_dir: home_dir,
  artifacts_dir: env.DTF_ARTIFACTS || home_dir + '/artifacts',
  generators_dir: env.DTF_GENERATORS || home_dir + '/generators',
  shell: env.DTF_SHELL || '/bin/bash',

  // Page size for infinite scrolling default
  page_limit: 200,

  // This should be randomly generated for your site.
  cookiesecret: 'randomly-generate-me',

  // Default schedule is to synchronize every 15 minutes, and to prune
  // missing modules after 40 successful synchronizes without finding the module.
  synchronize_schedule: '*/15 * * * *',

  // The time zone to use when setting the synchronize schedule.
  // If set to null, the default time zone will be used, Greenwich Mean Time.
  // Time zone format example: 'America/Los_Angeles'. For more information see http://momentjs.com/timezone/
  synchronize_schedule_time_zone: null,
  
  // JVM args for synchronize process.
  synchronize_jvm_args: "-Xms1024m -Xmx1024m",
  
  // The number of generator processes that may run in parallel during synchronize.
  generator_process_count: 5,

  // The number of synchronize runs without finding the module at which to prune, or null
  // to disable pruning.
  prune: 40
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
	sqs: all.sqs,
    page_limit: all.page_limit,
    cookiesecret: all.cookiesecret,
    'synchronize_schedule': all.synchronize_schedule,
	'synchronize_schedule_time_zone': all.synchronize_schedule_time_zone,
	synchronize_jvm_args: all.synchronize_jvm_args,
    generator_process_count: all.generator_process_count,
    'certificate_passphrase': certificate_passphrase,
    listen_ip: '0.0.0.0',
    http_port: 80,
    https_port: null,
    'oauth': oauth,
    artifacts_dir: all.artifacts_dir,
    generators_dir: all.generators_dir,
    shell: all.shell,
    prune: all.prune,
    home_dir: all.home_dir
  },
  production: {
    enable_livereload: false,
    mysql: all.mysql,
	sqs: all.sqs,
    page_limit: all.page_limit,
    cookiesecret: all.cookiesecret,
    'synchronize_schedule': all.synchronize_schedule,
	'synchronize_schedule_time_zone': all.synchronize_schedule_time_zone,
    synchronize_jvm_args: all.synchronize_jvm_args,
    generator_process_count: all.generator_process_count,
    'certificate_passphrase': certificate_passphrase,
    listen_ip: '0.0.0.0',
    http_port: 80,
    https_port: 443,
    'oauth': oauth,
    artifacts_dir: all.artifacts_dir,
    generators_dir: all.generators_dir,
    shell: all.shell,
    prune: all.prune,
    home_dir: all.home_dir
  }
};

var config = configs[E];
module.exports = config;
