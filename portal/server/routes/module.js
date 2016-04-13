var mysql = require( '../lib/mysql' );
var config = require( '../../config/config' );
var squel = require( 'squel' );
var Util = require('../lib/util');
var phantom = require('node-phantom-simple');
var path = require('path');
var Join = require('join').Join;

// [GET] List of modules
exports.list = function( req, res ) {
  var filter_str = req.param( 'filter' );

  /**
   * Get List of modules
   */
  var sql_query = squel
          .select()
          .field( 'module.pk_module' )
          .field( 'module.organization' )
          .field( 'module.name' )
          .field( 'module.attributes' )
          .field( 'module.version' )
          .field( 'module.sequence' )
          .field( 'COUNT(DISTINCT fk_test)', 'tests' )
          .field( 'COUNT(DISTINCT fk_test_plan)', 'plans' )
          .from( 'module' )
          .left_join( 'module_to_test_instance', null,
                      'module.pk_module = module_to_test_instance.fk_module' )
          .left_join( 'test_instance', null,
                      'test_instance.pk_test_instance = module_to_test_instance.fk_test_instance' )
          .left_join( 'test', null, 'test.pk_test = test_instance.fk_test' )
          .left_join( 'test_plan', null,
                      'test_plan.pk_test_plan = test.fk_test_plan' );

  // Expression for search
  var exp = squel.expr();
  if ( filter_str ) {
    exp.or( 'module.name LIKE ?' ).or( 'organization LIKE ?' )
            .or( 'attributes LIKE ?' ).or( 'version LIKE ?' )
            .or( 'sequence LIKE ?' );
    var filter_exp = '%' + filter_str.replace( /["']/g, '' ) + '%';
    sql_query.where( exp, filter_exp, filter_exp, filter_exp, filter_exp,
                     filter_exp );
  }

  /**
   * Group by
   */
  sql_query.group( 'module.pk_module' );

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
  case 'plans':
    sql_query.order( 'plans', order );
    break;
  case 'tests':
    sql_query.order( 'tests', order );
    break;
  default:
  case 'name':
    sql_query.order( 'module.organization', order );
    sql_query.order( 'module.name', order );
    sql_query.order( 'module.attributes', order );
    sql_query.order( 'module.version', order );
    sql_query.order( 'module.sequence', order );
    break;
  }

  sql_query.order( 'module.pk_module' );

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

// [GET] Individual module
exports.show = function( req, res ) {
  var after_id = req.param( 'after' ) || 0;
  var filter_str = req.param( 'filter' );

  /**
   * Get module
   */
  var get_comp = squel.select().field( 'pk_module' ).field( 'organization' )
          .field( 'name' ).field( 'version' ).field( 'sequence' )
          .field( 'attributes' ).field( 'scheduled_release' )
          .field( 'actual_release' ).from( 'module' ).where( 'pk_module = ?',
                                                             req.param( 'id' ) );

  mysql.getConnection( function( err, conn ) {
    conn.query( get_comp.toString(), function( err, result ) {
      if ( err )
        throw err;

      conn.release();

      res.format( {
        'text/html': function() {
          res.send( {
            module: result[ 0 ]
          } );
        },
        'application/json': function() {
          res.send( {
            module: result[ 0 ]
          } );
        }
      } );
    } );
  } );
};

exports.report = function( req, res ) {
  mysql.getConnection( function( err, conn ) {
    var module = req.param( 'id' );
    conn.query( 'CALL get_instance_list(NULL,NULL,' + module + ',NULL);',
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

exports.report_print = function( req, res ) {
  var pk = req.param( 'id' );
  var results = [];
  var query = squel.select().field( 'module.pk_module' ).field( 'module.name' )
          .from( 'module' ).where( 'module.pk_module = ' + pk );

  mysql.getConnection( function(err, conn_q){
    var q = conn_q.query( query.toString() );
    var join_tpq = Join.create();

    /**
     * Get Modules
     */
    q.on('result', function( c_res ) {
      var module = {
        name: c_res.name,
        pk_module: c_res.pk_module,
        test_plans: []
      };
      var tp_q = squel.select().field( 'test_plan.pk_test_plan' )
        .field( 'test_plan.name' ).field( 'test_plan.description' )
        .from( 'test_plan' );
      var tpq_end_callback = join_tpq.add();


      mysql.getConnection( function(err, conn_tpq){
        var join_tq = Join.create();

        var tpq = conn_tpq.query( tp_q.toString() );

        /**
        * Get test plans for module
        */
        tpq.on( 'error', function( err ) {
          console.log( 'ERROR: ' + err );
        }).on('result', function( tp_res ) {
          var test_plan = {
            name: tp_res.name,
            pk_test_plan: tp_res.pk_test_plan,
            tests: []
          };
          var t_q = squel.select().field( 'test.pk_test' )
            .field( 'test.name' )
            .field( 'test.description' ).from( 'test' )
            .where( 'test.fk_test_plan = ?',
                    test_plan.pk_test_plan );

          var tq_end_callback = join_tq.add();

          mysql.getConnection( function(err, conn_tq) {

            var tq = conn_tq.query( t_q.toString() );
            tq.on( 'result', function( test ) {
              test_plan.tests.push( test );
            } ).on( 'end', function() {
              conn_tq.release();
              module.test_plans.push( test_plan );
              tq_end_callback();
            } );
          });
        } ).on( 'end', function() {
          results.push( module );
          conn_tpq.release();
          join_tq.then(function(){
            tpq_end_callback();
          });
        });
      });
    }).on('end', function() {
      conn_q.release();
      join_tpq.then(function () {
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
              console.log( JSON.stringify( results ) );
              res.render( 'reports/module_tests', {
                modules: results
              } );
            },
            'application/json': function() {
              res.send( results );
            }
          } );
        }
      });
    });
  });
};
