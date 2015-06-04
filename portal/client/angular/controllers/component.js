var app = angular.module('qa-portal');
var Sort = require('../util/sort');

//Managing the component list
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
    Components.query(params,
        function success(results) {
      $scope.components = results;
    });
  };

  // Display create component inline
  var display_create = false;
  $scope.createComponent = function() {
    if (!display_create) {
      $scope.displayBlock = 'display-block';
      $timeout(function(){
        $scope.createComponentClass = 'display-create';
      },100);
      display_create = true;
    } else {
      $scope.createComponentClass = '';
      $timeout(function(){
        $scope.displayBlock = '';
      },500);
      display_create = false;
    };
  };

  // Delete a component by id
  $scope.deleteComponent = function (componentId) {
    $scope.component = Component.delete({componentId: componentId},
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
      Components.query(params,
          function success(results) {
        $scope.components = results;
      });
      socket.emit('get:stats');
    });
  };

  // Get more components
  $scope.moreComponents = function () {
    if ($scope.components) {
      if ($scope.busy) return;
      $scope.busy = true;
      var after = $scope.components[$scope.components.length - 1].pk_component;
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
      Components.query(params,
          function success(results) {
        if (results[0]) $scope.components.push(results[0]);
        $scope.busy = false;
      });
    };
  };

  /**
   * Setup sort by for components
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
    Component.query(params,
        function success(results) {
      $scope.components = results;
    });
  });
});

//Creating a new component
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

//View component
app.controller('ComponentViewCtrl',
    function ($scope, $routeParams, $timeout, $route, $location, Component, ComponentTestPlan) {
  $scope.busy = false;
  Component.get({componentId: $routeParams.componentId},
      function success(result) {
    $scope.component = result.component;
    $scope.test_plans = result.test_plans;
  });
  // Filter list of test plans
  $scope.getData = function (test_plans, query) {
    $scope.query_filter = query;
    var params;
    if ($scope.query_filter) {
      params = {
          componentId: $routeParams.componentId,
          filter: $scope.query_filter,
          sort_by: $scope.sort_by || 'id'
      }
    } else {
      params = {
          componentId: $routeParams.componentId,
          sort_by: $scope.sort_by || 'id'
      }
    }
    Component.get(params,
        function success(results) {
      $scope.test_plans = results.test_plans;
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

  // Add a test play to a component
  $scope.addTestPlan = function (componentId, testPlanId, testPlanSaved) {
    if (testPlanSaved !== componentId) {
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
    } else {
      ComponentTestPlan.delete(
          {
            fk_component: componentId,
            fk_test_plan: testPlanId
          }, function success() {
            var params;
            if ($scope.query_filter) {
              params = {
                  componentId: componentId,
                  filter: $scope.query_filter,
                  sort_by: $scope.sort_by || 'id'
              }
            } else {
              params = {
                  componentId: componentId,
                  sort_by: $scope.sort_by || 'id'
              }
            }
            Component.get(params,
                function success(result) {
              $scope.component = result.component;
              $scope.test_plans = result.test_plans;
            });
          });
    }

  };

  // Remove a test plan from a component
  $scope.removeTestPlan = function (componentId, testPlanId) {
    ComponentTestPlan.delete(
        {
          fk_component: componentId,
          fk_test_plan: testPlanId
        }, function success() {
          var params;
          if ($scope.query_filter) {
            params = {
                componentId: componentId,
                filter: $scope.query_filter,
                sort_by: $scope.sort_by || 'id'
            }
          } else {
            params = {
                componentId: componentId,
                sort_by: $scope.sort_by || 'id'
            }
          }
          Component.get(params,
              function success(result) {
            $scope.component = result.component;
            $scope.test_plans = result.test_plans;
          });
        });
  };

  $scope.enableEdit = function() {
    $scope.edit_component = !$scope.edit_component
  }

  // Edit test in view
  $scope.heading = 'Update Component';
  $scope.submitLabel = 'Update Component';
  Component.get({componentId: $routeParams.componentId},
      function success(result) {
    $scope.component = result.component;
  });
  $scope.submitFunction = function () {
    $scope.component = Component.save({
      componentId: $routeParams.componentId
    },
    $scope.component, function success() {
      $route.reload();
      // $location.path('components');
    }, function fail() {
      console.log('Error: failed to update component.');
    });
  };

  // Check if component has a specific test plan already
  $scope.hasTestPlan = function (component, test_plan) {
    return (component.pk_component != test_plan.fk_component);
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
            componentId: $routeParams.componentId,
            filter: $scope.query_filter,
            sort_by: $scope.sort_by || 'id',
            after: after
        }
      } else {
        params = {
            componentId: $routeParams.componentId,
            sort_by: $scope.sort_by || 'id',
            after: after
        }
      }
      Component.get(params,
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
          componentId: $routeParams.componentId,
          filter: $scope.query_filter,
          sort_by: sort_by || 'id'
      };
    } else {
      params = {
          componentId: $routeParams.componentId,
          sort_by: sort_by || 'id'
      };
    }
    Component.get(params,
        function success(result) {
      $scope.test_plans = result.test_plans;
      $scope.busy = false;
    });
  });
});

//Edit component
app.controller('ComponentEditCtrl',
    function ($scope, $routeParams, $location, Component) {
  $scope.heading = 'Update Component';
  $scope.submitLabel = 'Update Component';
  Component.get({componentId: $routeParams.componentId},
      function success(result) {
    $scope.component = result.component;
  });
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

//Delete component
app.controller('ComponentDeleteCtrl',
    function ($scope, $routeParams, $location, Component, Components, socket) {
  $scope.component = Component.delete({componentId: $routeParams.componentId},
      function success() {
    $location.path('components');
    socket.emit('get:stats');
  });
});