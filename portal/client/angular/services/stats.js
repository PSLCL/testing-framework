// Dashboard stats services
var app = angular.module('qa-portal');

// Stats service for handling dashboard stats
app.factory('Stats', function ($resource) {
  return $resource('/api/v1/stats');
});

// Stats service for handling dashboard stats
app.factory('AdminStats', function ($resource) {
  return $resource('/api/v1/stats/admin');
});

app.factory('AuthenticUser', function($resource) {
    return $resource('/loggedin');
});
