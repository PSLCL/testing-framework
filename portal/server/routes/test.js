// Mysql Connection
var mysql   = require('../lib/mysql');
var config = require('../../config/config');

// [GET] list of tests by test plan
exports.list = function (req, res) {
  mysql.getConnection(function(err,conn) {
    conn.query('SELECT pk_test,name,fk_test_plan FROM test ' +
      'WHERE fk_test_plan = ?', req.param('pk_test_plan'),
      function (err, result) {
        if (err) throw err;
            if (err) throw err;
            res.format({
              'text/html': function () {
                res.send(result);
              },
              'application/json': function () {
                res.send(result);
              }
            });
      }
    );
    conn.release();
  });
};

// [GET] individual test
exports.show = function (req, res) {
  mysql.getConnection(function(err,conn) {
    conn.query(
      'SELECT pk_test,name,description,script FROM test ' +
        'WHERE pk_test = ?', req.param('id'),
      function (err, result) {
        if (err) throw err;
        res.format({
          'text/html': function () {
            res.send(result[0]);
          },
          'application/json': function () {
            res.send(result[0]);
          }
        });
      }
    );
    conn.release();
  });
};

// [POST] new test
exports.create = function (req, res) {
  mysql.getConnection(function(err,conn) {
    conn.query('INSERT INTO test SET ?', req.body,
      function (err, result) {
        if (err) throw err;
        res.format({
          'text/html': function () {
            res.send(result);
          },
          'application/json': function () {
            res.send(result);
          }
        });
      }
    );
    conn.release();
  });
};

// [POST] update test by pk_test
exports.update = function (req, res) {
  var name = req.body['name'] || "";
  var description = req.body['description'] || "";
  var script = req.body['script'] || "";
  mysql.getConnection(function(err,conn) {
    conn.query(
      'UPDATE test SET name = ?, description = ?, script = ?' +
        'WHERE pk_test = ? ',
      [name, description, script, req.body['pk_test']],
      function (err,result) {
        if (err) throw err;
        res.format({
          'text/html': function () {
            res.send(result);
          },
          'application/json': function () {
            res.send(result);
          }
        });
      }
    );
    conn.release();
  });
};

// [DELETE] test by pk_test
exports.destroy = function (req, res) {
  mysql.getConnection(function(err,conn) {
    conn.query('DELETE FROM test WHERE pk_test = ?', req.param('id'),
      function (err, result) {
        if (err) throw err;
        res.format({
          'text/html': function () {
            res.send(result);
          },
          'application/json': function () {
            res.send(result);
          }
        });
      }
    );
    conn.release();
  });
};

exports.report_result = function (req, res) {
  mysql.getConnection(function(err,conn) {
    conn.query('CALL add_run(\'' + req.param('hash') + '\',' + req.param('result')+')',
      function (err, result) {
        if (err) throw err;

        res.format({
          'text/html': function () {
            res.send();
          },
          'application/json': function () {
            res.send();
          }
        });
      }
    );
    conn.release();
  });
};
