var app = angular.module('qa-portal');
var Sort = require('../util/sort');

//Managing the module list
app.controller('ModuleListCtrl',
    function ($scope, $rootScope, $filter, $timeout, Modules, Module, AuthenticUser, socket) {
  $scope.busy = false;
  $scope.user = AuthenticUser.get();
  
  // Get list of modules
  Modules.query(
      function success(results) {
        $scope.modules = results;
      });

  // Filter list of modules
  $scope.getData = function (modules, query) {
    $scope.query_filter = query;
    var params;
    if ($scope.query_filter) {
      params = {
          filter: $scope.query_filter,
          sort_by: $scope.sort_by || 'id'
      };
    } else {
      params = {
          sort_by: $scope.sort_by || 'id'
      };
    }
    Modules.query(params,
        function success(results) {
      $scope.modules = results;
    });
  };

  // Display create module inline
  var display_create = false;
  $scope.createModule = function() {
    if (!display_create) {
      $scope.displayBlock = 'display-block';
      $timeout(function(){
        $scope.createModuleClass = 'display-create';
      },100);
      display_create = true;
    } else {
      $scope.createModuleClass = '';
      $timeout(function(){
        $scope.displayBlock = '';
      },500);
      display_create = false;
    };
  };

  // Delete a module by id
  $scope.deleteModule = function (moduleId) {
    $scope.module = Module.delete({moduleId: moduleId},
        function success() {
      var params;
      if ($scope.query_filter) {
        params = {
            filter: $scope.query_filter,
            sort_by: $scope.sort_by || 'id'
        };
      } else {
        params = {
            sort_by: $scope.sort_by || 'id'
        };
      }
      Modules.query(params,
          function success(results) {
        $scope.modules = results;
      });
      socket.emit('get:stats');
    });
  };

  // Get more modules
  $scope.moreModules = function () {
    if ($scope.modules) {
      if ($scope.busy) return;
      $scope.busy = true;
      var after = $scope.modules[$scope.modules.length - 1].pk_module;
      var params;
      if ($scope.query_filter) {
        params = {
            filter: $scope.query_filter,
            sort_by: $scope.sort_by || 'id',
            after: after
        };
      } else {
        params = {
            sort_by: $scope.sort_by || 'id',
            after: after
        };
      }
      Modules.query(params,
          function success(results) {
        if (results[0]) $scope.modules.push(results[0]);
        $scope.busy = false;
      });
    };
  };

  /**
   * Setup sort by for modules
   * @type {Sort}
   */
  $scope.sort = new Sort(function (sort_by) {
    $scope.sort_by = sort_by;
    var params;
    if ($scope.query_filter) {
      params = {
          filter: $scope.query_filter,
          sort_by: sort_by || 'id'
      };
    } else {
      params = {
          sort_by: sort_by || 'id'
      };
    }
    Module.query(params,
        function success(results) {
      $scope.modules = results;
    });
  });
});

//Creating a new module
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

//View module
app.controller('ModuleViewCtrl',
    function ($scope, $routeParams, $timeout, $route, $location, Module, ModuleTestPlan) {
  $scope.busy = false;
  Module.get({moduleId: $routeParams.moduleId},
      function success(result) {
    $scope.module = result.module;
    $scope.test_plans = result.test_plans;
  });
  // Filter list of test plans
  $scope.getData = function (test_plans, query) {
    $scope.query_filter = query;
    var params;
    if ($scope.query_filter) {
      params = {
          moduleId: $routeParams.moduleId,
          filter: $scope.query_filter,
          sort_by: $scope.sort_by || 'id'
      }
    } else {
      params = {
          moduleId: $routeParams.moduleId,
          sort_by: $scope.sort_by || 'id'
      }
    }
    Module.get(params,
        function success(results) {
      $scope.test_plans = results.test_plans;
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

  // Add a test play to a module
  $scope.addTestPlan = function (moduleId, testPlanId, testPlanSaved) {
    if (testPlanSaved !== moduleId) {
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
    } else {
      ModuleTestPlan.delete(
          {
            fk_module: moduleId,
            fk_test_plan: testPlanId
          }, function success() {
            var params;
            if ($scope.query_filter) {
              params = {
                  moduleId: moduleId,
                  filter: $scope.query_filter,
                  sort_by: $scope.sort_by || 'id'
              }
            } else {
              params = {
                  moduleId: moduleId,
                  sort_by: $scope.sort_by || 'id'
              }
            }
            Module.get(params,
                function success(result) {
              $scope.module = result.module;
              $scope.test_plans = result.test_plans;
            });
          });
    }

  };

  // Remove a test plan from a module
  $scope.removeTestPlan = function (moduleId, testPlanId) {
    ModuleTestPlan.delete(
        {
          fk_module: moduleId,
          fk_test_plan: testPlanId
        }, function success() {
          var params;
          if ($scope.query_filter) {
            params = {
                moduleId: moduleId,
                filter: $scope.query_filter,
                sort_by: $scope.sort_by || 'id'
            }
          } else {
            params = {
                moduleId: moduleId,
                sort_by: $scope.sort_by || 'id'
            }
          }
          Module.get(params,
              function success(result) {
            $scope.module = result.module;
            $scope.test_plans = result.test_plans;
          });
        });
  };

  $scope.enableEdit = function() {
    $scope.edit_module = !$scope.edit_module
  }

  // Edit test in view
  $scope.heading = 'Update Module';
  $scope.submitLabel = 'Update Module';
  Module.get({moduleId: $routeParams.moduleId},
      function success(result) {
    $scope.module = result.module;
  });
  $scope.submitFunction = function () {
    $scope.module = Module.save({
      moduleId: $routeParams.moduleId
    },
    $scope.module, function success() {
      $route.reload();
      // $location.path('modules');
    }, function fail() {
      console.log('Error: failed to update module.');
    });
  };

  // Check if module has a specific test plan already
  $scope.hasTestPlan = function (module, test_plan) {
    return (module.pk_module != test_plan.fk_module);
  };

  // Get more test plans
  $scope.moreTestPlans = function () {
    if ($scope.test_plans) {
      if ($scope.busy) return;
      $scope.busy = true;
      var after = $scope.test_plans[$scope.test_plans.length - 1].pk_test_plan;
      var params;
      if ($scope.query_filter) {
        params = {
            moduleId: $routeParams.moduleId,
            filter: $scope.query_filter,
            sort_by: $scope.sort_by || 'id',
            after: after
        }
      } else {
        params = {
            moduleId: $routeParams.moduleId,
            sort_by: $scope.sort_by || 'id',
            after: after
        }
      }
      Module.get(params,
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

  /**
   * Setup sort by for test plans
   * @type {Sort}
   */
  $scope.sort = new Sort(function (sort_by) {
    $scope.sort_by = sort_by;
    var params;
    if ($scope.query_filter) {
      params = {
          moduleId: $routeParams.moduleId,
          filter: $scope.query_filter,
          sort_by: sort_by || 'id'
      };
    } else {
      params = {
          moduleId: $routeParams.moduleId,
          sort_by: sort_by || 'id'
      };
    }
    Module.get(params,
        function success(result) {
      $scope.test_plans = result.test_plans;
      $scope.busy = false;
    });
  });
});

//Edit module
app.controller('ModuleEditCtrl',
    function ($scope, $routeParams, $location, Module) {
  $scope.heading = 'Update Module';
  $scope.submitLabel = 'Update Module';
  Module.get({moduleId: $routeParams.moduleId},
      function success(result) {
    $scope.module = result.module;
  });
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

//Delete module
app.controller('ModuleDeleteCtrl',
    function ($scope, $routeParams, $location, Module, Modules, socket) {
  $scope.module = Module.delete({moduleId: $routeParams.moduleId},
      function success() {
    $location.path('modules');
    socket.emit('get:stats');
  });
});