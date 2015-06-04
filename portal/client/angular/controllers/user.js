var app = angular.module('qa-portal');

// Handle login
app.controller('LoginCtrl',
  function ($scope, $rootScope, $location, $window, Auth) {
    $scope.user = {
      username: '',
      password: ''
    };
    $scope.loginUser = function () {
      console.log("loginUser called");
      // Do basic authentication
      Auth.login($scope.user);
      $rootScope.currentUserName = $scope.user.username;
      $location.path('admin_dashboard');
    };
    $scope.logoutUser = function () {
      Auth.logout();
      $location.path('dashboard');
      $rootScope.currentUserName = "";
    };
  });

// Handle logout
app.controller('LogoutCtrl',
  function ($rootScope, $location, $window, Auth) {
    Auth.logout();
    $location.path('dashboard');
    $rootScope.currentUserName = "";
  });