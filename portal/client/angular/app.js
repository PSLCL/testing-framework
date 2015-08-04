'use strict';

var app = angular.module('qa-portal',
    [
     'qa-portal.directives',
     'ngRoute',
     'infinite-scroll',
     'ngResource',
     'ngCookies',
     'ngSanitize',
     'nvd3ChartDirectives',
     'chartjs-directive',
     'ajoslin.promise-tracker',
     'cgBusy',
     'ngAnimate'
     ]);

var checkLoggedin = function($q, $timeout, $http, $location, $rootScope) {
    var deferred = $q.defer();
    $http.get('/loggedin').success( function(user) {
        if ( user !== '0' )
            deferred.resolve();
        else {
            $rootScope.message = 'You need to log in.';
            deferred.reject();
            $location.url('/auth/atlassian-oauth');
            $location.replace();
        }
    });
    
    return deferred.promise;
};

app.config(['$routeProvider',
            function ($routeProvider) {
  $routeProvider.
  when('/modules', {
    templateUrl: 'partials/modules/list.html',
    controller: 'ModuleListCtrl'
  }).
  when('/modules/new', {
    templateUrl: 'partials/modules/form.html',
    controller: 'ModuleNewCtrl'
  }).
  when('/modules/:moduleId', {
    templateUrl: 'partials/modules/view.html',
    controller: 'ModuleViewCtrl'
  }).
  when('/modules/:moduleId/edit', {
    templateUrl: 'partials/modules/form.html',
    controller: 'ModuleEditCtrl'
  }).
  when('/modules/:moduleId/destroy', {
    templateUrl: 'partials/modules/view.html',
    controller: 'ModuleDeleteCtrl'
  }).
  when('/test_plans', {
    templateUrl: 'partials/test_plans/list.html',
    controller: 'TestPlanListCtrl'
  }).
  when('/test_plans/new', {
    templateUrl: 'partials/test_plans/form.html',
    controller: 'TestPlanNewCtrl'
  }).
  when('/test_plans/:testPlanId', {
    templateUrl: 'partials/test_plans/view.html',
    controller: 'TestPlanViewCtrl'
  }).
  when('/test_plans/:testPlanId/edit', {
    templateUrl: 'partials/test_plans/form.html',
    controller: 'TestPlanEditCtrl'
  }).
  when('/test_plans/:testPlanId/destroy', {
    templateUrl: 'partials/test_plans/view.html',
    controller: 'TestPlanDeleteCtrl'
  }).
  when('/test_plans/:testPlanId/tests', {
    templateUrl: 'partials/tests/list.html',
    controller: 'TestListCtrl'
  }).
  when('/test_plans/:testPlanId/tests/new', {
    templateUrl: 'partials/tests/form.html',
    controller: 'TestNewCtrl'
  }).
  when('/test_plans/:testPlanId/tests/:testId', {
    templateUrl: 'partials/tests/view.html',
    controller: 'TestViewCtrl'
  }).
  when('/test_plans/:testPlanId/tests/:testId/edit', {
    templateUrl: 'partials/tests/form.html',
    controller: 'TestEditCtrl'
  }).
  when('/test_plans/:testPlanId/tests/:testId/destroy', {
    templateUrl: 'partials/tests/view.html',
    controller: 'TestDeleteCtrl'
  }).
  when('/reports', {
    templateUrl: 'partials/reports/list.html',
    controller: 'ReportsCtrl'
  }).
  when('/dashboard', {
    templateUrl: 'partials/dashboard.html',
    controller: 'DashboardCtrl'
  }).
  when('/admin_dashboard', {
    templateUrl: 'partials/dashboard.html',
    controller: 'AdminDashboardCtrl',
    resolve: {
        loggedin: checkLoggedin
    }
  }).
  otherwise({
    redirectTo: '/dashboard'
  });
}]);

//Interceptor for handling request and 401 unauthorized error
app.config(function ($httpProvider) {
  $httpProvider.interceptors.push(function($q, $location) {
    return {
      response: function(response) {
        return response;
      },
      responseError: function(response) {
        if ( response.status === 401 )
          $location.url('/auth/atlassian-oauth');
        return $q.reject(response);
      }
    };
  });
});