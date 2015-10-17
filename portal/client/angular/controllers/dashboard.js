var app = angular.module('qa-portal');

// Dashboard page
app.controller('DashboardCtrl',
  function ($scope, $rootScope, $location, Stats, RunRates, ReportUserTests, UntestedArtifacts, AuthenticUser, socket) {
    $scope.stats = {
      comp_count: 0,
      test_plan_count: 0,
      test_count: 0
    };
    $scope.isActive = function (viewLocation) {
      return (viewLocation === $location.path());
    };
    $scope.stats = Stats.get();
    $scope.runrates = RunRates.get();
    $scope.untested = UntestedArtifacts.query();
    $scope.untested.$promise.then( function( untested ) {
        $scope.untested_count = untested.length;
        });
    $scope.user = AuthenticUser.get();
    $scope.user.$promise.then( function( user ) {
        var username = user.email;
        if ( user.isAdmin )
            username = null;
            
        ReportUserTests.query( { username }, function ( result ) {
            $scope.usertests = result;
        });
    });
        
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
