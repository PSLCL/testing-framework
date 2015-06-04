var BasicStrategy = require('gt-passport-http').BasicStrategy;
var LdapAuth      = require('ldapauth');

//Authentication Strategy
module.exports = function (passport,config) {
  // Production environment uses LDAP for authentication.
  passport.use(new BasicStrategy({ disableBasicChallenge: true },
      function (userid, password, done) { // (development only)
    /** @namespace process.env.NODE_ENV */

    // LDAP
    var ldap = new LdapAuth({
      url: config.ldap.url,
      adminDn: config.ldap.adminDn,
      adminPassword: config.ldap.adminPassword,
      searchBase: config.ldap.searchBase,
      searchFilter: config.ldap.searchFilter,
      cache: true
    });

    ldap.authenticate(userid, password, function (err, user) {
      if (err) {
        console.log("LDAP auth error: %s", err);
        return done(null, false);
      }

      return done(null, user);
    });
  }
  ));
};