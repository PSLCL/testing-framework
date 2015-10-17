var app = angular.module('qa-portal');

// Test plans service for handling all test plans
app.factory('TestPlans', function ($resource) {
  return $resource('/api/v1/test_plans', {
    filter:  '@string',
    order:   '@string',
    limit:   '@string',
    offset:  '@string'
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

app.factory('TestPlanReport', function($resource) {
  return $resource('/api/v1/instances?plan=:testPlanId', {});
})

