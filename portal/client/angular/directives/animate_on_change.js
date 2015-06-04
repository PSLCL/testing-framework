// Angular directives
angular.module('qa-portal.directives', []).
  directive('animateOnChange', function ($animate) {
    return function (scope, elem, attr) {
      // Watch for changes to value and set new label accordingly
      /** @namespace attr.animateOnChange */
      scope.$watch(attr.animateOnChange, function (nv, ov) {
        if (nv != ov) {
          var c;
          if (nv > ov) {
            c = 'label-success';
          } else if (nv < ov) {
            c = 'label-danger';
          } else {
            c = 'label-default';
          }
          // Remove old label class if present
          if (elem.hasClass('label-danger')) {
            $animate.removeClass(elem, 'label-danger');
          }
          if (elem.hasClass('label-success')) {
            $animate.removeClass(elem, 'label-success');
          }
          if (elem.hasClass('label-default')) {
            $animate.removeClass(elem, 'label-default');
          }
          // Add new label class (animate from css)
          $animate.addClass(elem, c);
        }
      });
    }
  });