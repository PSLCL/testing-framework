// Mysql Connection
var mysql   = require('../lib/mysql');
var config = require('../../config/config');
var squel = require( 'squel' );
var tar = require('tar-stream');
var zlib = require('zlib');
var fs = require('fs');

exports.list = function (req, res ) {
  var module = req.param( 'id' );
  var filter_str = req.param( 'filter' );

  /**
   * Get List of artifacts
   */
  var sql_query = squel
          .select()
          .field( 'artifact.pk_artifact' )
          .field( 'artifact.name' )
          .field( 'artifact.configuration' )
          .field( 'artifact.derived_from_artifact' )
          .field( 'artifact.merged_from_module' )
          .field( 'COUNT(DISTINCT fk_test)', 'tests' )
          .field( 'COUNT(DISTINCT fk_test_plan)', 'plans' )
          .from( 'artifact' )
          .left_join( 'artifact_to_dt_line', null,
                      'artifact.pk_artifact = artifact_to_dt_line.fk_artifact' )
          .left_join( 'dt_line', null,
                      'dt_line.pk_dt_line = artifact_to_dt_line.fk_dt_line' )
          .left_join( 'described_template', null,
                      'described_template.pk_described_template = dt_line.fk_described_template' )
          .left_join( 'test_instance', null,
                      'test_instance.fk_described_template = described_template.pk_described_template' )
          .left_join( 'test', null, 'test.pk_test = test_instance.fk_test' )
          .left_join( 'test_plan', null,
                      'test_plan.pk_test_plan = test.fk_test_plan' )
          .where( 'artifact.fk_module = ?', module );

  // Expression for search
  var exp = squel.expr();
  if ( filter_str ) {
    var filter_exp = '%' + filter_str.replace( /["']/g, '' ) + '%';
    exp.or( 'artifact.name LIKE ?' ).or( 'artifact.configuration LIKE ?' );
    sql_query.where( exp, filter_exp, filter_exp );
  }
  

  /**
   * Group by
   */
  sql_query.group( 'artifact.pk_artifact' );

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
  case 'configuration':
    sql_query.order( 'configuration', order );
  default:
  case 'name':
    sql_query.order( 'artifact.name', order );
    break;
  }

  sql_query.order( 'artifact.pk_artifact' );
  
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

// [GET] get a list of untested artifacts
exports.untested = function (req, res) {
  mysql.getConnection( function( err,conn) {
    var query_string = 'SELECT' +
      ' module.pk_module, artifact.pk_artifact, module.name, module.version, artifact.name' +
      ' FROM artifact INNER JOIN module ON artifact.fk_module = module.pk_module' +
      ' LEFT JOIN artifact_to_dt_line ON artifact.pk_artifact = artifact_to_dt_line.fk_artifact' +
      ' GROUP BY artifact.pk_artifact HAVING COUNT(artifact_to_dt_line.fk_dt_line) = 0';
    
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
  });
}

// [GET] get a single artifact
exports.single = function (req, res) {
  var artifactid = req.param('artifactid');
  
  mysql.getConnection(function(err,conn) {
    conn.query('SELECT name, hex(fk_content) as filename FROM artifact WHERE pk_artifact = ?', artifactid,
    function (err, result) {
      if (err) throw err;

      var file = fs.statSync(config.artifacts_dir + '/' + result[0].filename);
      file.name = result[0].name;
      
      res.setHeader('Content-disposition', 'attachment; filename=' + file.name);
      res.setHeader('Content-Type', 'application/x-gzip');

      var filestream = fs.createReadStream(config.artifacts_dir + '/' + result[0].filename);
      filestream.pipe( res );
    });
    conn.release();
  });
}

// [GET] get multiple artifacts
exports.multiple = function (req, res) {
  var instanceid = req.param('instanceid');
  var pack = tar.pack() // p is a streams2 stream

  pack.entry({ name: 'my-test.txt' }, 'Hello World!');
  pack.finalize();

  res.setHeader('Content-disposition', 'attachment; filename=test.tar.gz');
  res.setHeader('Content-Type', 'application/x-gzip');
  
  var gzip = zlib.createGzip();
  pack.pipe(gzip).pipe( res );
};
