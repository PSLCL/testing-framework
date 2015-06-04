var routes    = require('../routes');
var comp      = require('../routes/component');
var test      = require('../routes/test');
var plan      = require('../routes/test_plan');
var version   = require('../routes/version');
var artifact  = require('../routes/artifact');

module.exports = function (app, config, passport) {
  require('./middlewares/auth')(passport,config);

  // Root route
  app.get('/', routes.index);

  // API routes
  // Anonymous routes
  app.get('/api/v1/stats',                              routes.index);
  app.get('/api/v1/components',                         comp.list);
  app.get('/api/v1/components/:id',                     comp.show);
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
  app.get('/report/plan/:component',                    plan.name_report);
  app.get('/report/result/:component',                  version.name_report);
  app.get('/report/result/:component/:version',         version.name_report);

  // Artifact routes (anonymous)
  app.get('/artifact/:artifactid',                      artifact.single);
  app.get('/artifacts/:instanceid',                     artifact.multiple);

  // Authenticated Routes
  app.get('/api/v1/stats/admin',
      passport.authenticate('basic',{ session: false }), routes.admin_index);
  // Component routes
  app.post  ('/api/v1/components',
      passport.authenticate('basic', { session: false }), comp.create);
  app.post  ('/api/v1/components/:id',
      passport.authenticate('basic', { session: false }), comp.update);
  app.delete('/api/v1/components/:id',
      passport.authenticate('basic', { session: false }), comp.destroy);

  // Test Plan routes
  app.post  ('/api/v1/test_plans',
      passport.authenticate('basic', { session: false }), plan.create);
  app.post  ('/api/v1/test_plans/:pk_test_plan',
      passport.authenticate('basic', { session: false }), plan.update);
  app.delete('/api/v1/test_plans/:pk_test_plan',
      passport.authenticate('basic', { session: false }), plan.destroy);

  // Add/Remove Test Plan from components
  app.delete('/api/v1/component/test_plan',
      passport.authenticate('basic', { session: false}), comp.remove_test_plan);
  app.post  ('/api/v1/component/test_plan',
      passport.authenticate('basic', { session: false}), comp.add_test_plan);

  // Test Routes
  app.post  ('/api/v1/test_plans/:pk_test_plan/tests',
      passport.authenticate('basic', { session: false }), test.create);
  app.post  ('/api/v1/test_plans/:pk_test_plan/tests/:id',
      passport.authenticate('basic', { session: false }), test.update);
  app.delete('/api/v1/test_plans/:pk_test_plan/tests/:id',
      passport.authenticate('basic', { session: false }), test.destroy);

  // Report Result
  app.post('/api/v1/report_result',
      passport.authenticate('basic', { session: false }), test.report_result);

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