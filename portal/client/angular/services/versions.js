var app = angular.module('qa-portal');

// Modules service for handling all modules
app.factory('Versions', function ($resource) {
  return $resource('/api/v1/versions', {
    after: '@id',
    filter: '@string'
  });
});
