// Mysql Connection
var mysql    = require('../lib/mysql');
var config  = require('../../config/config');
var squel   = require('squel');
var Util    = require('../lib/util');
var path    = require('path');
var phantom = require('node-phantom');
var async   = require('async');
var _        = require('underscore');
var d3      = require('d3');

function buildModules( m ) {
    m.sort(function(a,b) { return a.offset - b.offset; });
    return _.map( m, function(mod) {
        var s = mod.organization + '#' + mod.name + ';' + mod.version + '(' + mod.sequence + ')';
        if ( mod.attributes )
            s += '[' + mod.attributes + ']';
        
        return s;
    });
};

function expandModules( m, ls ) {
    var la = ls.split(',');
    return _.map( la, function(i) {
        return m[i];
    });
};

// [GET] List of instances
exports.list = function (req, res) {
    var plan_str = req.param('plan') || null;
    var test_str = req.param('test') || null;
    var module_str = req.param('module') || null;
    var against_str = req.param('against') || '';
    
    mysql.getConnection(function(err,conn) {
        var statement = 'call get_instance_list('+ plan_str + ',' + test_str + ',' + module_str + ',"' + against_str + '");';
        console.log( statement );
        conn.query(statement,
          function (err, result) {
            var R = { instances: [],
                    descriptions: []
            }
            if ( err == null && result != null && result.length > 1 ) {
                // Build a list of module names
                var modules = buildModules( result[3] );
                
                // The list is returned in sorted order by test plan name and test name.
                R.instances = d3.nest()
                    .key( function(d) { return d.pk_test_plan } )
                    .key( function(d) { return d.pk_test } )
                    .entries(result[0]);
                R.instances = _.map( R.instances, function( test_plan ) {
                    var docs = _.find( result[1], function(e) { return e.pk_test_plan == test_plan.key; } );
                    var vals = _.map( test_plan.values, function( test ) {
                        var docs = _.find( result[2], function(e) { return e.pk_test == test.key; } );
                        var summary = _.countBy(test.values, function(i) {
                            if ( i.result == null ) return 'pending';
                            return i.result == 0 ? 'failed' : 'passed';
                        });
                        
                        if ( summary.passed == undefined )
                            summary.passed = 0;
                        if ( summary.failed == undefined )
                            summary.failed = 0;
                        if ( summary.pending == undefined )
                            summary.pending = 0;
                        summary.total = summary.passed + summary.failed + summary.pending;
                        
                        var data = [
                                    {
                                      value: summary.passed,
                                      color:"#00CC00"
                                    },
                                    {
                                      value : summary.failed,
                                      color : "#FF0000"
                                    },
                                    {
                                      value : summary.pending,
                                      color : "#A0A0A0"
                                    }
                                    ];
                        var chart = { "data": data, "options": { "animation": false, "animateRotate": false, "segmentShowStroke": false } };
                        
                        var items = _.map( test.values, function(v) {
                            v.module_list = expandModules( modules, v.modules );
                            if ( v.result == null )
                                v.result_text = "Pending";
                            else if ( v.result == 0 )
                                v.result_text = "Failed";
                            else
                                v.result_text = "Passed";
                            
                            return v;
                        });
                        
                        return {
                            key: test.key,
                            name: docs.name,
                            description: docs.description,
                            'summary': summary,
                            'chart': chart,
                            values: items
                        };
                    });
                    
                    var summary = { passed: 0, failed: 0, pending: 0, total: 0 };
                    _.each( vals, function(e) {
                        this.passed += e.summary.passed;
                        this.failed += e.summary.failed;
                        this.pending += e.summary.pending;
                        this.total += e.summary.total;
                    }, summary );

                    var data = [
                                {
                                  value: summary.passed,
                                  color:"#00CC00"
                                },
                                {
                                  value : summary.failed,
                                  color : "#FF0000"
                                },
                                {
                                  value : summary.pending,
                                  color : "#A0A0A0"
                                }
                                ];
                    var chart = { "data": data, "options": { "animation": false, "animateRotate": false, "segmentShowStroke": false } };
                    
                    return {
                        key: test_plan.key,
                        name: docs.name,
                        description: docs.description,
                        'summary': summary,
                        'chart': chart,
                        values: vals
                    };
                });

                var summary = { passed: 0, failed: 0, pending: 0, total: 0 };
                _.each( R.instances, function(e) {
                    this.passed += e.summary.passed;
                    this.failed += e.summary.failed;
                    this.pending += e.summary.pending;
                    this.total += e.summary.total;
                }, summary );
                
                var data = [
                            {
                              value: summary.passed,
                              color:"#00CC00"
                            },
                            {
                              value : summary.failed,
                              color : "#FF0000"
                            },
                            {
                              value : summary.pending,
                              color : "#A0A0A0"
                            }
                            ];
                var chart = { "data": data, "options": { "animation": false, "animateRotate": false, "segmentShowStroke": false } };
                
                R.summary = summary;
                R.chart = chart;
                
                R.descriptions = result[1];
            }

            res.format({
                'text/html': function () {
                  res.send(JSON.stringify(R));
                },
                'application/json': function () {
                  res.send(R);
                }
              });
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