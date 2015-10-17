var app = angular.module( 'qa-portal' );
var Sort = require( '../util/sort' );

// Managing the module list
app.controller( 'ModuleListCtrl',
                function( $scope, $rootScope, $filter, $timeout, Modules,
                          Module, socket ) {
                  $scope.busy = false;

                  // Get list of modules
                  Modules.query( function success( results ) {
                    $scope.modules = results;
                  } );

                  // Filter list of modules
                  $scope.getData = function( modules, filter ) {
                    $scope.filter = filter;
                    var params;
                    if ( $scope.filter ) {
                      params = {
                        'filter': $scope.filter,
                        'order': $scope.order
                      };
                    }
                    else {
                      params = {
                        'order': $scope.order
                      };
                    }
                    Modules.query( params, function success( results ) {
                      $scope.modules = results;
                    } );
                  };

                  // Get more modules
                  $scope.moreModules = function() {
                    if ( $scope.modules ) {
                      if ( $scope.busy )
                        return;
                      $scope.busy = true;
                      var offset = $scope.modules.length;
                      var params;
                      if ( $scope.filter ) {
                        params = {
                          filter: $scope.filter,
                          'order': $scope.sort_by,
                          'offset': offset
                        };
                      }
                      else {
                        params = {
                          'order': $scope.sort_by,
                          'offset': offset
                        };
                      }

                      Modules.query( params, function success( results ) {
                        if ( results )
                          $scope.modules = $scope.modules.concat( results );
                        $scope.busy = false;
                      } );
                    }
                    ;
                  };

                  /**
                   * Setup sort by for modules
                   * 
                   * @type {Sort}
                   */
                  $scope.sort = new Sort( function( order ) {
                    $scope.order = order;
                    var params;
                    if ( $scope.order ) {
                      params = {
                        filter: $scope.filter,
                        'order': order
                      };
                    }
                    else {
                      params = {
                        'order': order
                      };
                    }
                    Module.query( params, function success( results ) {
                      $scope.modules = results;
                    } );
                  } );
                } );

// View module
app
        .controller(
                     'ModuleViewCtrl',
                     function( $scope, $routeParams, $timeout, $route,
                               $location, Module, Artifacts, ModuleTestPlan ) {
                       $scope.busy = false;
                       Module.get( {
                         moduleId: $routeParams.moduleId
                       }, function success( result ) {
                         $scope.module = result.module;
                       } );
                       
                       Artifacts.query( {
                         moduleId: $routeParams.moduleId
                       }, function success( result ) {
                         $scope.artifacts = result;
                       })
                       
                      // Filter list of modules
                      $scope.getData = function( artifacts, filter ) {
                        $scope.filter = filter;
                        var params;
                        if ( $scope.filter ) {
                          params = {
                            'filter': $scope.filter,
                            'order': $scope.order
                          };
                        }
                        else {
                          params = {
                            'order': $scope.order
                          };
                        }
                        
                        params.moduleId = $routeParams.moduleId;
                        
                        Artifacts.query( params, function success( results ) {
                          $scope.artifacts = results;
                        } );
                      };

                       // Get more test plans
                       $scope.moreTestPlans = function() {
                         if ( $scope.test_plans ) {
                           if ( $scope.busy )
                             return;
                           $scope.busy = true;
                           var after = $scope.test_plans[ $scope.test_plans.length - 1 ].pk_test_plan;
                           var params;
                           if ( $scope.query_filter ) {
                             params = {
                               moduleId: $routeParams.moduleId,
                               filter: $scope.query_filter,
                               sort_by: $scope.sort_by || 'id',
                               after: after
                             }
                           }
                           else {
                             params = {
                               moduleId: $routeParams.moduleId,
                               sort_by: $scope.sort_by || 'id',
                               after: after
                             }
                           }
                           Module.get( params, function success( result ) {
                             if ( result && result.test_plans ) {
                               var plans = result.test_plans;
                               for ( var i = 0; i < plans.length; i++ ) {
                                 $scope.test_plans.push( plans[ i ] );
                               }
                             }
                             $scope.busy = false;
                           } );
                         }
                       };

                       /**
                        * Setup sort by for modules
                        * 
                        * @type {Sort}
                        */
                       $scope.sort = new Sort( function( order ) {
                         $scope.order = order;
                         var params;
                         if ( $scope.order ) {
                           params = {
                             filter: $scope.filter,
                             'order': order
                           };
                         }
                         else {
                           params = {
                             'order': order
                           };
                         }

                         params.moduleId = $routeParams.moduleId;

                         Artifacts.query( params, function success( results ) {
                           $scope.artifacts = results;
                         } );
                       } );
                     } );


// Report on module
app.controller( 'ModuleReportCtrl',
                function( $window, $scope, $routeParams, $location, Module,
                          ModuleReport ) {
                  $scope.exportReport = function() {
                    $window.open( '/api/v1/modules/' + $scope.module.pk_module
                                  + '/report_print' );
                  };

                  Module.get( {
                    moduleId: $routeParams.moduleId
                  }, function success( result ) {
                    $scope.module = result.module;
                  } );

                  ModuleReport.get( {
                    moduleId: $routeParams.moduleId
                  }, function success( results ) {
                    $scope.result = results;
                  } );
                } );
