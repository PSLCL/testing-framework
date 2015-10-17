var app = angular.module('qa-portal');

// Modules service for handling all modules
app.factory('Modules', function ($resource) {
  return $resource('/api/v1/modules', {
    filter:  '@string',
    order:   '@string',
    limit:   '@string',
    offset:  '@string'
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
    return $resource('/api/v1/instances?module=:moduleId', {});
})

// Modules to test plans
app.factory('ModuleTestPlan', function ($resource) {
  return $resource('/api/v1/module/test_plan');
});

//Modules service for handling all modules
app.factory('Artifacts', function ($resource) {
  return $resource('/api/v1/modules/:moduleId/artifacts', {
    filter:  '@string',
    order:   '@string',
    limit:   '@string',
    offset:  '@string'
  });
});
