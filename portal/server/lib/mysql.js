//Module dependencies
var config   = require('../../config/config');
var mysql  = require('mysql');

var pool = mysql.createPool({
  host: config.mysql.host,
  port: config.mysql.port,
  user: config.mysql.user,
  password: config.mysql.password,
  database: config.mysql.db,
  multipleStatements: true
});

exports.getConnection = function(callback) {
  pool.getConnection(function(err, connection) {
    callback(err, connection);
  });
}
