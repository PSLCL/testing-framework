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
    $scope.getData = function (test_plans, query) {
      $scope.query_filter = query;
      TestPlans.query({
          sort_by: $scope.sort_by || 'id',
          filter: query
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
            params = {after: after, sort_by: $scope.sort_by || 'id'};
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
        var after = $scope.test_plans[$scope.test_plans.length - 1].pk_test_plan;
        var params;
        if ($scope.query_filter) {
          params = {
            after: after,
            filter: $scope.query_filter,
            sort_by: $scope.sort_by || 'id'
          };
        } else {
          params = {after: after, sort_by: $scope.sort_by || 'id'};
        }
        TestPlans.query(params,
          function success(results) {
            if (results[0]) $scope.test_plans.push(results[0]);
            $scope.busy = false;
          });
      }
    };

    /**
     * Setup sort by for test plans
     * @type {Sort}
     */
    $scope.sort = new Sort(function (sort_by) {
      $scope.sort_by = sort_by;
      var params = {};
      if ($scope.query_filter) {
        params = {
          query: $scope.query_filter,
          sort_by: sort_by
        }
      } else {
        params = {
          sort_by: sort_by
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
  function ($scope, $routeParams, $timeout, $route, TestPlan, Test, socket) {
    TestPlan.get({
      testPlanId: $routeParams.testPlanId
    }, function success(data) {
      $scope.test_plan = data.test_plan;
      $scope.tests = data.tests;
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
      if ($scope.tests) {
        if ($scope.busy) return;
        $scope.busy = true;
        var after = $scope.tests[$scope.tests.length - 1].pk_test;
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
    TestPlan.get({testPlanId: $routeParams.testPlanId},
    function success(result) {
      $scope.test_plan = result.test_plan;
    });
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

// Edit test plan
app.controller('TestPlanEditCtrl',
  function ($scope, $routeParams, $location, TestPlan) {
    $scope.heading = 'Update Test Plan';
    $scope.submitLabel = 'Update Test Plan';
    TestPlan.get({testPlanId: $routeParams.testPlanId},
    function success(result) {
      $scope.test_plan = result.test_plan;
    });
    $scope.testPlanSubmit = function () {
      $scope.test_plan = TestPlan.save({
          testPlanId: $routeParams.testPlanId},
        $scope.test_plan, function success() {
          $location.path('test_plans');
        }, function fail() {
          console.log('failed to update test plan');
        });
    };
  });

// Delete test plan
app.controller('TestPlanDeleteCtrl',
  function ($scope, $routeParams, $location, TestPlan, socket) {
    $scope.module = TestPlan.delete({testPlanId: $routeParams.testPlanId},
      function success() {
        $location.path('test_plans');
        socket.emit('get:stats');
      });
  });
