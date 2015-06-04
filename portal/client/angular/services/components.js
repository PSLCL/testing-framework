var app = angular.module('qa-portal');

// Components service for handling all components
app.factory('Components', function ($resource) {
  return $resource('/api/v1/components', {
    after:   '@id',
    all:     '@boolean',
    filter:  '@string',
    sort_by: '@string'
  });
});

// Components service for handling individual components
app.factory('Component', function ($resource) {
  return $resource('/api/v1/components/:componentId', {
    after: '@id',
    filter: '@string',
    sort_by: '@string'
  });
});

// Components to test plans
app.factory('ComponentTestPlan', function ($resource) {
  return $resource('/api/v1/component/test_plan');
});
