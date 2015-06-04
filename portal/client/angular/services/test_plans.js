var app = angular.module('qa-portal');

// Test plans service for handling all test plans
app.factory('TestPlans', function ($resource) {
  return $resource('/api/v1/test_plans', {
    after: '@id',
    filter: '@string',
    sort_by: '@string'
  });
});

// Test plan service for handling individual test plans
app.factory('TestPlan', function ($resource) {
  return $resource('/api/v1/test_plans/:testPlanId', {
    after: '@id',
    filter: '@string',
    sort_by: '@string'
  });
});
