// Angular directives
angular.module('qa-portal.directives')
.directive('accordianWrap', function() {
  return {
    controller: function($scope) {
      this.setHeight = function($index) {
        $scope.open_index = $index;
      }

      this.getIndex = function() {
        return $scope.open_index;
      }

    }
  }
})
.directive('accordian', function ($window, $timeout) {

  return {
    require: '^accordianWrap',
    link: function(scope,element,attrs,accordianCtrl) {

      var open = false;
      scope.showInfo = function($index) {
        var prev = accordianCtrl.getIndex();
        var closed_height = element.children()[0].clientHeight;
        var open_height = element.children()[1].clientHeight + closed_height + 12;
        scope.open_style = open_height + 'px';
        scope.closed_style = closed_height + 'px';
        scope.display_info = 'display-info';
        if (prev == $index) {
          if (open) {
            accordianCtrl.setHeight($index);
            scope.open_style = open_height + 'px';
            scope.display_info = 'display-info';
          } else {
            accordianCtrl.setHeight($index);
            scope.open_style = closed_height + 'px';
            scope.display_info = '';
          }
          open = !open
        } else {
          accordianCtrl.setHeight($index, open_height);
          scope.open_style = open_height + 'px';
          scope.display_info = 'display-info';
        }
      }

      var setHeight = function() {
        var closed_height = element.children()[0].clientHeight;
        var open_height = element.children()[1].clientHeight + closed_height + 12;
        scope.closed_style = closed_height + 'px';
        scope.open_style = open_height + 'px';
      }

      angular.element(document).ready(function () {
        $timeout(function(){
          setHeight();
        },0);
      });
      angular.element($window).bind('resize', function() {
        setHeight();
      });

    }
  }
});