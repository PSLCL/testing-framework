var app = angular.module('qa-portal');

// Modules service for handling all modules
app.factory('Modules', function ($resource) {
  return $resource('/api/v1/modules', {
    after:   '@id',
    all:     '@boolean',
    filter:  '@string',
    sort_by: '@string'
  });
});

// Modules service for handling individual modules
app.factory('Module', function ($resource) {
  return $resource('/api/v1/modules/:moduleId', {
    after: '@id',
    filter: '@string',
    sort_by: '@string'
  });
});

app.factory('ModuleReport', function($resource) {
    return $resource('/api/v1/modules/:moduleId/report', {});
})

// Modules to test plans
app.factory('ModuleTestPlan', function ($resource) {
  return $resource('/api/v1/module/test_plan');
});
