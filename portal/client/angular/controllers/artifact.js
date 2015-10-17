var app = angular.module('qa-portal');

// Managing the test list by test plan
app.controller('ArtifactListCtrl',
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