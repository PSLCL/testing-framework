var AtlassianOAuthStrategy = require('passport-atlassian-oauth').Strategy;
//var LdapAuth      = require('ldapauth');
var JiraApi       = require('jira').JiraApi;

//Authentication Strategy
module.exports = function (passport,config) {
  passport.serializeUser(function(user,done) {
    done(null, user);
  });
  
  passport.deserializeUser(function(obj, done) {
    done(null, obj);
  });
  
var RsaPrivateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
    "MIICXgIBAAKBgQCnyf88d0JtGxuWLAtlirWSrir1tYIk4ZAh4109jy711jOGtt07\n" +
    "bbs9ZC1iSecRI61mvzEEtq9uydXrerAHBtVPeidAp/qsoNoyNeQqq/5v1hY4NzfU\n" +
    "E9CzfkjaIQ240Jom/uG7HWDuL7Vo3QJvoxcAML5gK1Fqotf/U3UVcjP3zQIDAQAB\n" +
    "AoGBAKPTab+rHhOTtTLCXHpPngvSvKVeDO1fw7Ai5CY4CH0GIRc2svnUoU5G4r0m\n" +
    "NVOZxzQcF4tnqBD/Rzx9dSPXOyuidY9LEYL3Mmt32uhoOGLdRfvi517L/ziEGTSq\n" +
    "2Zl4HJH6JJBkc1sX44yQpJr6QIkH7j7rKgzL2YVkoe/NrPKhAkEA2hB8Wa4otx0H\n" +
    "V+RSQTU2mdrdNQNxKPZbbNkdESKeSdPBakU5YONS4x8KOvkhSpJO6scRdIpl9dMp\n" +
    "gO7+4GXvZQJBAMT6frUW3rOqAm5e5pIwi4TPFpfipUJjTCkpYyqgOr7CrQGFGLvz\n" +
    "mco474+j+FsGU1v7EJHUyOfuhsSl6GtOpEkCQQCx5z87/v0SnyktiIWe02YPUWKk\n" +
    "7ID2lT5zaBHxt9eBjbO/Gmi8ZdlqmBlFkZiaeRDPM8dG195lFnhGyzS/KAqNAkAl\n" +
    "maNiMfFk3kkEhH0C3t1jBpzLdWoSWB4LpWjWlCtiioq3xY5JImDy5xdjlesshPKY\n" +
    "lj49eB65lyCGnRzWjvZxAkEArWHbNO3vev06j1zKO2ezaNib5ule7G8eKct3+1Y2\n" +
    "0yMR6D76hPW69/CcQIBYIR59JF4nLA5gkS8H91EGuBorqg==\n" +
    "-----END RSA PRIVATE KEY-----";

  passport.use(new AtlassianOAuthStrategy({
        applicationURL: "https://issue.opendof.org",
        callbackURL: "https://testing.opendof.org/auth/atlassian-oauth/callback",
        consumerKey: "testing-consumer",
        consumerSecret: RsaPrivateKey,
        },
      function (token, tokenSecret, profile, done) {
        return done(null, profile);
      }
  ));
};