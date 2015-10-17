var app = angular.module( 'qa-portal' );

// Managing the module list
app.controller( 'InstanceViewCtrl',
                function( $scope, $rootScope, $filter, $timeout, $routeParams, $sce, Instance, Template,
                          socket ) {
                  $scope.busy = false;
                  
                  $scope.instance = Instance.get( { id: $routeParams.id } );
                  $scope.instance.$promise.then( function ( instance ) {
                    $scope.lines = Template.query( { id: instance.fk_described_template } );
                    $scope.lines.$promise.then( function( lines ) {
                      console.log(lines);
                      $scope.descriptions = lines.map( function(line) { console.log( line.description ); return $sce.trustAsHtml( line.description ); } );
                      console.log($scope.descriptions);
                    });
                  });
});
