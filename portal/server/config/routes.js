var routes    = require('../routes');
var comp      = require('../routes/module');
var test      = require('../routes/test');
var plan      = require('../routes/test_plan');
var version   = require('../routes/version');
var artifact  = require('../routes/artifact');

module.exports = function (app, config, passport) {
  require('./middlewares/auth')(passport,config);

  var auth = function(req, res, next) {
    if (!req.isAuthenticated())
      res.send(401);
    else
      next();
  };
  
  // Root route
  app.get('/', routes.index );

  // API routes
  // Anonymous routes
  app.get('/api/v1/stats',                              routes.index);
  app.get('/api/v1/modules',                         comp.list);
  app.get('/api/v1/modules/:id',                     comp.show);
  app.get('/api/v1/test_plans',                         plan.list);
  app.get('/api/v1/test_plans/:pk_test_plan',           plan.show);
  app.get('/api/v1/test_plans/:pk_test_plan/tests',     test.list);
  app.get('/api/v1/test_plans/:pk_test_plan/tests/:id', test.show);
  app.get('/api/v1/versions',                           version.list);
  app.get('/api/v1/report_versions',                    version.report);
  app.get('/api/v1/report_descriptions',                version.descriptions);
  app.get('/api/v1/report_test_plans',                  plan.report);
  app.get('/api/v2/report_test_plans',                  plan.new_report);

  // Report routes (anonymous)
  app.get('/report/plan/:module',                    plan.name_report);
  app.get('/report/result/:module',                  version.name_report);
  app.get('/report/result/:module/:version',         version.name_report);

  // Artifact routes (anonymous)
  app.get('/artifact/:artifactid',                      artifact.single);
  app.get('/artifacts/:instanceid',                     artifact.multiple);

  // Authentication routes.
  app.get('/auth/atlassian-oauth',
        passport.authenticate('atlassian-oauth'), function (req,res) {});
  app.get('/auth/atlassian-oauth/callback',
        passport.authenticate('atlassian-oauth', { failureRedirect: '/login' }),
           function(req,res) {
                res.redirect('/');
           });
  app.get('/loggedin', function(req,res) {
    var result = {};
    if ( req.isAuthenticated() ) {   
        var isAdmin = false;
        var displayName = "";
        var avatar = null;
    
        if ( req.user.groups.indexOf('testing-admin') > 0 )
            isAdmin = true;
    
        displayName = req.user.displayName;
        avatar = req.user.avatarUrls['32x32'];
    
        result = { "isAdmin": isAdmin, "username": req.user.username, "displayName": displayName, "avatar": avatar, "user": result };
    }
    
    res.send( result );
  });
  app.get('/logout', function(req,res) {
    req.logout();
    res.redirect('/');
  });
  
  // Authenticated Routes
  app.get('/api/v1/stats/admin',                 auth, routes.admin_index);
  app.post  ('/api/v1/modules',                  auth, comp.create);
  app.post  ('/api/v1/modules/:id',              auth, comp.update);
  app.delete('/api/v1/modules/:id',              auth, comp.destroy);

  // Test Plan routes
  app.post  ('/api/v1/test_plans',               auth, plan.create);
  app.post  ('/api/v1/test_plans/:pk_test_plan', auth, plan.update);
  app.delete('/api/v1/test_plans/:pk_test_plan', auth, plan.destroy);

  // Add/Remove Test Plan from modules
  app.delete('/api/v1/module/test_plan',         auth, comp.remove_test_plan);
  app.post  ('/api/v1/module/test_plan',         auth, comp.add_test_plan);

  // Test Routes
  app.post  ('/api/v1/test_plans/:pk_test_plan/tests',  auth, test.create);
  app.post  ('/api/v1/test_plans/:pk_test_plan/tests/:id',  auth, test.update);
  app.delete('/api/v1/test_plans/:pk_test_plan/tests/:id',  auth, test.destroy);

  // Report Result
  app.post('/api/v1/report_result', auth, test.report_result);

  // development error handler
  // will print stack trace
  if (app.get('env') === 'development') {
    if (config.enable_livereload) {
      var livereload = require('express-livereload');
      livereload(app, config);
    }
    app.use(function (err, req, res) {
      res.render('error', {
        message: err.message,
        error: err
      });
    });
  }
};