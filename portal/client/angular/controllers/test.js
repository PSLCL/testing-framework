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
      $scope.component = Test.delete({
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
      description: ''
    };
    $scope.testSubmit = function () {
      $scope.test = Tests.save({
          testPlanId: $routeParams.testPlanId
        },
        $scope.test, function success() {
          $location.path('test_plans/' + $routeParams.testPlanId);
          socket.emit('get:stats');
        }, function fail() {
          console.log('Error: failed to save test.');
        });
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

// Edit test plan
app.controller('TestEditCtrl',
  function ($scope, $routeParams, $location, Test) {
    $scope.heading = 'Update Test';
    $scope.submitLabel = 'Update Test';
    $scope.test_plan = $routeParams.testPlanId;
    Test.get({
      testPlanId: $routeParams.testPlanId,
      testId: $routeParams.testId
    }, function success(result) {
      $scope.test = result;
    });
    $scope.testSubmit = function () {
      $scope.test = Test.save({
          testPlanId: $routeParams.testPlanId,
          testId: $routeParams.testId
        },
        $scope.test, function success() {
          $location.path('test_plans/' + $routeParams.testPlanId);
        }, function fail() {
          console.log('Error: failed to update test');
        });
    };
  });

// Delete test plan
app.controller('TestDeleteCtrl',
  function ($scope, $routeParams, $location, Test, socket) {
    $scope.component = Test.delete({
        testPlanId: $routeParams.testPlanId,
        testId: $routeParams.testId
      },
      function success() {
        $location.path('test_plans/' + $routeParams.testPlanId);
        socket.emit('get:stats');
      });
  });