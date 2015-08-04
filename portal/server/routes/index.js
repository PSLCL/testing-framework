//Mysql Connection
var mysql = require('../lib/mysql');

//Select database

exports.index = function (req, res) {
  mysql.getConnection(function(err,conn) {
    conn.query('SELECT' +
        '(SELECT COUNT(*) FROM module) as module_count,' +
        '(SELECT COUNT(*) FROM test_plan) as test_plan_count,' +
        '(SELECT COUNT(*) FROM test) as test_count,' +
        '(SELECT COUNT(*) FROM content) as artifact_count,' +
        '(SELECT COUNT(*) FROM test_instance) as ti_count',
        function (err, result) {
      if (err) { console.log(err); throw err; }

      res.format({
        'text/html': function () {
          res.render('index', { title: 'QA Portal', totals: result[0] });
			console.log("Rendering");
			console.log(JSON.stringify(req.user));
        },
        'application/json': function () {
          res.send(result[0]);
        }
      })
    }
    );
    conn.release();
  });
};

exports.admin_index = function (req, res) {
  mysql.getConnection(function(err,conn) {
    conn.query('SELECT' +
        '(SELECT COUNT(*) FROM module) as module_count,' +
        '(SELECT COUNT(*) FROM test_plan) as test_plan_count,' +
        '(SELECT COUNT(*) FROM test) as test_count,' +
        '(SELECT COUNT(*) FROM content) as artifact_count,' +
        '(SELECT COUNT(*) FROM test_instance) as ti_count',
        function (err, result) {
      if (err) throw err;
      res.format({
        'text/html': function () {
          res.render('index', { title: 'QA Portal', totals: result[0] });
        },
        'application/json': function () {
          res.send(result[0]);
        }
      })
    }
    );
    conn.release();
  });
};