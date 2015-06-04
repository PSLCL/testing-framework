var app = angular.module('qa-portal');

// Report test plans
app.factory('ReportTestPlans', function ($resource) {
  return $resource('/api/v1/report_test_plans', {}, {
    query: {method:'GET', params:{filter:'@string'}, isArray:true}
  });
});

// Report versions
app.factory('ReportVersions', function ($resource) {
  return $resource('/api/v1/report_versions', {}, {
    query: {method:'GET', params:{select:'@string',incl_passed:'@boolean',incl_failed:'@boolean',incl_pending:'@boolean'}, isArray:false}
  });
});


app.factory('ReportResult', function ($resource) {
  return $resource('/api/v1/report_result', {}, {
    report: {method:'POST', params:{ hash:'@string'}}
  });
});