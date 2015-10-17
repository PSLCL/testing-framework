// Dashboard stats services
var app = angular.module('qa-portal');

// Stats service for handling dashboard stats
app.factory('Stats', function ($resource) {
  return $resource('/api/v1/stats');
});

// Get runrates
app.factory('RunRates', function ($resource) {
  return $resource('/api/v1/runrates');
});

app.factory('AuthenticUser', function($resource) {
    return $resource('/loggedin');
});
