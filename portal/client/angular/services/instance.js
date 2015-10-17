var app = angular.module('qa-portal');

// Modules service for handling all modules
app.factory('Instance', function ($resource) {
  return $resource('/api/v1/instance/:id', {});
});

//Modules service for handling all modules
app.factory('Template', function ($resource) {
  return $resource('/api/v1/template/:id', {});
});
