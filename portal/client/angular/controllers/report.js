var app = angular.module('qa-portal');
var Util = require('../util/util');

//Reports
app.controller('ReportsCtrl',
    function ($scope, $q, $http, $location, $anchorScroll, $window, Modules, TestPlan,
        Versions, ReportTestPlans, ReportVersions, ReportResult, promiseTracker) {
  $scope.reportTracker = promiseTracker();
  $scope.r2generation = null;
  $scope.selectedModule = {};
  $scope.r2 = { selectedModule: [], includePassed: false, includeFailed: true, includePending: false };

  $scope.selectedVersions = {};
  $scope.util = new Util();
  /**
   * Get entire list of modules (all=true)
   */
  Modules.query({all: true, sort_by: 'name'},
      function success(results) {
    $scope.modules = results;
    $scope.availableModulesR1 = angular.copy($scope.modules);
    $scope.selectedModulesR1 = []; // Leave empty initially
    $scope.availableModulesR2 = angular.copy($scope.modules);
  }
  );
  $scope.version = [];
  Versions.query(
      function success(results) {
        $scope.versions = results;
        $scope.availableVersionsR2 = []; // Leave empty initially
        $scope.selectedCombinationsR2 = []; // Leave empty initially
      }
  );

  $scope.gotoTemplate = function(id) {
    // set the location.hash to the id of
    // the element you wish to scroll to.
    var old = $location.hash();
    $location.hash('template'+id);

    // call $anchorScroll()
    $anchorScroll();
    $location.hash(old);
  };

  $scope.gotoResource = function(id) {
    // set the location.hash to the id of
    // the element you wish to scroll to.
    var old = $location.hash();
    $location.hash('resource'+id);

    // call $anchorScroll()
    $anchorScroll();
    $location.hash(old);
  };

  // Setup navigation and report state variables
  $scope.showR1 = true;
  $scope.showR1Results = false;
  $scope.showR2 = false;
  $scope.showR2Results = false;
  $scope.reportR1 = [];
  $scope.reportR2 = [];

  var pass = function (parent) {
    parent.summary.pending -= 1;
    parent.summary.pass += 1;
    var newdata = parent.chart.data;
    newdata[0].value += 1;
    newdata[2].value -= 1;
    parent.chart = { "data": newdata, "options": parent.chart.options };
  };

  $scope.passIt = function(instance, test, test_plan, version, module) {
    ReportResult.report(
        {
          hash: instance.hash,
          result: true
        }, function success() {
          instance.passed = true;
          pass( test );
          pass( test_plan );
          pass( version );
        });
  };

  var fail = function (parent) {
    parent.summary.pending -= 1;
    parent.summary.fail += 1;
    var newdata = parent.chart.data;
    newdata[1].value += 1;
    newdata[2].value -= 1;
    parent.chart = { "data": newdata, "options": parent.chart.options };
  };

  $scope.failIt = function(instance, test, test_plan, version, module) {
    ReportResult.report(
        {
          hash: instance.hash,
          result: false
        }, function success() {
          instance.passed = false;
          fail( test );
          fail( test_plan );
          fail( version );
        });
  }

  /**
   * Export report redirects to saved pdf.
   */
  $scope.exportReport = function (rpt) {
    if(rpt == 'r1') {
      if($scope.filter_r1) {
        $window.open(
            '/api/v2/report_test_plans?export=true&filter='+$scope.filter_r1);
      }
    } else {
      if($scope.select_r2) {
        $window.open(
            '/api/v1/report_versions?export=true&select='+$scope.select_r2+"&filter="+$scope.filter_r2+"&incl_passed="+$scope.r2.includePassed+"&incl_failed="+$scope.r2.includeFailed+"&incl_pending="+$scope.r2.includePending);
      }
    }
  };

  // Define functions
  $scope.switchReport = function (rpt) {
    // Handle inner-page navigation
    $scope.showR1 = false;
    $scope.showR2 = false;
    if (rpt == 'r1') {
      $scope.showR1 = true;
    }
    if (rpt == 'r2') {
      $scope.showR2 = true;
    }
  };

  /**
   * Find a module in an array of modules
   *
   * @param array
   * @param id
   * @returns {*}
   */
  function findModule(array,id) {
    for(var i = 0; i < array.length; i++) {
      if (array[i].pk_module == id) {
        return array[i];
      }
    }
  }

  $scope.xFunction = function() {
    return function(d) { console.log(d); return d.key; }
  }

  function getValue(d) {
    console.log( "Report length: " + d.data.length );
    //if ( y < d.data.length )
    //  return d.data[y].summary.pending;
    //else
    return 1;
  }

  $scope.yFunction = function(y) {
    return getValue;
  }

  /**
   * Get report for 'r1' or 'r2'
   * @param rpt
   */
  $scope.getReport = function (rpt) {
    // Handle report generation
    if (rpt == 'r1') {
      $scope.showR1Results = true;
      $scope.filter_r1 = $scope.util.join_by(
          'pk_module', $scope.selectedModulesR1
      );
      ReportTestPlans.query({filter: $scope.filter_r1},
          function success(results) {
        $scope.reportR1 = [];
        angular.forEach(results, function (test_plan) {
          var module
          = findModule($scope.reportR1, test_plan.pk_module) || {
            name: test_plan.name,
            pk_module: test_plan.pk_module,
            test_plans: []
          };
          if($scope.reportR1.indexOf(module) == -1) {
            $scope.reportR1.push(module);
          }
          var key = $scope.reportR1.indexOf(module);
          TestPlan.get({testPlanId: test_plan.pk_test_plan},
              function success(result) {
            $scope.reportR1[key].test_plans.push(result);
          });
        });
      }, function fail() {
        $scope.reportR1 = [];
      });
    } else if (rpt == 'r2') {
      $scope.showR2Results = true;
      $scope.select_r2 = $scope.util.join_by(
          'pk_version', $scope.selectedCombinationsR2
      );

      if ( $scope.r2generation != null )
        $scope.r2generation.reject('Cancelled');

      $scope.r2generation = $q.defer();
      $scope.reportR2 = [];
      $scope.descriptionsR2 = [];
      $scope.r2_resources = [];

      ReportVersions.query({
        select: $scope.select_r2,
        incl_passed: $scope.r2.includePassed,
        incl_failed: $scope.r2.includeFailed,
        incl_pending: $scope.r2.includePending},
        function success(results) {
          $scope.reportR2 = results.details;
          $scope.descriptionsR2 = results.descriptions;
          $scope.r2_resources = results.resources;

          angular.forEach( results.details, function( module ) {
            angular.forEach( module.versions, function ( version ) {
              var data = [
                          {
                            value: version.summary.pass,
                            color:"#00CC00"
                          },
                          {
                            value : version.summary.fail,
                            color : "#FF0000"
                          },
                          {
                            value : version.summary.pending,
                            color : "#A0A0A0"
                          }
                          ];
              version.chart = { "data": data, "options": { "animation": false, "animateRotate": false, "segmentShowStroke": false } };

              angular.forEach( version.test_plans, function( test_plan ) {
                var data = [
                            {
                              value: test_plan.summary.pass,
                              color:"#00CC00"
                            },
                            {
                              value : test_plan.summary.fail,
                              color : "#FF0000"
                            },
                            {
                              value : test_plan.summary.pending,
                              color : "#A0A0A0"
                            }
                            ];
                test_plan.chart = { "data": data, "options": { "animation": false, "animateRotate": false, "segmentShowStroke": false } };
              });
            });
          });

          $scope.r2generation.resolve();
        }, function fail() {
          console.log( "FAILURE" );
          $scope.reportR2 = [];
          $scope.descriptionsR2 = [];
          $scope.r2generation.reject('Failure');
        });
    }
  };

  /**
   * Move modules based on target
   * @param target
   */
  $scope.moveSelectedModulesR1 = function (target) {
    for (var i = 0; i < $scope.selectedModule.length; i++) {
      if (target == 'selected') {
        var idx_s = $scope.availableModulesR1.indexOf($scope.selectedModule[i]);
        if (idx_s != -1) {
          $scope.selectedModulesR1.push($scope.selectedModule[i]);
          $scope.availableModulesR1.splice(idx_s, 1);
        }
      } else if (target === 'available') {
        var idx_a = $scope.selectedModulesR1.indexOf($scope.selectedModule[i]);
        if (idx_a != -1) {
          $scope.availableModulesR1.push($scope.selectedModule[i]);
          $scope.selectedModulesR1.splice(idx_a, 1);
        }
      }
    }
  };

  /**
   * Get available versions by selected module
   */
  $scope.generateAvailableVersionsR2 = function ( c ) {
    // Build the versions list based on selected modules
    $scope.availableVersionsR2 = [];
    var module = $scope.r2.selectedModule[0];
    angular.forEach($scope.versions, function (valueL1) {
      if (module.pk_module == valueL1.fk_module) {
        var doPush = true;
//      angular.forEach($scope.selectedCombinationsR2, function (valueL2) {
//      if (valueL1.pk_version != valueL2.pk_version) {
//      doPush = false;
//      }
//      }
//      );
        if (doPush) {
          valueL1.name = valueL1.version + " - " + module.name;
          $scope.availableVersionsR2.push(valueL1);
        }
      }
    });
  };

  /**
   * Move version into selected
   * @param target
   */
  $scope.moveSelectedVersionsR2 = function (target) {
    // Move options from available/selected
    if (target == 'selected') {
      angular.forEach($scope.selectedVersions, function (version) {
        $scope.selectedCombinationsR2.push(version);
      });
      $scope.generateAvailableVersionsR2();
    }
    if (target == 'available') {
      var deadElementPKs = [];
      var selectedCombinationsR2Selector = jQuery('#selectedCombinationsR2Selector');
      angular.forEach($scope.selectedVersions, function (version) {
        deadElementPKs[version.pk_version] = version;
      });
      var tempSelectedCombinationsR2 = angular.copy($scope.selectedCombinationsR2);
      $scope.selectedCombinationsR2 = [];
      angular.forEach(tempSelectedCombinationsR2, function (valueL1) {
        if (deadElementPKs[valueL1.pk_version] == undefined) {
          $scope.selectedCombinationsR2.push(valueL1);
        }
      });
      $scope.generateAvailableVersionsR2();
    }
  };

  $scope.gotoTag = function(tag){
    $location.hash(tag);
    $anchorScroll();
  }
});