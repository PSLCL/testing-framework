var mysql = require( '../lib/mysql' );
var config = require( '../../config/config' );
var squel = require( 'squel' );
var Util = require( '../lib/util' );
var path = require( 'path' );
var phantom = require( 'phantom' );

// [GET] List of test plans
exports.list = function( req, res ) {
  var filter_str = req.param( 'filter' );

  /**
   * Get list of test plans
   */
  var sql_query = squel.select().field( 'test_plan.pk_test_plan' )
          .field( 'test_plan.name' ).field( 'test_plan.description' )
          .field( 'COUNT(DISTINCT test.pk_test)', 'tests' ).from( 'test_plan' )
          .left_join( 'test', null,
                      'test.fk_test_plan = test_plan.pk_test_plan' );

  /**
   * Test Plan search filtering
   */
  var exp = squel.expr();
  if ( filter_str ) {
    exp.or( 'test_plan.name LIKE ?' ).or( 'test_plan.description LIKE ?' );
    var filter_exp = '%' + filter_str.replace( /["']/g, '' ) + '%';
    sql_query.where( exp, filter_exp, filter_exp );
  }

  /**
   * Group by
   */
  sql_query.group( 'test_plan.pk_test_plan' );

  /**
   * Sort by the order parameter. The sort order must be stable for the limit
   * and offset options to be consistent.
   */
  var order = true;
  var field = 'name';
  if ( req.param( 'order' ) ) {
    field = '' + req.param( 'order' );
    if ( field.indexOf( '<' ) == 0 )
      field = field.substring( 1 );
    if ( field.indexOf( '>' ) == 0 ) {
      order = false;
      field = field.substring( 1 );
    }
  }

  switch ( field ) {
  case 'tests':
    sql_query.order( 'tests', order );
    break;
  case 'description':
    sql_query.order( 'description', order );
    break;
  default:
  case 'name':
    sql_query.order( 'name', order );
    break;
  }

  sql_query.order( 'pk_test_plan' );

  /**
   * Limit of records by global page_limit All parameter being true means no
   * limit
   */
  if ( req.param( 'limit' ) ) {
    if ( req.param( 'limit' ) == 'all' ) {
      if ( req.param( 'offset' ) )
        sql_query.limit( 100000 );
    }
    else
      sql_query.limit( 0 + req.param( 'limit' ) );
  }
  else {
    sql_query.limit( 0 + config.page_limit );
  }

  if ( req.param( 'offset' ) )
    sql_query.offset( 0 + req.param( 'offset' ) );

  var query_string = sql_query.toString();

  mysql.getConnection( function( err, conn ) {
    conn.query( query_string, function( err, result ) {
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
    conn.release();
  } );
};

// [GET] individual test plan
exports.show = function( req, res ) {
  var after_id = 0;
  var filter_str = req.param( 'filter' );
  if ( req.param( 'after' ) ) {
    after_id = req.param( 'after' );
  }

  mysql.getConnection( function( err, conn ) {
    conn
            .query( 'SELECT pk_test_plan,name,description FROM test_plan '
                    + 'WHERE pk_test_plan = ?', req.param( 'pk_test_plan' ),
                    function( err, result ) {
                      var sql_query = squel.select().field( 'pk_test' )
                              .field( 'fk_test_plan' ).field( 'name' )
                              .field( 'description' ).field( 'script' )
                              .from( 'test' );

                      // Expression for search
                      var exp = squel.expr();
                      if ( filter_str ) {
                        exp.and( 'fk_test_plan = ?' ).and( 'name LIKE ?' )
                                .and( 'pk_test > ?' );
                        sql_query.where( exp, req.param( 'pk_test_plan' ),
                                         "%" + filter_str.replace( /["']/g, "" )
                                                 + "%", after_id );
                      }
                      else {
                        exp.and( 'fk_test_plan = ?' ).and( 'pk_test > ?' );
                        sql_query.where( exp, req.param( 'pk_test_plan' ),
                                         after_id );
                      }

                      // Limit
                      sql_query.limit( config.page_limit );

                      conn.query( sql_query.toString(), function( t_err,
                                                                  t_result ) {
                        res.format( {
                          'text/html': function() {
                            res.send( {
                              test_plan: result[ 0 ],
                              tests: t_result
                            } );
                          },
                          'application/json': function() {
                            res.send( {
                              test_plan: result[ 0 ],
                              tests: t_result
                            } );
                          }
                        } );
                      } );
                    } );
    conn.release();
  } );
};

// [POST] new test plan
exports.create = function( req, res ) {
  mysql.getConnection( function( err, conn ) {
    conn.query( 'INSERT INTO test_plan SET ?', req.body,
                function( err, result ) {
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
    conn.release();
  } );
};

// [POST] Update test plan by pk_test_plan
exports.update = function( req, res ) {
  mysql.getConnection( function( err, conn ) {
    var name = req.body[ 'name' ] || "";
    var description = req.body[ 'description' ] || "";

    conn.query( 'UPDATE test_plan SET name = ?, description = ? '
                + 'WHERE pk_test_plan = ? ', [ name, description,
                                              req.body[ 'pk_test_plan' ]
    ], function( err, result ) {
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
    conn.release();
  } );
};

// [DELETE] test plan by pk_test_plan
exports.destroy = function( req, res ) {
  mysql.getConnection( function( err, conn ) {
    conn.query( 'DELETE FROM test_plan WHERE pk_test_plan = ?', req
            .param( 'pk_test_plan' ), function( err, result ) {
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
    conn.release();
  } );
};

exports.new_report = function( req, res ) {
  var filter_str = req.param( 'filter' );
  var results = [];
  var query = squel.select().field( 'DISTINCT module.pk_module' )
          .field( 'module.name' ).from( 'module' )
          .where(
                  'module.pk_module IN (' + filter_str.replace( /["']/g, "" )
                          + ')' );

  mysql.getConnection( function( err, conn ) {
    var q = conn.query( query.toString() );
    /**
     * Get Modules
     */
     //SELECT DISTINCT tp.pk_test_plan, tp.name, tp.description FROM test_plan `tp` INNER JOIN test ON (tp.pk_test_plan = test.fk_test_plan) INNER JOIN test_instance `ti` ON (test.pk_test = ti.fk_test) INNER JOIN module_to_test_instance `mtti` ON (ti.pk_test_instance = mtti.fk_test_instance) WHERE (mtti.fk_module IN (32));

    q.on(
          'result',
          function( c_res ) {
            conn.pause();
            var module = {
              name: c_res.name,
              pk_module: c_res.pk_module,
              test_plans: []
            };
            var tp_q = squel.select().field( 'DISTINCT test_plan.pk_test_plan' )
                    .field( 'test_plan.name' ).field( 'test_plan.description' )
                    .from( 'test_plan' )
                    .join( 'module_to_test_plan', 'cttp',
                           'cttp.fk_test_plan = test_plan.pk_test_plan' )
                    .where( 'cttp.fk_module = ?', module.pk_module );

            mysql.getConnection( function( err, conn2 ) {
              var tpq = conn2.query( tp_q.toString() );
              /**
               * Get test plans for module
               */
              tpq.on(
                      'result',
                      function( tp_res ) {
                        conn2.pause();
                        var test_plan = {
                          name: tp_res.name,
                          pk_test_plan: tp_res.pk_test_plan,
                          tests: []
                        };
                        var t_q = squel.select()
                                .field( 'DISTINCT test.pk_test' )
                                .field( 'test.name' )
                                .field( 'test.description' ).from( 'test' )
                                .where( 'test.fk_test_plan = ?',
                                        test_plan.pk_test_plan );

                        mysql.getConnection( function( err, conn3 ) {
                          var tq = conn3.query( t_q.toString() );
                          tq.on( 'result', function( test ) {
                            test_plan.tests.push( test );
                          } ).on( 'end', function() {
                            conn2.resume();
                            module.test_plans.push( test_plan );
                          } );
                          conn3.release();
                        } );
                      } ).on( 'end', function() {
                // Add module to results
                results.push( module );
                conn.resume();
              } );
              conn2.release();
            } );
          } ).on(
                  'end',
                  function() {
                    /**
                     * If exporting generate a pdf of the same url via HTML
                     */
                    if ( req.param( 'export' ) ) {
                      var file_id = new Util().guid();
                      phantom.create( function( ph ) {
                        ph.createPage( function( page ) {
                          page.set( 'paperSize', {
                            format: 'A4',
                            orientation: 'portrait'
                          } );
                          var url = req.protocol + '://' + req.get( 'host' )
                                    + '/api/v2/report_test_plans?filter='
                                    + filter_str;
                          page.open( url, function() {
                            page
                                    .render( path.join( __dirname,
                                                        '../../public/reports/'
                                                                + file_id
                                                                + '.pdf' ),
                                             function() {
                                               ph.exit();
                                               res
                                                       .redirect( '/reports/'
                                                                  + file_id
                                                                  + '.pdf' );
                                             } );
                          } );
                        } );
                      } );
                    }
                    else {
                      res.format( {
                        'text/html': function() {
                          res.render( 'reports/test_plan_report', {
                            modules: results
                          } );
                        },
                        'application/json': function() {
                          res.send( results );
                        }
                      } );
                    }
                  } );
    conn.release();
  } );
};

// [GET] Test plan for module by name
exports.name_report = function( req, res ) {
  var module_name = req.param( 'module' );

  var sql_query = squel.select().field( 'module.pk_module' ).from( 'module' )
          .where( 'module.name = \'' + module_name + '\'' );

  mysql.getConnection( function( err, conn ) {
    conn.query( sql_query.toString(), function( err, result ) {
      if ( err )
        throw err;
      res.redirect( '/api/v2/report_test_plans?export=true&filter='
                    + result[ 0 ].pk_module );
    } );
    conn.release();
  } );
}

// [GET] List of test plans
exports.report = function( req, res ) {
  var filter_str = req.param( 'filter' );

  /**
   * Get list of test plans
   */
  var sql_query = squel.select().field( 'DISTINCT test_plan.pk_test_plan' )
          .field( 'module.pk_module' ).field( 'module.name' )
          .field( 'test_plan.name', 'test_plan_name' )
          .field( 'test_plan.description', 'test_plan_description' )
          .from( 'test_plan' )
          .join( 'module_to_test_plan', null,
                 'module_to_test_plan.fk_test_plan = test_plan.pk_test_plan' )
          .join( 'module', null,
                 'module.pk_module = module_to_test_plan.fk_module' )
          .where(
                  'module.pk_module IN (' + filter_str.replace( /["']/g, "" )
                          + ')' );

  mysql.getConnection( function( err, conn ) {
    conn.query( sql_query.toString(), function( err, result ) {
      if ( err )
        throw err;
      if ( req.param( 'export' ) ) {
        var file_id = new Util().guid();
        phantom.create( function( ph ) {
          ph.createPage( function( page ) {
            page.set( 'paperSize', {
              format: 'A4',
              orientation: 'portrait'
            } );
            var url = req.protocol + '://' + req.get( 'host' )
                      + '/api/v1/report_test_plans?filter=' + filter_str;
            page.open( url, function() {
              page.render( path.join( __dirname, '../../public/reports/'
                                                 + file_id + '.pdf' ),
                           function() {

                             ph.exit();
                             res.redirect( '/reports/' + file_id + '.pdf' );
                           } );
            } );
          } );
        } );
      }
      else {
        res.format( {
          'text/html': function() {
            res.render( 'reports/test_plan_report', {
              modules: result
            } );
          },
          'application/json': function() {
            res.send( result );
          }
        } );
      }
    } );
    conn.release();
  } );
};
