/// Mysql Connection
var mysql    = require('../lib/mysql');
var config  = require('../../config/config');
var squel   = require('squel');
var Util    = require('../lib/util');
var path    = require('path');
var phantom = require('node-phantom');
var async   = require('async');
var _        = require('underscore');
var d3      = require('d3');
var instances = require('../models/instances');

exports.view = function( req, res ) {
  var instance_id = req.param('id');
  var sql_query = squel
    .select()
    .field( 'test_instance.description' )
    .field( 'test_instance.due_date' )
    .field( 'test_instance.fk_described_template' )
    .field( 'run.start_time' )
    .field( 'run.ready_time' )
    .field( 'run.end_time' )
    .field( 'run.result' )
    .field( 'template.steps' )
    .from( 'test_instance' )
    .left_join( 'run', null, 'run.pk_run = test_instance.fk_run' )
    .left_join( 'described_template', null, 'described_template.pk_described_template = test_instance.fk_described_template' )
    .left_join( 'template', null, 'template.pk_template = described_template.fk_template' )
    .where( 'pk_test_instance = ?', instance_id );

  var query_string = sql_query.toString();

  mysql.getConnection( function( err, conn ) {
    conn.query( query_string, function( err, result ) {
      conn.release();

      if ( err )
        throw err;

      res.format( {
        'text/html': function() {
          res.send( result[0] );
        },
        'application/json': function() {
          res.send( result[0] );
        }
      } );
    } );
  } );
};

exports.lines = function( req, res ) {
  var instance_id = req.param('id');
  var sql_query = squel
    .select()
    .field( 'pk_dt_line' )
    .field( 'fk_child_dt' )
    .field( 'GROUP_CONCAT(fk_artifact)', 'artifacts' )
    .field( 'description' )
    .from( 'dt_line' )
    .left_join( 'artifact_to_dt_line', null, 'artifact_to_dt_line.fk_dt_line = dt_line.pk_dt_line' )
    .where( 'fk_described_template = ?', instance_id )
    .group( 'line' )
    .order( 'line', true );

  var query_string = sql_query.toString();

  mysql.getConnection( function( err, conn ) {
    conn.query( query_string, function( err, result ) {
      conn.release();

      if ( err )
        throw err;

      res.format( {
        'text/html': function() {
          res.send( result );
        },
        'application/json': function() {
          res.send( result );
        }
      } );
    } );
  } );
};

exports.user_tests = function (req, res) {
  var filter_str = req.param( 'user' );

  /**
   * Get List of modules
   */
  var sql_query = squel
          .select()
          .field( 'test_instance.pk_test_instance' )
          .field( 'run.pk_run' )
          .from( 'test_instance' )
          .left_join( 'run', null,
                      'test_instance.fk_run = run.pk_run' );

  // Expression for search
  var exp = squel.expr();
  if ( filter_str ) {
    sql_query.where( 'owner = ?', filter_str );
  }
  else {
    sql_query.where( 'owner IS NOT NULL' );
  }

  sql_query.where( 'end_time IS NULL' );

  var query_string = sql_query.toString();

  mysql.getConnection( function( err, conn ) {
    conn.query( query_string, function( err, result ) {
      conn.release();

      if ( err )
        throw err;
      res.format( {
        'text/html': function() {
          res.send( result );
        },
        'application/json': function() {
          res.send( result );
        }
      } );
    } );
  } );
};

// [GET] List of instances
exports.list = function (req, res) {
  var plan_str = req.param('plan') || null;
  var test_str = req.param('test') || null;
  var module_str = req.param('module') || null;
  var against_str = req.param('against') || '';

  instances.list_instances( plan_str, test_str, module_str, against_str, function(err, result){
    res.format({
      'text/html': function () {
        res.send(JSON.stringify(result));
      },
      'application/json': function () {
        res.send(result);
      }
    });
  });

  return;

  var after_id = 0;
  var filter_str = req.param('filter');
  if (req.param('after')) {
    after_id = req.param('after');
  }

  // Use squel to generate sql
  var sql_query =
    squel.select()
    .field('pk_module')
    .field('version')
    .field('scheduled_release')
    .field('actual_release')
    .from('module');

  // Expression for search
  var exp = squel.expr();
  if (filter_str) {
    exp.and('pk_module > ?')
      .and("version LIKE ?");
    sql_query.where(exp, after_id, "%" + filter_str.replace(/["']/g, "") + "%");
  } else {
    exp.and('pk_module > ?');
    sql_query.where(exp, after_id);
  }

  // Group by and page limit
  sql_query.group('pk_module');
    //.limit(config.page_limit);

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

function findPK(array,id) {
    for(var i = 0; i < array.length; i++) {
        if (array[i].pk == id) {
            return array[i];
        }
    }
}

function testPass(o) {
    o.summary.total += 1;
    o.summary.pass += 1;
}

function testFail(o) {
    o.summary.total += 1;
    o.summary.fail += 1;
}

function testPend(o) {
    o.summary.total += 1;
    o.summary.pending += 1;
}

/**
 * [Get] Report for module and (optional) version by name.
 */
exports.name_report = function (req, res) {
  var module_name = req.param('module');
  var version_name = req.param('version');

  var sql_query = squel.select()
    .field('version.pk_version')
    .field('version.version')
    .from('version')
    .join('module', '', 'module.pk_module = version.fk_module')
    .where('module.name = \'' + module_name + '\'');

  if ( version_name )
    sql_query = sql_query.where('version.version = \'' + version_name + '\'');

  mysql.getConnection(function(err,conn) {
    conn.query(sql_query.toString(),
      function(err, result) {
        if (err) throw err;

        var matches = [];
        result.forEach( function(match) {
          matches.push( match.pk_version );
        });

        if ( matches.length == 0 )
          res.redirect('/');
        else
          res.redirect('/api/v1/report_versions?export=true&select='+matches.join(',')+'&filter=');
      });
    conn.release();
  });
}

// Break the descriptions array into nested groups
function process_descriptions( descriptions ) {
  var nested = [];

  descriptions.forEach( function (d) {
    var D = findPK( nested, d.fk_described_template ) || {
      pk: d.fk_described_template,
      lines: []
    };
    if ( nested.indexOf(D) == -1 ) {
      nested.push(D);
    }

    D.lines.push( d );
  })

  return nested;
}

// Summary contains test plan summary information for each version.
// Results contains test results for each version.
// Descriptions contains test descriptions for each test.
function process_report( incl_passed, incl_failed, incl_pending, summaries, results ) {
  var nested = [];

  summaries.forEach( function (ti) {
    var C = findPK( nested, ti.pk_module ) || {
      pk: ti.pk_module,
      name: ti.module_name,
      versions: []
    };
    if ( nested.indexOf(C) == -1 ) {
        nested.push(C);
    }

    var V = findPK(C.versions, ti.pk_version ) || {
      pk: ti.pk_version,
      name: ti.version,
      summary: { total: 0, pass: 0, fail: 0, pending: 0 },
      test_plans: []
    };
    if (C.versions.indexOf(V) == -1 )
        C.versions.push(V);

    var TP = findPK(V.test_plans, ti.pk_test_plan ) || {
      pk: ti.pk_test_plan,
      name: ti.test_plan_name,
      summary: { total: ti.total, pass: ti.passed, fail: ti.failed, pending: ti.pending },
      tests: []
    };
    if (V.test_plans.indexOf(TP) == -1 )
        V.test_plans.push(TP);

    V.summary.total += ti.total;
    V.summary.pass += ti.passed;
    V.summary.fail += ti.failed;
    V.summary.pending += ti.pending;
  });

  results.forEach(function (ti) {
    var C = findPK( nested, ti.pk_module ) || {
        pk: ti.pk_module,
        name: ti.module_name,
        versions: []
    };
    if ( nested.indexOf(C) == -1 ) {
        nested.push(C);
    }

    var V = findPK(C.versions, ti.pk_version ) || {
        pk: ti.pk_version,
        name: ti.version,
        summary: { total: 0, pass: 0, fail: 0, pending: 0 },
        test_plans: []
    };
    if (C.versions.indexOf(V) == -1 )
        C.versions.push(V);

    var TP = findPK(V.test_plans, ti.pk_test_plan ) || {
        pk: ti.pk_test_plan,
        name: ti.test_plan_name,
        summary: { total: 0, pass: 0, fail: 0, pending: 0 },
        tests: []
    };
    if (V.test_plans.indexOf(TP) == -1 )
        V.test_plans.push(TP)

    var T = findPK(TP.tests, ti.pk_test ) || {
        pk: ti.pk_test,
        name: ti.test_name,
        results: []
    };

    if ( ti.passed == null ) {
        if ( incl_pending == 'true' ) {
            if (TP.tests.indexOf(T) == -1 )
              TP.tests.push(T);
            T.results.push(ti);
        }
    }
    else if ( ti.passed ) {
        if ( incl_passed == 'true' ) {
          if (TP.tests.indexOf(T) == -1 )
            TP.tests.push(T);
          T.results.push(ti);
        }
    }
    else {
        if ( incl_failed == 'true' ) {
          if (TP.tests.indexOf(T) == -1 )
            TP.tests.push(T);
          T.results.push(ti);
        }
    }
  });

  return nested;
}

/**
 * [GET] Report of test instances and results by versions
 * @param req
 * @param res
 */
exports.report = function (req, res) {
  var select_str = req.param('select');
  var filter_str = req.param('filter');
  var incl_passed = req.param('incl_passed');
  var incl_failed = req.param('incl_failed');
  var incl_pending = req.param('incl_pending');

  if ( req.param('export') ) {
    var file_id = new Util().guid();
    phantom.create(
      function( err,ph ) {
      ph.createPage(function(err,page) {
        page.set('paperSize', { format: 'A4', orientation: 'portrait', border: '20px' });
        var url = req.protocol + '://' + req.get('host') +
          '/api/v1/report_versions?select='+select_str+'&filter='+filter_str+'&incl_failed='+incl_failed+'&incl_passed='+incl_passed+'&incl_pending='+incl_pending;

        page.open(url, function(err,status) {
          page.evaluate(
            function() { return build_graphs(); },
            function(err,initial_count) {
              var remaining = initial_count;
              async.whilst(
                function() {
                  return remaining > 0;
                },
                function(callback) {
                  page.evaluate(
                    function() {
                      return pending_graphs();
                    },
                    function(err,result) {
                      console.log("Remaining: " + err + "/" + result);
                      remaining = result;
                      callback(err);
                    }
                  );
                },
                function(err) {
                  page.render(
                    path.join( __dirname, '../../public/reports/'+file_id+'.pdf' ),
                    function() {
                      ph.exit();
                      res.redirect('/reports/'+file_id+'.pdf');
                    }
                  );
                }
              );
            }
          );
        });
      });
    }, {parameters:{'ignore-ssl-errors':'yes'}});
    return;
  }

  /**
   * Get list of test result data by versions
   */
  var summary_query = "call get_summary_by_version('" + select_str.replace(/["']/g, "") + "');";
  var results_query = "call get_detail_by_version('" + select_str.replace(/["']/g, "") + "',"+incl_passed+","+incl_failed+","+incl_pending+");";
  var descriptions_query = "call get_descriptions_by_version('" + select_str.replace(/["']/g, "") + "',"+incl_passed+","+incl_failed+","+incl_pending+");";
  var resource_query = "call get_resources_by_version('" + select_str.replace(/["']/g, "") + "',"+incl_passed+","+incl_failed+","+incl_pending+");";

  mysql.getConnection(function(err,conn) {
    conn.query(summary_query,
      function (err, summary) {
        if (err) throw err;

          conn.query(results_query,
            function(err, results) {
              if (err) throw err;

              conn.query(descriptions_query,
                function (err, descriptions) {
                  if (err) throw err;

                  conn.query(resource_query,
                      function (err, resources) {
                    var nested_details = process_report(incl_passed, incl_failed, incl_pending, summary[0], results[0] );
                    var nested_descriptions = process_descriptions( descriptions[0] );

                    res.format({
                      'text/html': function () {
                        res.render('reports/version_report', {items: nested_details});
                      },
                      'application/json': function () {
                        res.send( { details: nested_details, descriptions: nested_descriptions, resources: resources[0] } );
                      }
                    });
                  });
              });
            });
        }
    );
    conn.release();
  });
};

/**
 * [GET] Get a list of descriptions associated with a set of test instances
 * @param req
 * @param res
 */
exports.descriptions = function (req, res) {
    var select_str = req.param('select') || "";
    var filter_str = req.param('filter') || "";

    /**
     * Get list of test result data by module version
     */
    // Determine the selection, can be empty
    var where_clause = "";
    var having_clause = "";
    var spacer = " WHERE ";
    if ( select_str.length > 0 ) {
        where_clause = " WHERE version.pk_version IN (" + select_str.replace(/["']/g, "") + ')';
        spacer = " AND ";
    }
    var sql_query = "SELECT * FROM module;";

    mysql.getConnection(function(err,conn) {
      conn.query(sql_query,
          function (err, result) {
              if (err) throw err;
              res.format({
                  'text/html': function () {
                      res.render('reports/version_report', {items: result});
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
