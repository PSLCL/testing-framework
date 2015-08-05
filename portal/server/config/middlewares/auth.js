var AtlassianOAuthStrategy = require('passport-atlassian-oauth').Strategy;
var JiraApi       = require('jira').JiraApi;
var env      = process.env.NODE_ENV || 'production';
var config   = require('../../../config/config')[env];

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