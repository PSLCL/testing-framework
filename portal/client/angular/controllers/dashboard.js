var app = angular.module('qa-portal');

// Dashboard page
app.controller('DashboardCtrl',
  function ($scope, $rootScope, $location, Stats, AuthenticUser, socket) {
    $scope.stats = {
      comp_count: 0,
      test_plan_count: 0,
      test_count: 0
    };
    $scope.isActive = function (viewLocation) {
      return (viewLocation === $location.path());
    };
    $scope.stats = Stats.get();
    $scope.user = AuthenticUser.get();
        
    var sidebarIsHidden = false;
    $scope.hideSidebar = function(){ 
      if (!sidebarIsHidden) {
        $scope.sidebarClass = 'collapsed';
        sidebarIsHidden = true;
      } else {
        $scope.sidebarClass = '';
        sidebarIsHidden = false;
      }
    }

    // Listeners for socket.io
    socket.on('init', function (data) {
      // TODO: Implement initialization values
    });
    // Load stats data
    socket.on('get:stats', function (data) {
      $scope.stats = data;
    })
  });

// Admin Dashboard page
app.controller('AdminDashboardCtrl',
  function ($scope, $rootScope, $location, AdminStats, socket) {
    $scope.stats = {
      comp_count: 0,
      test_plan_count: 0,
      test_count: 0
    };
    $scope.isActive = function (viewLocation) {
      return (viewLocation === $location.path());
    };
    $scope.stats = AdminStats.get();

    // Listeners for socket.io
    socket.on('init', function (data) {
      // TODO: Implement initialization values
    });
    // Load stats data
    socket.on('get:stats', function (data) {
      $scope.stats = data;
    })
  });
