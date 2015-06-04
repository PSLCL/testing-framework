'use strict';

// Dashboard page
app.controller('DashboardCtrl',
  function ($scope, $rootScope, $location, Stats, socket) {
    $scope.stats = {
      comp_count: 0,
      test_plan_count: 0,
      test_count: 0
    };
    $scope.isActive = function (viewLocation) {
      return (viewLocation === $location.path());
    };
    
    $scope.stats = Stats.get();

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

// Reports
app.controller('ReportsCtrl', function ($scope, $location) {
  // TODO: Implement reports
});

// Handle login
app.controller('LoginCtrl',
  function ($scope, $rootScope, $location, $window, Auth) {
    $scope.user = {
      username: '',
      password: ''
    };
    $scope.loginUser = function () {
      // Do basic authentication
      Auth.login($scope.user);
      $rootScope.currentUser = $scope.user.username;
      $location.path('dashboard');
    };
    $scope.logoutUser = function () {
      Auth.logout();
      $location.path('/');
    };
  });

// Handle logout
app.controller('LogoutCtrl',
  function ($rootScope, $location, $window, Auth) {
    Auth.logout();
    $location.path('/login');
  });

// Managing the component list
app.controller('ComponentListCtrl',
  function ($scope, $rootScope, $filter, $timeout, Components, Component, socket) {
    $scope.busy = false;
    // Get list of components
    Components.query(
      function success(results) {
        $scope.components = results;
      });

    // Filter list of components
    $scope.getData = function (components, query) {
      $scope.queryData = $filter('filter')(components, query);
    };

    // Delete a component by id
    $scope.deleteComponent = function (componentId) {
      $scope.component = Component.delete({componentId: componentId},
        function success() {
          Components.query(
            function success(results) {
              $scope.components = results;
            });
          socket.emit('get:stats');
        });
    };

    // Display create component inline
    var display_create = false;
    $scope.createComponent = function() {
      if (!display_create) {
        $scope.displayBlock = 'display-block';
        $timeout(function(){
          $scope.createComponentClass = 'display-create';
        },100)
        display_create = true;
      } else {
        $scope.createComponentClass = '';
        $timeout(function(){
          $scope.displayBlock = '';
        },500);
        display_create = false;
      };
    };

    // Get more components
    $scope.moreComponents = function () {
      if ($scope.components) {
        if ($scope.busy) return;
        $scope.busy = true;
        var after = $scope.components[$scope.components.length - 1].pk_component;
        Components.query({ after: after },
          function success(results) {
            if (results[0]) $scope.components.push(results[0]);
            $scope.busy = false;
          });
      }
    };
  });

// Creating a new component
app.controller('ComponentNewCtrl',
  function ($scope, $location, Components, socket) {
    $scope.heading = 'Create new Component';
    $scope.submitLabel = 'Create Component';
    $scope.component = {
      name: ''
    };
    $scope.submitFunction = function () {
      $scope.component = Components.save(
        $scope.component, function success(result) {
          socket.emit('get:stats');
          $location.path('components/' + result.insertId);
        }, function fail() {
          console.log('Error: failed to save component.');
        });
    };
  });

// View component
app.controller('ComponentViewCtrl',
  function ($scope, $routeParams, $timeout, Component, ComponentTestPlan) {
    $scope.busy = false;
    Component.get({componentId: $routeParams.componentId},
      function success(result) {
        $scope.component = result.component;
        $scope.test_plans = result.test_plans;
      });
    $scope.addTestPlan = function (componentId, testPlanId, testPlanSelected) {
      if(componentId === testPlanSelected){
        ComponentTestPlan.delete(
        {
          fk_component: componentId,
          fk_test_plan: testPlanId
        }, function success() {
          Component.get({componentId: componentId},
            function success(result) {
              $scope.component = result.component;
              $scope.test_plans = result.test_plans;
            });
        });
      } else {
        ComponentTestPlan.save(
        {
          fk_component: componentId,
          fk_test_plan: testPlanId
        }, function success() {
          Component.get({componentId: componentId},
            function success(result) {
              $scope.component = result.component;
              $scope.test_plans = result.test_plans;
            });
        });
      }
    };
    $scope.removeTestPlan = function (componentId, testPlanId) {
      ComponentTestPlan.delete(
        {
          fk_component: componentId,
          fk_test_plan: testPlanId
        }, function success() {
          Component.get({componentId: componentId},
            function success(result) {
              $scope.component = result.component;
              $scope.test_plans = result.test_plans;
            });
        });
    };
    // Display create component inline
    var display_create = false;
    $scope.createTestPlan = function() {
      if (!display_create) {
        $scope.displayBlock = 'display-block';
        $timeout(function(){
          $scope.createTestPlanClass = 'display-create';
        },100)
        display_create = true;
      } else {
        $scope.createTestPlanClass = '';
        $timeout(function(){
          $scope.displayBlock = '';
        },500);
        display_create = false;
      };
    };
    // Get more test plans
    $scope.moreTestPlans = function () {
      if ($scope.test_plans) {
        if ($scope.busy) return;
        $scope.busy = true;
        var after = $scope.test_plans[$scope.test_plans.length - 1].pk_test_plan;
        Component.get({componentId: $routeParams.componentId, after: after},
          function success(result) {
            if (result && result.test_plans) {
              var plans = result.test_plans;
              for (var i = 0; i < plans.length; i++) {
                $scope.test_plans.push(plans[i]);
              }
            }
            $scope.busy = false;
          });
      }
    };
  });

// Edit component
app.controller('ComponentEditCtrl',
  function ($scope, $routeParams, $location, Component) {
    $scope.heading = 'Update Component';
    $scope.submitLabel = 'Update Component';
    $scope.component = Component.get({componentId: $routeParams.componentId});
    $scope.submitFunction = function () {
      $scope.component = Component.save({
          componentId: $routeParams.componentId
        },
        $scope.component, function success() {
          $location.path('components');
        }, function fail() {
          console.log('Error: failed to update component.');
        });
    };
  });

// Delete component
app.controller('ComponentDeleteCtrl',
  function ($scope, $routeParams, $location, Component, Components, socket) {
    $scope.component = Component.delete({componentId: $routeParams.componentId},
      function success() {
        $location.path('components');
        socket.emit('get:stats');
      });
  });

// Managing the test plan list
app.controller('TestPlanListCtrl',
  function ($scope, $rootScope, $filter, $timeout, TestPlans, TestPlan, socket) {
    $scope.busy = false;
    // Get list of test plans
    TestPlans.query(
      function success(results) {
        $scope.test_plans = results;
      });
    $scope.getData = function (test_plans, query) {
      $scope.queryData = $filter('filter')(test_plans, query);
    };
    // Display create component inline
    var display_create = false;
    $scope.createTestPlan = function() {
      if (!display_create) {
        $scope.displayBlock = 'display-block';
        $timeout(function(){
          $scope.createTestPlanClass = 'display-create';
        },100)
        display_create = true;
      } else {
        $scope.createTestPlanClass = '';
        $timeout(function(){
          $scope.displayBlock = '';
        },500);
        display_create = false;
      };
    };
    // Delete a test plan by id
    $scope.deleteTestPlan = function (testPlanId) {
      $scope.component = TestPlan.delete({testPlanId: testPlanId},
        function success() {
          TestPlans.query(
            function success(results) {
              $scope.test_plans = results;
            });
          socket.emit('get:stats');
        });
    };

    // Get more test plans
    $scope.moreTestPlans = function () {
      if ($scope.test_plans) {
        if ($scope.busy) return;
        $scope.busy = true;
        var after = $scope.test_plans[$scope.test_plans.length - 1].pk_test_plan;
        TestPlans.query({ after: after },
          function success(results) {
            if (results[0]) $scope.test_plans.push(results[0]);
            $scope.busy = false;
          });
      }
    };
  });

// Creating a new test plan
app.controller('TestPlanNewCtrl',
  function ($scope, $location, TestPlans, socket) {
    $scope.heading = 'Create New Test Plan';
    $scope.submitLabel = 'Create Test Plan';
    $scope.test_plan = {
      name: '',
      description: ''
    };
    $scope.testPlanSubmit = function () {
      $scope.test_plan = TestPlans.save(
        $scope.test_plan, function success(result) {
          $location.path('test_plans/' + result.insertId);
          socket.emit('get:stats');
        }, function fail() {
          console.log('Error: failed to save test plan');
        });
    };
  });

// View test plan
app.controller('TestPlanViewCtrl',
  function ($scope, $routeParams, $timeout, TestPlan, Test, socket) {
    TestPlan.get({
      testPlanId: $routeParams.testPlanId
    }, function success(data) {
      $scope.test_plan = data.test_plan;
      $scope.tests = data.tests;
    });
    // Display create component inline
    var display_create = false;
    $scope.createTest = function() {
      if (!display_create) {
        $scope.displayBlock = 'display-block';
        $timeout(function(){
          $scope.createTestClass = 'display-create';
        },100)
        display_create = true;
      } else {
        $scope.createTestClass = '';
        $timeout(function(){
          $scope.displayBlock = '';
        },500);
        display_create = false;
      };
    };

    // Delete a test by id
    $scope.deleteTest = function (testPlanId, testId) {
      $scope.component = Test.delete({
          testPlanId: testPlanId,
          testId: testId
        },
        function success() {
          TestPlan.get({
            testPlanId: testPlanId
          }, function success(data) {
            $scope.tests = data.tests;
          });
          socket.emit('get:stats');
        });
    };

    // Get more tests
    $scope.moreTests = function () {
      if ($scope.tests) {
        if ($scope.busy) return;
        $scope.busy = true;
        var after = $scope.tests[$scope.tests.length - 1].pk_test;
        TestPlan.get({testPlanId: $routeParams.testPlanId, after: after},
          function success(result) {
            if (result && result.tests) {
              var tests = result.tests;
              for (var i = 0; i < tests.length; i++) {
                $scope.tests.push(tests[i]);
              }
            }
            $scope.busy = false;
          });
      }
    };

  });

// Edit test plan
app.controller('TestPlanEditCtrl',
  function ($scope, $routeParams, $location, TestPlan) {
    $scope.heading = 'Update Test Plan';
    $scope.submitLabel = 'Update Test Plan';
    $scope.test_plan = TestPlan.get({testPlanId: $routeParams.testPlanId});
    $scope.testPlanSubmit = function () {
      $scope.test_plan = TestPlan.save({
          testPlanId: $routeParams.testPlanId},
        $scope.test_plan, function success() {
          $location.path('test_plans');
        }, function fail() {
          console.log('failed to update test plan');
        });
    };
  });

// Delete test plan
app.controller('TestPlanDeleteCtrl',
  function ($scope, $routeParams, $location, TestPlan, socket) {
    $scope.component = TestPlan.delete({testPlanId: $routeParams.testPlanId},
      function success() {
        $location.path('test_plans');
        socket.emit('get:stats');
      });
  });

// Managing the test list by test plan
app.controller('TestListCtrl',
  function ($scope, $routeParams, $filter, $timeout, Tests, Test) {
    $scope.busy = false;
    $scope.test_plan = $routeParams.testPlanId;
    $scope.tests = Tests.query({testPlanId: $routeParams.testPlanId });
    $scope.getData = function (tests, query) {
      $scope.queryData = $filter('filter')(tests, query);
    };
    // Display create component inline
    var display_create = false;
    $scope.createTest = function() {
      if (!display_create) {
        $scope.displayBlock = 'display-block';
        $timeout(function(){
          $scope.createTestClass = 'display-create';
        },100)
        display_create = true;
      } else {
        $scope.createTestClass = '';
        $timeout(function(){
          $scope.displayBlock = '';
        },500);
        display_create = false;
      };
    };
    // Delete a test by id
    $scope.deleteTest = function (testPlanId, testId) {
      $scope.component = Test.delete({
          testPlanId: testPlanId,
          testId: testId
        },
        function success() {
          Tests.query({testPlanId: testPlanId},
            function success(results) {
              $scope.tests = results;

            });
          socket.emit('get:stats');
        });
    };
    // Get more tests
    $scope.moreTests = function () {
      if ($scope.tests) {
        if ($scope.busy) return;
        $scope.busy = true;
        var after = $scope.tests[$scope.tests.length - 1].pk_test;
        Tests.query({ after: after },
          function success(results) {
            if (results[0]) $scope.tests.push(results[0]);
            $scope.busy = false;
          });
      }
    };
  });

// Creating a new test plan
app.controller('TestNewCtrl',
  function ($scope, $routeParams, $location, Tests, socket) {
    $scope.heading = 'Create New Test';
    $scope.submitLabel = 'Create Test';
    $scope.test_plan = $routeParams.testPlanId;
    $scope.test = {
      fk_test_plan: $routeParams.testPlanId,
      name: '',
      description: ''
    };
    $scope.testSubmit = function () {
      $scope.test = Tests.save({
          testPlanId: $routeParams.testPlanId
        },
        $scope.test, function success() {
          $location.path('test_plans/' + $routeParams.testPlanId);
          socket.emit('get:stats');
        }, function fail() {
          console.log('Error: failed to save test.');
        });
    };
  });

// View test
app.controller('TestViewCtrl',
  function ($scope, $routeParams, Test) {
    $scope.test_plan = $routeParams.testPlanId;
    $scope.test = Test.get({
      testPlanId: $routeParams.testPlanId,
      testId: $routeParams.testId
    });
  });

// Edit test plan
app.controller('TestEditCtrl',
  function ($scope, $routeParams, $location, Test) {
    $scope.heading = 'Update Test';
    $scope.submitLabel = 'Update Test';
    $scope.test_plan = $routeParams.testPlanId;
    $scope.test = Test.get({
      testPlanId: $routeParams.testPlanId,
      testId: $routeParams.testId
    });
    $scope.testSubmit = function () {
      $scope.test = Test.save({
          testPlanId: $routeParams.testPlanId,
          testId: $routeParams.testId
        },
        $scope.test, function success() {
          $location.path('test_plans/' + $routeParams.testPlanId);
        }, function fail() {
          console.log('Error: failed to update test');
        });
    };
  });

// Delete test plan
app.controller('TestDeleteCtrl',
  function ($scope, $routeParams, $location, Test, socket) {
    $scope.component = Test.delete({
        testPlanId: $routeParams.testPlanId,
        testId: $routeParams.testId
      },
      function success() {
        $location.path('test_plans/' + $routeParams.testPlanId);
        socket.emit('get:stats');
      });
  });