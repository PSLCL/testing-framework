var app = angular.module('qa-portal');

// Components service for handling all components
app.factory('Versions', function ($resource) {
  return $resource('/api/v1/versions', {
    after: '@id',
    filter: '@string'
  });
});
