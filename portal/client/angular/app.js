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

app.config(['$routeProvider',
            function ($routeProvider) {
  $routeProvider.
  when('/components', {
    templateUrl: 'partials/components/list.html',
    controller: 'ComponentListCtrl'
  }).
  when('/components/new', {
    templateUrl: 'partials/components/form.html',
    controller: 'ComponentNewCtrl'
  }).
  when('/components/:componentId', {
    templateUrl: 'partials/components/view.html',
    controller: 'ComponentViewCtrl'
  }).
  when('/components/:componentId/edit', {
    templateUrl: 'partials/components/form.html',
    controller: 'ComponentEditCtrl'
  }).
  when('/components/:componentId/destroy', {
    templateUrl: 'partials/components/view.html',
    controller: 'ComponentDeleteCtrl'
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
  when('/login', {
    templateUrl: 'partials/login.html',
    controller: 'LoginCtrl'
  }).
  when('/logout', {
    templateUrl: 'partials/login.html',
    controller: 'LogoutCtrl'
  }).
  when('/dashboard', {
    templateUrl: 'partials/dashboard.html',
    controller: 'DashboardCtrl'
  }).
  when('/admin_dashboard', {
    templateUrl: 'partials/dashboard.html',
    controller: 'AdminDashboardCtrl'
  }).
  otherwise({
    redirectTo: '/dashboard'
  });
}]);

//Interceptor for handling request and 401 unauthorized error
app.config(function ($httpProvider) {
  $httpProvider.interceptors.push(
      function ($rootScope, $location, $cookieStore, $q) {

        return {
          'request': function (request) {
            $rootScope.currentUser = $cookieStore.get('authdata');
            $rootScope.currentUserName = $cookieStore.get('authname');
            if ($rootScope.currentUser) {
              /** @namespace $httpProvider.defaults.headers.common */
              $httpProvider.defaults.headers.common['Authorization'] =
                'Basic ' + $rootScope.currentUser;
            }
            if (request.method != 'GET'
              && !$rootScope.currentUser
              && $location.path() != '/login'
                && $location.path() != '/reports') {
              $rootScope.loginError = '';
              $rootScope.loginWarning = 'You are not currently logged in.';
              $location.path('/login');
            }
            return request;
          },
          'responseError': function (rejection) {
            // if we're not logged-in to the web service, redirect to login page
            if (rejection.status === 401 && $location.path() != '/login') {
              if ($rootScope.currentUser) {
                $cookieStore.remove('authdata');
                $rootScope.currentUser = '';
                $rootScope.loginWarning = '';
                $rootScope.loginError = 'Invalid username/password.';
              }
              $location.path('/login');
            }
            return $q.reject(rejection);
          }
        };
      });
});