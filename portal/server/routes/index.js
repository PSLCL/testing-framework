var mysql = require( '../lib/mysql' );
var _        = require('underscore');

var error = function( err, req, res ) {
  console.log( 'Failure: ' + err );
  res.send( 504, 'Resource failure.' );
};

/**
 * Return the index page, which is the main application page.
 */
exports.index = function( req, res ) {
  res.render( 'index' );
};

/**
 * Return counts of main objects in the database for a dashboard.
 */
exports.stats = function( req, res ) {
  mysql.getConnection( function( err, conn ) {
    if ( err ) {
      error( err, req, res );
      return;
    }

    conn.query( 'SELECT' + '(SELECT COUNT(*) FROM module) AS module_count,'
                + '(SELECT COUNT(*) FROM test_plan) AS test_plan_count,'
                + '(SELECT COUNT(*) FROM test) AS test_count,'
                + '(SELECT COUNT(*) FROM content) AS artifact_count,'
                + '(SELECT COUNT(*) FROM test_instance) AS ti_count,'
                + '(SELECT COUNT(*) FROM test_instance WHERE fk_run IS NULL) AS ti_pending,'
                + '(SELECT COUNT(*) FROM test_instance INNER JOIN run ON run.pk_run = test_instance.fk_run WHERE end_time IS NULL) AS ti_running',
                function( err, result ) {
                  conn.release();

                  if ( err ) {
                    console.log( err );
                    error( err, req, res );
                    return;
                  }

                  res.send( result[ 0 ] );
                } );
  } );
};

/**
 * Return histograms for run statistics between dates and minute ranges.
 */
exports.runrates = function( req, res ) {
  var monthNames = [ 'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec' ];
  
  mysql.getConnection( function( err, conn ) {
    if ( err ) {
      error( err, req, res );
      return;
    }

    var from = req.param('from');
    if ( from == null )
      from = (new Date().getTime() / 1000) - (7*24*60*60);
    
    var to = req.param('to');
    if ( to == null )
      to = new Date().getTime() / 1000;
    
    var bucket = req.param('bucket');
    if ( bucket == null )
      bucket = 60;
    
    bucket = bucket * 60;
    from -= (from % bucket);
    to -= (to % bucket);
    
    conn.query('SELECT FLOOR(UNIX_TIMESTAMP(start_time)/' + bucket + ') AS t, COUNT(*) AS total FROM run WHERE start_time IS NOT NULL AND UNIX_TIMESTAMP(start_time) > ' + from + ' AND UNIX_TIMESTAMP(start_time) < ' + to + ' GROUP BY t;'
               + 'SELECT FLOOR(UNIX_TIMESTAMP(ready_time)/' + bucket + ') AS t, COUNT(*) AS total FROM run WHERE ready_time IS NOT NULL GROUP BY t;'
               + 'SELECT FLOOR(UNIX_TIMESTAMP(end_time)/' + bucket + ') AS t, COUNT(*) AS total FROM run WHERE end_time IS NOT NULL GROUP BY t;',
              function( err, result ) {
                conn.release();
                
                if ( err ) {
                  console.log( err );
                  error( err, req, res );
                  return;
                }
                
                var labels = [];
                var starting = [];
                var running = [];
                var complete = [];
                var lastDay = -1;
                for ( quanta = from; quanta <= to; quanta += bucket ) {
                  var bucket_value = quanta / bucket;
                  var D = new Date( quanta*1000 );
                  if ( D.getDate() != lastDay ) {
                    lastDay = D.getDate();
                    labels.push( D.getDate() + ' ' + monthNames[D.getMonth()] );
                  }
                  else
                    labels.push( '' );
                  
                  var start = _.find( result[0], function(r) { return r.t == bucket_value; } );
                  if ( start !== undefined )
                    starting.push( start.total );
                  else
                    starting.push( 0 );
                  
                  var run = _.find( result[1], function(r) { return r.t == bucket_value; } );
                  if ( run !== undefined )
                    running.push( run.total );
                  else
                    running.push( 0 );
                  
                  var done = _.find( result[2], function(r) { return r.t == bucket_value; } );
                  if ( done !== undefined )
                    complete.push( done.total );
                  else
                    complete.push( 0 );
                }
                
                var startGraph = {
                                  options: {},
                                  data: {
                                    labels: labels,
                                    datasets: [ { data: starting } ]
                                  }
                };
                                  
                var runningGraph = {
                                    options: {},
                                    data: {
                                      labels: labels,
                                      datasets: [ { data: running } ]
                                    }
                };

                var completeGraph = {
                                    options: {},
                                    data: {
                                      labels: labels,
                                      datasets: [ { data: complete } ]
                                    }
                };

                res.send( { 'from': from, 'to': to, 'bucket': bucket, labels: labels, 'starting': startGraph, 'running': runningGraph, 'complete': completeGraph } );
              } );
  } );
};