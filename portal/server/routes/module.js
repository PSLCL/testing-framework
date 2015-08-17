// Mysql Connection
var mysql   = require('../lib/mysql');
var config = require('../../config/config');
var squel  = require('squel');

// [GET] List of modules
exports.list = function (req, res) {
  var after_id = 0;
  var filter_str = req.param('filter');
  if (req.param('after')) {
    after_id = req.param('after');
  }

  /**
   * Get List of modules
   */
  var sql_query =
    squel.select()
    .field('module.pk_module')
    .field('module.organization')
    .field('module.name')
    .field('module.attributes')
    .field('module.version')
    .field('module.sequence')
    .from('module');

  // Expression for search
  var exp = squel.expr();
  if (filter_str) {
    exp.and('module.pk_module > ?')
      .or_begin("name LIKE ?")
      .or("organization LIKE ?")
      .or("attributes LIKE ?")
      .or("version LIKE ?")
      .or("sequence LIKE ?")
      .end();
    var filter_exp = "%" + filter_str.replace(/["']/g, "") + "%";
    sql_query.where(exp, after_id, filter_exp, filter_exp, filter_exp, filter_exp, filter_exp);
  } else {
    exp.and('module.pk_module > ?');
    sql_query.where(exp, after_id);
  }

  /**
   * Group by
   */
  sql_query.group('module.pk_module');

  /**
   * Order by sort parameter
   */
  switch(req.param('sort_by')) {
    case 'test_plans':
      sql_query.order('test_plans',false);
      break;
    case 'tests':
      sql_query.order('tests',false);
      break;
    case 'name':
      sql_query.order('module.organization');
      sql_query.order('module.name');
      sql_query.order('module.attributes');
      sql_query.order('module.version');
      sql_query.order('module.sequence');
      break;
    case 'associated':
    case 'id':
    default:
      sql_query.order('module.pk_module');
  }

  /**
   * Limit of records by global page_limit
   * All parameter being true means no limit
   */
  if (!req.param('all') || req.param('all') != 'true') {
    sql_query.limit(config.page_limit);
  }

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

// [GET] Individual module
exports.show = function (req, res) {
  var after_id = req.param('after') || 0;
  var filter_str = req.param('filter');

  /**
   * Get module
   */
  var get_comp = squel.select()
    .field('pk_module')
    .field('organization')
    .field('name')
    .field('version')
    .field('sequence')
    .field('attributes')
    .field('scheduled_release')
    .field('actual_release')
    .from('module')
    .where('pk_module = ?', req.param('id'));

  mysql.getConnection(function(err,conn) {
    conn.query(
        get_comp.toString(),
      function (err, result) {
        if (err) throw err;

        var sql_query = squel.select()
          .field('test_plan.name')
          .field('test_plan.pk_test_plan')
          .field('test_plan.description')
          .field('module_to_test_plan.fk_module')
          .field('COUNT(DISTINCT test.pk_test)', 'tests')
          .from('test_plan')
          .left_join('module_to_test_plan', null,
            'module_to_test_plan.fk_test_plan = test_plan.pk_test_plan ' +
            'AND module_to_test_plan.fk_module = '+conn.escape(req.param('id')))
          .left_join('test', null, 'test.fk_test_plan = test_plan.pk_test_plan');

        /**
         * Expression for like search
         * by filter_str
         */
        var exp = squel.expr();
        if (filter_str) {
          exp.and('test_plan.pk_test_plan > ?')
            .and('test_plan.name LIKE ?');
          sql_query.where(exp,
            after_id, "%" + filter_str.replace(/["']/g, "") + "%");
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
            sql_query.order('fk_module',false);
            break;
          case 'id':
          default:
            sql_query.order('test_plan.pk_test_plan');
        }

        /**
         * Limit of records by global page_limit
         */
        sql_query.limit(config.page_limit);

        /**
         * Get test plans
         */
        conn.query(sql_query.toString(),
          function (tp_err, tp_result) {
            res.format({
              'text/html': function () {
                res.send({ module: result[0], test_plans: tp_result });
              },
              'application/json': function () {
                res.send({ module: result[0], test_plans: tp_result });
              }
            });
          });
      }
    );
    conn.release();
  });
};

exports.report = function( req, res ) {
    mysql.getConnection(function(err,conn) {
        conn.query('SELECT name FROM test_plan',
        //    'WHERE fk_module = ? AND fk_test_plan = ?',
        //  [req.param('fk_module'), req.param('fk_test_plan')],
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

exports.report_print = function( req, res ) {
    var pk = req.param('id');
    var results = [];
    var query = squel.select()
      .field('module.pk_module')
      .field('module.name')
      .from('module')
      .where('module.pk_module = ' + pk );

    mysql.getConnection(function(err,conn) {
      if ( err ) {
          console.log( 'ERROR: ' + err );
          throw err;
      }
      
      var q = conn.query(query.toString());
      /**
       * Get Modules
       */
      q.on('result', function (c_res) {
        var module = {
          name: c_res.name,
          pk_module: c_res.pk_module,
          test_plans: []
        };
        var tp_q = squel.select()
          .field('test_plan.pk_test_plan')
          .field('test_plan.name')
          .field('test_plan.description')
          .from('test_plan');

        mysql.getConnection(function(err,conn2) {
          if ( err ) {
              console.log( 'ERROR: ' + err );
              throw err;
          }
          
          var tpq = conn2.query(tp_q.toString());
          /**
           * Get test plans for module
           */
            tpq
            .on('error', function(err) {
                console.log( 'ERROR: ' + err );
            })
            .on('result', function (tp_res) {
              var test_plan = {
                name: tp_res.name,
                pk_test_plan: tp_res.pk_test_plan,
                tests: []
              };
              var t_q = squel.select()
                .field('test.pk_test')
                .field('test.name')
                .field('test.description')
                .from('test')
                .where('test.fk_test_plan = ?', test_plan.pk_test_plan);

              mysql.getConnection(function(err,conn3) {
                var tq = conn3.query(t_q.toString());
                tq.on('result', function (test) {
                  test_plan.tests.push(test);
                }).on('end', function () {
                  module.test_plans.push(test_plan);
                });
                conn3.release();
              });
            })
            .on('end', function () {
              // Add module to results
              results.push(module);
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
                    ph.exit();
                    res.redirect('/reports/'+file_id+'.pdf');
                  });
              });
            });
          });
        } else {
          res.format({
            'text/html': function () {
              console.log(JSON.stringify( results ) );
              res.render('reports/module_tests', {modules: results});
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

// [DELETE] test plan from module
exports.remove_test_plan = function (req, res) {
  mysql.getConnection(function(err,conn) {
    conn.query('DELETE FROM module_to_test_plan ' +
        'WHERE fk_module = ? AND fk_test_plan = ?',
      [req.param('fk_module'), req.param('fk_test_plan')],
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

// [POST] test plan to module
exports.add_test_plan = function (req, res) {
  mysql.getConnection(function(err,conn) {
    conn.query('INSERT INTO module_to_test_plan SET ?', req.body,
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

// [POST] new module
exports.create = function (req, res) {
  mysql.getConnection(function(err,conn) {
    conn.query('INSERT INTO module SET ?', req.body,
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

// [POST] Update module by pk_module
exports.update = function (req, res) {
  mysql.getConnection(function(err,conn) {
    conn.query(
        'UPDATE module SET name = ? ' +
        'WHERE pk_module = ? ',
      [req.body['name'], req.body['pk_module']],
      function (err, result) {
        if (err) throw err;
        res.format({
          'text/html': function () {
            res.send(result)
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

// [DELETE] module by pk_module
exports.destroy = function (req, res) {
  mysql.getConnection(function(err,conn) {
    conn.query('DELETE FROM module WHERE pk_module = ?', req.param('id'),
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
