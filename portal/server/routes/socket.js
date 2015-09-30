// Mysql Connection
var mysql = require('../lib/mysql');

// socket io route
module.exports = function (socket) {

  // TODO: implement initialization info
  socket.emit('init', {
  });

  // broadcast stats updates
  socket.on('get:stats', function () {
    mysql.getConnection(function(err,conn) {
      conn.query('SELECT' +
        '(SELECT COUNT(*) FROM module) as module_count,' +
        '(SELECT COUNT(*) FROM test_plan) as test_plan_count,' +
        '(SELECT COUNT(*) FROM test) as test_count',
        function (err, result) {
          if (err) throw err;
          socket.broadcast.emit('get:stats', result[0]);
        }
      );
      conn.release();
    });
  });
};