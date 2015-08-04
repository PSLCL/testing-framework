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
  
var RsaPrivateKey = "";

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