var app = angular.module('qa-portal');

// Modules service for handling all modules
app.factory('UntestedArtifacts', function ($resource) {
  return $resource('/api/v1/artifacts/untested', {});
});