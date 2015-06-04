// Angular directives
angular.module('qa-portal.directives').
  directive('dropdownBox', function () {
    return {
      link : function (scope, elem, attr) {
        scope.selected_filter = 'Sort...';
        var sort_open = false;
        scope.openSort = function() {
          var height = elem.children()[0].clientHeight;
          if (!sort_open) {
            elem.attr('style','height:' + height + 'px');
            sort_open = !sort_open;
          } else {
            elem.attr('style','height:30px');
            sort_open = !sort_open;
          }
          
        };

        scope.changeFilter = function(name) {
          scope.selected_filter = name;
        }
      }
    }
  });