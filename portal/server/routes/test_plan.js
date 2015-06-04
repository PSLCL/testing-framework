// Mysql Connection
var mysql   = require('../lib/mysql');
var env     = process.env.NODE_ENV || 'development';
var config  = require('../config/config')[env];
var squel   = require('squel');
var Util    = require('../lib/util');
var path    = require('path');
var phantom = require('phantom');

// [GET] List of test plans
exports.list = function (req, res) {
  var after_id = req.param('after') || 0;
  var filter_str = req.param('filter');

  /**
   * Get list of test plans
   */
  var sql_query = squel.select()
    .field('test_plan.pk_test_plan')
    .field('test_plan.name')
    .field('test_plan.description')
    .field('COUNT(DISTINCT test.pk_test)', 'tests')
    .from('test_plan')
    .left_join('test', null, 'test.fk_test_plan = test_plan.pk_test_plan');

  /**
   * Test Plan search filtering
   */
  var exp = squel.expr();
  if (filter_str) {
    exp.and('test_plan.pk_test_plan > ?')
      .and('test_plan.name LIKE ?');
    sql_query.where(exp, after_id, "%" + filter_str.replace(/["']/g, "") + "%");
  } else {
    exp.and('test_plan.pk_test_plan > ?');
    sql_query.where(exp, after_id);
  }

  /**
   * Group by
   */
  sql_query.group('test_plan.pk_test_plan');

  /**
   * Order by sort parameter
   */
  switch(req.param('sort_by')) {
    case 'tests':
      sql_query.order('tests',false);
      break;
    case 'name':
      sql_query.order('test_plan.name');
      break;
    case 'associated':
    case 'id':
    default:
      sql_query.order('test_plan.pk_test_plan');
  }

  /**
   * Limit of records by global page_limit
   */
  sql_query.limit(config.page_limit);

  mysql.getConnection(function(err,conn) {
    conn.query(sql_query.toString(),
      function (err, result) {
        if (err) throw err;
        res.format({
          'text/html': function () {
            res.send(result);
          },
          'application/json': function () {
            res.send(result);
          }
        });
      }
    );
    conn.release();
  });
};

// [GET] individual test plan
exports.show = function (req, res) {
  var after_id = 0;
  var filter_str = req.param('filter');
  if (req.param('after')) {
    after_id = req.param('after');
  }

  mysql.getConnection(function(err,conn) {
    conn.query(
        'SELECT pk_test_plan,name,description FROM test_plan ' +
        'WHERE pk_test_plan = ?', req.param('pk_test_plan'),
      function (err, result) {
        var sql_query = squel.select()
          .field('pk_test')
          .field('fk_test_plan')
          .field('name')
          .field('description')
            .field('script')
          .from('test');

        // Expression for search
        var exp = squel.expr();
        if (filter_str) {
          exp.and('fk_test_plan = ?')
            .and('name LIKE ?')
            .and('pk_test > ?');
          sql_query.where(exp, req.param('pk_test_plan'),
            "%" + filter_str.replace(/["']/g, "") + "%",
            after_id);
        } else {
          exp.and('fk_test_plan = ?')
            .and('pk_test > ?');
          sql_query.where(exp, req.param('pk_test_plan'), after_id);
        }

        // Limit
        sql_query.limit(config.page_limit);

        conn.query(sql_query.toString(), function (t_err, t_result) {
            res.format({
              'text/html': function () {
                res.send({test_plan: result[0], tests: t_result});
              },
              'application/json': function () {
                res.send({test_plan: result[0], tests: t_result});
              }
            });
          });
      }
    );
    conn.release();
  });
};

// [POST] new test plan
exports.create = function (req, res) {
  mysql.getConnection(function(err,conn) {
    conn.query('INSERT INTO test_plan SET ?', req.body,
      function (err, result) {
        if (err) throw err;
        res.format({
          'text/html': function () {
            res.send(result);
          },
          'application/json': function () {
            res.send(result);
          }
        });
      }
    );
    conn.release();
  });
};

// [POST] Update test plan by pk_test_plan
exports.update = function (req, res) {
  mysql.getConnection(function(err,conn) {
    conn.query(
        'UPDATE test_plan SET name = ?, description = ? ' +
        'WHERE pk_test_plan = ? ',
      [req.body['name'], req.body['description'], req.body['pk_test_plan']],
      function (err, result) {
        if (err) throw err;
        res.format({
          'text/html': function () {
            res.send(result);
          },
          'application/json': function () {
            res.send(result);
          }
        });
      }
    );
    conn.release();
  });
};

// [DELETE] test plan by pk_test_plan
exports.destroy = function (req, res) {
  mysql.getConnection(function(err,conn) {
    conn.query('DELETE FROM test_plan WHERE pk_test_plan = ?',
      req.param('pk_test_plan'),
      function (err, result) {
        if (err) throw err;
        res.format({
          'text/html': function () {
            res.send(result);
          },
          'application/json': function () {
            res.send(result);
          }
        });
      }
    );
    conn.release();
  });
};

exports.new_report = function (req, res) {
  var filter_str = req.param('filter');
  var results = [];
  var query = squel.select()
    .field('DISTINCT component.pk_component')
    .field('component.name')
    .from('component')
    .where('component.pk_component IN ('+ filter_str.replace(/["']/g, "")+')');

  mysql.getConnection(function(err,conn) {
    var q = conn.query(query.toString());
    /**
     * Get Components
     */
    q.on('result', function (c_res) {
      conn.pause();
      var component = {
        name: c_res.name,
        pk_component: c_res.pk_component,
        test_plans: []
      };
      var tp_q = squel.select()
        .field('DISTINCT test_plan.pk_test_plan')
        .field('test_plan.name')
        .field('test_plan.description')
        .from('test_plan')
        .join('component_to_test_plan',
          'cttp', 'cttp.fk_test_plan = test_plan.pk_test_plan')
        .where('cttp.fk_component = ?',component.pk_component);

      mysql.getConnection(function(err,conn2) {
        var tpq = conn2.query(tp_q.toString());
        /**
         * Get test plans for component
         */
          tpq.on('result', function (tp_res) {
            conn2.pause();
            var test_plan = {
              name: tp_res.name,
              pk_test_plan: tp_res.pk_test_plan,
              tests: []
            };
            var t_q = squel.select()
              .field('DISTINCT test.pk_test')
              .field('test.name')
              .field('test.description')
              .from('test')
              .where('test.fk_test_plan = ?', test_plan.pk_test_plan);

            mysql.getConnection(function(err,conn3) {
              var tq = conn3.query(t_q.toString());
              tq.on('result', function (test) {
                test_plan.tests.push(test);
              }).on('end', function () {
                conn2.resume();
                component.test_plans.push(test_plan);
              });
              conn3.release();
            });
          }).on('end', function () {
            // Add component to results
            results.push(component);
            conn.resume();
          });
        conn2.release();
      });
    }).on('end', function () {
      /**
       * If exporting generate a pdf of the same url via HTML
       */
      if(req.param('export')) {
        var file_id = new Util().guid();
        phantom.create(function(ph){
          ph.createPage(function(page) {
            page.set('paperSize', { format: 'A4', orientation: 'portrait' });
            var url = req.protocol + '://' + req.get('host') +
              '/api/v2/report_test_plans?filter='+filter_str;
            page.open(url, function() {
              page.render(
                path.join(
                  __dirname, '../../public/reports/'+file_id+'.pdf'
                ), function(){
                  console.log('Page Rendered');
                  ph.exit();
                  res.redirect('/reports/'+file_id+'.pdf');
                });
            });
          });
        });
      } else {
        res.format({
          'text/html': function () {
            res.render('reports/test_plan_report', {components: results});
          },
          'application/json': function () {
            res.send(results);
          }
        });
      }
    });
    conn.release();
  });
};

// [GET] Test plan for component by name
exports.name_report = function(req, res) {
  var component_name = req.param('component');

  console.log( component_name );
  var sql_query = squel.select()
    .field('component.pk_component')
    .from('component')
    .where('component.name = \'' + component_name + '\'') ;

  mysql.getConnection(function(err,conn) {
    conn.query(sql_query.toString(),
      function(err, result) {
        if (err) throw err;
        console.log(result);
        res.redirect('/api/v2/report_test_plans?export=true&filter='+result[0].pk_component);
      });
    conn.release();
  });
}

// [GET] List of test plans
exports.report = function (req, res) {
  var filter_str = req.param('filter');
  
  /**
   * Get list of test plans
   */
  var sql_query = squel.select()
    .field('DISTINCT test_plan.pk_test_plan')
    .field('component.pk_component')
    .field('component.name')
    .field('test_plan.name', 'test_plan_name')
    .field('test_plan.description', 'test_plan_description')
    .from('test_plan')
    .join('component_to_test_plan',
      null, 'component_to_test_plan.fk_test_plan = test_plan.pk_test_plan')
    .join('component',
      null,'component.pk_component = component_to_test_plan.fk_component')
    .where('component.pk_component IN ('+ filter_str.replace(/["']/g, "")+')');

  mysql.getConnection(function(err,conn) {
    conn.query(sql_query.toString(),
      function (err, result) {
        if (err) throw err;
        if(req.param('export')) {
          var file_id = new Util().guid();
          phantom.create(function(ph){
            ph.createPage(function(page) {
            page.set('paperSize', { format: 'A4', orientation: 'portrait' });
              var url = req.protocol + '://' + req.get('host') +
                '/api/v1/report_test_plans?filter='+filter_str;
              page.open(url, function() {
                page.render(
                  path.join(
                    __dirname, '../../public/reports/'+file_id+'.pdf'
                  ), function(){
                  console.log('Page Rendered');
                  ph.exit();
                  res.redirect('/reports/'+file_id+'.pdf');
                });
              });
            });
          });
        } else {
          res.format({
            'text/html': function () {
              res.render('reports/test_plan_report', {components: result});
            },
            'application/json': function () {
              res.send(result);
            }
          });
        }
      }
    );
    conn.release();
  });
};
