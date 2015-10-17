var app = angular.module('qa-portal');

// Managing the test list by test plan
app.controller('TestListCtrl',
  function ($scope, $routeParams, $filter, Tests, Test) {
    $scope.busy = false;
    $scope.test_plan = $routeParams.testPlanId;
    $scope.tests = Tests.query({testPlanId: $routeParams.testPlanId });
    $scope.getData = function (tests, query) {
      $scope.queryData = $filter('filter')(tests, query);
    };
    // Delete a test by id
    $scope.deleteTest = function (testPlanId, testId) {
      $scope.module = Test.delete({
          testPlanId: testPlanId,
          testId: testId
        },
        function success() {
          Tests.query({testPlanId: testPlanId},
            function success(results) {
              $scope.tests = results;

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
        Tests.query({ after: after },
          function success(results) {
            if (results[0]) $scope.tests.push(results[0]);
            $scope.busy = false;
          });
      }
    };
  });

// Creating a new test plan
app.controller('TestNewCtrl',
  function ($scope, $routeParams, $location, Tests, socket) {
    $scope.heading = 'Create New Test';
    $scope.submitLabel = 'Create Test';
    $scope.test_plan = $routeParams.testPlanId;
    $scope.test = {
      fk_test_plan: $routeParams.testPlanId,
      name: '',
      description: '',
      script: ''
    };
    $scope.testSubmit = function () {
      Tests.save({
          testPlanId: $routeParams.testPlanId
        },
        $scope.test, function success() {
          $location.path('test_plans/' + $routeParams.testPlanId);
          socket.emit('get:stats');
        }, function fail() {
          console.log('Error: failed to save test.');
        });
      $scope.test = {
      fk_test_plan: $routeParams.testPlanId,
      name: '',
      description: '',
      script: ''
    };
    };
  });

// View test
app.controller('TestViewCtrl',
  function ($scope, $routeParams, Test) {
    $scope.test_plan = $routeParams.testPlanId;
    $scope.test = Test.get({
      testPlanId: $routeParams.testPlanId,
      testId: $routeParams.testId
    });
  });

// Report
app.controller('TestReportCtrl',
  function ($scope, $routeParams, Test, TestReport) {
    $scope.test = Test.get({
      testPlanId: $routeParams.testPlanId,
      testId: $routeParams.testId
    });
  TestReport.get( {
    testId: $routeParams.testId
  }, function success( results ) {
    $scope.result = results;
  } );
  });