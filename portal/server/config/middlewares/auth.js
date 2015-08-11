var AtlassianOAuthStrategy = require('passport-atlassian-oauth').Strategy;
var JiraApi       = require('jira').JiraApi;
var config   = require('../../../config/config');

//Authentication Strategy
module.exports = function (passport,config) {
  passport.serializeUser(function(user,done) {
    done(null, user);
  });
  
  passport.deserializeUser(function(obj, done) {
    done(null, obj);
  });
  
  passport.use(new AtlassianOAuthStrategy( config.oauth,
      function (token, tokenSecret, profile, done) {
        return done(null, profile);
      }
  ));
};