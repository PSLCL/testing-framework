var app = angular.module('qa-portal');

// Tests service for handling all tests
app.factory('Tests', function ($resource) {
  return $resource('/api/v1/test_plans/:testPlanId/tests');
});

// Test service for handling individual tests
app.factory('Test', function ($resource) {
  return $resource('/api/v1/test_plans/:testPlanId/tests/:testId');
});

app.factory('TestReport', function($resource) {
  return $resource('/api/v1/instances?test=:testId', {});
})

