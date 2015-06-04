// Mysql Connection
var mysql   = require('../lib/mysql');
var env    = process.env.NODE_ENV || 'development';
var config = require('../config/config')[env];
var tar = require('tar-stream');
var zlib = require('zlib');
var fs = require('fs');

// [GET] get a single artifact
exports.single = function (req, res) {
  var artifactid = req.param('artifactid');
  console.log('In the call');
  
  mysql.getConnection(function(err,conn) {
    console.log('Got the connection with artifactid=' + artifactid);
    conn.query('SELECT name, hex(fk_content) as filename FROM artifact WHERE pk_artifact = ?', artifactid,
    function (err, result) {
      console.log(result);
      if (err) throw err;

      var pack = tar.pack(); // p is a streams2 stream
      var file = fs.statSync('Z:/' + result[0].filename); // /opt/enc-test-platform/artifactcache/
      file.name = result[0].name;
      console.log(file);
      var entry = pack.entry(file);
      
      var filestream = fs.createReadStream('Z:/' + result[0].filename);
      filestream.pipe(entry);
      
      pack.finalize();

      res.setHeader('Content-disposition', 'attachment; filename=artifact-'+artifactid+'.tar.gz');
      res.setHeader('Content-Type', 'application/x-gzip');

      var gzip = zlib.createGzip();
      pack.pipe(gzip).pipe( res );
    });
    conn.release();
  });
}

// [GET] get multiple artifacts
exports.multiple = function (req, res) {
  var instanceid = req.param('instanceid');
  var pack = tar.pack() // p is a streams2 stream

  pack.entry({ name: 'my-test.txt' }, 'Hello World!');
  pack.finalize();

  res.setHeader('Content-disposition', 'attachment; filename=test.tar.gz');
  res.setHeader('Content-Type', 'application/x-gzip');
  
  var gzip = zlib.createGzip();
  pack.pipe(gzip).pipe( res );
};
