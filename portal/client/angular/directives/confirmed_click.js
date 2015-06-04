// Modal confirmation click
angular.module('qa-portal.directives').
  directive('ngConfirmClick', [
    function () {
      return {
        link: function (scope, element, attr) {
          /** @namespace attr.ngConfirmClick */
          var msg = attr.ngConfirmClick || "Are you sure?";
          /** @namespace attr.confirmedClick */
          var clickAction = attr.confirmedClick;
          element.bind('click', function () {
            if (window.confirm(msg)) {
              scope.$eval(clickAction)
            }
          });
        }
      };
    }]);