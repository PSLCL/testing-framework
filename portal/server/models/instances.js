/** Mysql Connection */
var mysql  = require('../lib/mysql');
var config = require('../../config/config');
var _      = require('underscore');
var d3     = require('d3');


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
	if(!ls)
		return null;
    var la = ls.split(',');
    return _.map( la, function(i) {
        return m[i];
    });
};

/**
 * Get a list of test instances and result summaries.
 */
module.exports.list_instances = function( plan, test, module_str, against, callback ) {
  var includeInstances = false;
  mysql.getConnection(function(err,conn) {
    if(err){
      console.log("Error getting mysql connection: " + err);
      callback(err);
    }

    var statement = 'call get_instance_list('+ plan + ',' + test + ',' + module_str + ',"' + against + '");';
    conn.query(statement, function (err, result) {
      if(err){
        console.log("Error getting instance data: " + err);
        callback(err);
      }

      var R = {
                instances: [],
                descriptions: []
              };

      if(result != null && result.length > 1){
        /** Build a list of module names */
        var modules = buildModules( result[3] );

        /** The list is returned in sorted order by test plan name and test name. */
        R.instances = d3.nest()
            .key( function(d) { return d.pk_test_plan } )
            .key( function(d) { return d.pk_test } )
            .entries(result[0]);

        R.instances = _.map( R.instances, function( test_plan ) {

          var docs = _.find( result[1], function(e) { return e.pk_test_plan == test_plan.key; } );

          var vals = _.map( test_plan.values, function( test ) {

            var docs = _.find( result[2], function(e) { return e.pk_test == test.key; } );
            var failedInstances = [];
            var summary = _.countBy(test.values, function(i) {
                if ( i.result == null ) return 'pending';
                if(i.result == 0){
                  failedInstances.push(i.pk_test_instance);
                  return 'failed';
                }
                return 'passed';
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

            var items = !includeInstances ? [] : _.map( test.values, function(v) {
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
                values: items,
                failed: failedInstances 
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

      return callback(null, R);
    });
  });
}
