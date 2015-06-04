/* Main entry point for the qaportal application.
 * The following sets up an express application that supports both secure and unsecure
 * servers. Development environment does not require security, Production does and
 * redirects unsecure to secure. Production systems also redirect logging to the
 * console.
 */

//Dependencies
var express  = require('express');
var http     = require('http');
var https    = require('https');
var passport = require('passport');
var env      = process.env.NODE_ENV || 'development';
var config   = require('./server/config/config')[env];
var fs       = require('fs');
var os       = require('os');

if ( env == 'production' ) {
  if ( os.platform() != 'win32' ) {
    require('rconsole');
    console.set({ facility: 'local0', title: 'basic', stdout: false, stderr: false })
  }
}

var options = {
    pfx: fs.readFileSync('server.pfx'),
    passphrase: 'qaexport',
    requestCert: true
};

//Setup server and socket io
var app = express();
var forwarder = express();

var unsecureServer;
if ( env == 'production' ) {
  unsecureServer = http.createServer(forwarder);
}
else {
  unsecureServer = http.createServer(app);
}

var secureServer = https.createServer(options, app);

var sio = require('socket.io').listen(secureServer);
sio.set('log level', 1);

//Init Express configuration
require('./server/config/express')(app, config, passport);

//Init Routing
require('./server/config/routes')(app, config, passport);

//Socket IO routes
sio.sockets.on('connection', require('./server/routes/socket'));

forwarder.get('*',function(req,res) {
  res.redirect('https://' + req.headers.host + req.url)
});

unsecureServer.listen(app.get('http_port'), function(){
  console.log('Server listening on port ' + app.get('http_port'));
});

secureServer.listen( app.get('https_port'), function(){
  console.log('Secure servier listening on port ' + app.get('https_port'));
});
