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

// Managing the module list
app.controller('ModuleListCtrl',
  function ($scope, $rootScope, $filter, $timeout, Modules, Module, socket) {
    $scope.busy = false;
    // Get list of modules
    Modules.query(
      function success(results) {
        $scope.modules = results;
      });

    // Filter list of modules
    $scope.getData = function (modules, query) {
      $scope.queryData = $filter('filter')(modules, query);
    };

    // Delete a module by id
    $scope.deleteModule = function (moduleId) {
      $scope.module = Module.delete({moduleId: moduleId},
        function success() {
          Modules.query(
            function success(results) {
              $scope.modules = results;
            });
          socket.emit('get:stats');
        });
    };

    // Display create module inline
    var display_create = false;
    $scope.createModule = function() {
      if (!display_create) {
        $scope.displayBlock = 'display-block';
        $timeout(function(){
          $scope.createModuleClass = 'display-create';
        },100)
        display_create = true;
      } else {
        $scope.createModuleClass = '';
        $timeout(function(){
          $scope.displayBlock = '';
        },500);
        display_create = false;
      };
    };

    // Get more modules
    $scope.moreModules = function () {
      if ($scope.modules) {
        if ($scope.busy) return;
        $scope.busy = true;
        var after = $scope.modules[$scope.modules.length - 1].pk_module;
        Modules.query({ after: after },
          function success(results) {
            if (results[0]) $scope.modules.push(results[0]);
            $scope.busy = false;
          });
      }
    };
  });

// Creating a new module
app.controller('ModuleNewCtrl',
  function ($scope, $location, Modules, socket) {
    $scope.heading = 'Create new Module';
    $scope.submitLabel = 'Create Module';
    $scope.module = {
      name: ''
    };
    $scope.submitFunction = function () {
      $scope.module = Modules.save(
        $scope.module, function success(result) {
          socket.emit('get:stats');
          $location.path('modules/' + result.insertId);
        }, function fail() {
          console.log('Error: failed to save module.');
        });
    };
  });

// View module
app.controller('ModuleViewCtrl',
  function ($scope, $routeParams, $timeout, Module, ModuleTestPlan) {
    $scope.busy = false;
    Module.get({moduleId: $routeParams.moduleId},
      function success(result) {
        $scope.module = result.module;
        $scope.test_plans = result.test_plans;
      });
    $scope.addTestPlan = function (moduleId, testPlanId, testPlanSelected) {
      if(moduleId === testPlanSelected){
        ModuleTestPlan.delete(
        {
          fk_module: moduleId,
          fk_test_plan: testPlanId
        }, function success() {
          Module.get({moduleId: moduleId},
            function success(result) {
              $scope.module = result.module;
              $scope.test_plans = result.test_plans;
            });
        });
      } else {
        ModuleTestPlan.save(
        {
          fk_module: moduleId,
          fk_test_plan: testPlanId
        }, function success() {
          Module.get({moduleId: moduleId},
            function success(result) {
              $scope.module = result.module;
              $scope.test_plans = result.test_plans;
            });
        });
      }
    };
    $scope.removeTestPlan = function (moduleId, testPlanId) {
      ModuleTestPlan.delete(
        {
          fk_module: moduleId,
          fk_test_plan: testPlanId
        }, function success() {
          Module.get({moduleId: moduleId},
            function success(result) {
              $scope.module = result.module;
              $scope.test_plans = result.test_plans;
            });
        });
    };
    // Display create module inline
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
        Module.get({moduleId: $routeParams.moduleId, after: after},
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

// Edit module
app.controller('ModuleEditCtrl',
  function ($scope, $routeParams, $location, Module) {
    $scope.heading = 'Update Module';
    $scope.submitLabel = 'Update Module';
    $scope.module = Module.get({moduleId: $routeParams.moduleId});
    $scope.submitFunction = function () {
      $scope.module = Module.save({
          moduleId: $routeParams.moduleId
        },
        $scope.module, function success() {
          $location.path('modules');
        }, function fail() {
          console.log('Error: failed to update module.');
        });
    };
  });

// Delete module
app.controller('ModuleDeleteCtrl',
  function ($scope, $routeParams, $location, Module, Modules, socket) {
    $scope.module = Module.delete({moduleId: $routeParams.moduleId},
      function success() {
        $location.path('modules');
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
    // Display create module inline
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
      $scope.module = TestPlan.delete({testPlanId: testPlanId},
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
    // Display create module inline
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
      $scope.module = Test.delete({
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
    $scope.module = TestPlan.delete({testPlanId: $routeParams.testPlanId},
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
    // Display create module inline
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
      $scope.module = Test.delete({
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
    $scope.module = Test.delete({
        testPlanId: $routeParams.testPlanId,
        testId: $routeParams.testId
      },
      function success() {
        $location.path('test_plans/' + $routeParams.testPlanId);
        socket.emit('get:stats');
      });
  });