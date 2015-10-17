var app = angular.module('qa-portal');
var Sort = require('../util/sort');

// Managing the test plan list
app.controller('TestPlanListCtrl',
  function ($scope, $rootScope, $filter, $timeout, TestPlans, TestPlan, socket) {
    $scope.busy = false;
    // Get list of test plans
    TestPlans.query(
      function success(results) {
        $scope.test_plans = results;
      });
    // Filter list of modules
    $scope.getData = function (test_plans, filter) {
      $scope.filter = filter;
      TestPlans.query({
          'order': $scope.order,
          'filter': filter
        },
        function success(results) {
          $scope.test_plans = results;
        });
    };
    // Display create module inline
    var display_create = false;
    $scope.createTestPlan = function() {
      if (!display_create) {
        $scope.displayBlock = 'display-block';
        $timeout(function(){
          $scope.createTestPlanClass = 'display-create';
        },100)
        display_create = true;
      } else {
        $scope.createTestPlanClass = '';
        $timeout(function(){
          $scope.displayBlock = '';
        },500);
        display_create = false;
      };
    };
    // Delete a test plan by id
    $scope.deleteTestPlan = function (testPlanId) {
      $scope.module = TestPlan.delete({testPlanId: testPlanId},
        function success() {
          var params;
          if ($scope.query_filter) {
            params = {
              filter: $scope.query_filter,
              sort_by: $scope.sort_by || 'id'
            };
          } else {
            params = {sort_by: $scope.sort_by || 'id'};
          }
          TestPlans.query(params,
            function success(results) {
              $scope.test_plans = results;
            });
          socket.emit('get:stats');
        });
    };

    // Get more test plans
    $scope.moreTestPlans = function () {
      if ($scope.test_plans) {
        if ($scope.busy) return;
        $scope.busy = true;
        var offset = $scope.test_plans.length;
        var params;
        if ($scope.filter) {
          params = {
            'offset': offset,
            'filter': $scope.filter,
            'order': $scope.order
          };
        } else {
          params = {'offset': offset, 'order': $scope.order};
        }
        TestPlans.query(params,
          function success(results) {
            $scope.test_plans = $scope.test_plans.concat(results);
            $scope.busy = false;
          });
      }
    };

    /**
     * Setup sort by for test plans
     * 
     * @type {Sort}
     */
    $scope.sorter = function (order) {
      $scope.order = order;
      var params = {};
      if ($scope.filter) {
        params = {
          'filter': $scope.filter,
          'order': order
        }
      } else {
        params = {
          'order': order
        }
      }
      TestPlans.query(params,
        function success(results) {
          $scope.test_plans = results;
        });
    };

    $scope.sort = new Sort(function (order) {
      $scope.order = order;
      var params = {};
      if ($scope.filter) {
        params = {
          'filter': $scope.filter,
          'order': order
        }
      } else {
        params = {
          'order': order
        }
      }
      TestPlans.query(params,
        function success(results) {
          $scope.test_plans = results;
        });
    });
  });

// Creating a new test plan
app.controller('TestPlanNewCtrl',
  function ($scope, $location, TestPlans, socket) {
    $scope.heading = 'Create New Test Plan';
    $scope.submitLabel = 'Create Test Plan';
    $scope.test_plan = {
      name: '',
      description: ''
    };
    $scope.testPlanSubmit = function () {
      $scope.test_plan = TestPlans.save(
        $scope.test_plan, function success(result) {
          $location.path('test_plans/' + result.insertId);
          socket.emit('get:stats');
        }, function fail() {
          console.log('Error: failed to save test plan');
        });
    };
  });

// View test plan
app.controller('TestPlanViewCtrl',
  function ($scope, $routeParams, $timeout, $route, TestPlan, Tests, Test, socket) {
    TestPlan.get({
      testPlanId: $routeParams.testPlanId
    }, function success(data) {
      $scope.test_plan = data.test_plan;
    });

    Tests.query({
      testPlanId: $routeParams.testPlanId
    }, function success(data) {
      console.log(data);
      $scope.tests = data;
    });
    
    // Filter list of tests
    $scope.getData = function (tests, query) {
    $scope.query_filter = query;
    TestPlan.get({ testPlanId: $routeParams.testPlanId, filter: query},
    function success(results) {
      $scope.tests = results.tests;
    });
    };

    // Display create module inline
    var display_create = false;
    $scope.createTest = function() {
      if (!display_create) {
        $scope.displayBlock = 'display-block';
        $timeout(function(){
          $scope.createTestClass = 'display-create';
        },100)
        display_create = true;
      } else {
        $scope.createTestClass = '';
        $timeout(function(){
          $scope.displayBlock = '';
        },500);
        display_create = false;
      };
    };

    // Delete a test by id
    $scope.deleteTest = function (testPlanId, testId) {
      $scope.module = Test.delete({
          testPlanId: testPlanId,
          testId: testId
        },
        function success() {
          TestPlan.get({
            testPlanId: testPlanId
          }, function success(data) {
            $scope.tests = data.tests;
          });
          socket.emit('get:stats');
        });
    };

    // Get more tests
    $scope.moreTests = function () {
      return;
      if ($scope.tests) {
        if ($scope.busy) return;
        $scope.busy = true;
        var after = $scope.tests.length > 0 ? $scope.tests[$scope.tests.length - 1].pk_test : 0;
        var params;
        if ($scope.query_filter) {
          params = {
            testPlanId: $routeParams.testPlanId,
            filter: $scope.query_filter,
            after: after
          }
        } else {
          params = {testPlanId: $routeParams.testPlanId, after: after};
        }
        TestPlan.get(params,
          function success(result) {
            if (result && result.tests) {
              var tests = result.tests;
              for (var i = 0; i < tests.length; i++) {
                $scope.tests.push(tests[i]);
              }
            }
            $scope.busy = false;
          });
      }
    };

    $scope.enableEdit = function() {
      $scope.edit_test_plan = !$scope.edit_test_plan
    }

    // Edit test in view
    $scope.heading = 'Update Test Plan';
    $scope.submitLabel = 'Update Test Plan';
    // TestPlan.get({testPlanId: $routeParams.testPlanId},
    // function success(result) {
    // $scope.test_plan = result.test_plan;
    // });
    $scope.testPlanSubmit = function () {
      $scope.test_plan = TestPlan.save({
          testPlanId: $routeParams.testPlanId},
        $scope.test_plan, function success() {
          $route.reload();
          // $location.path('test_plans');
        }, function fail() {
          console.log('failed to update test plan');
        });
      
    };

  });

//Report on module
app.controller( 'TestPlanReportCtrl',
                function( $window, $scope, $routeParams, $location, TestPlan,
                          TestPlanReport ) {
                  $scope.exportReport = function() {
                    $window.open( '/api/v1/modules/' + $scope.module.pk_module
                                  + '/report_print' );
                  };

                  TestPlan.get( {
                    testPlanId: $routeParams.testPlanId
                  }, function success( result ) {
                    $scope.plan = result.test_plan;
                  } );

                  TestPlanReport.get( {
                    testPlanId: $routeParams.testPlanId
                  }, function success( results ) {
                    $scope.result = results;
                  } );
                } );
