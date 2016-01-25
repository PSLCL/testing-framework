var mysql   = require('../lib/mysql');
var config = require('../../config/config');
var fs = require('fs');

// [GET] get a content file
exports.file = function (req, res) {
  var contentid = req.param('contentid');
  var file;
  
  try{
    file = fs.statSync(config.artifacts_dir + '/' + contentid);
  } catch(err) {
    return res.status(404).send(contentid + " Not Found");
  }

  res.setHeader('Content-disposition', 'attachment; filename=' + contentid);
  res.setHeader('Content-Type', 'application/octet-stream');

  var filestream = fs.createReadStream(config.artifacts_dir + '/' + contentid);
  filestream.pipe( res );
}
