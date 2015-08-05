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
var env      = process.env.NODE_ENV || 'production';
var config   = require('./config/config')[env];
var fs       = require('fs');
var os       = require('os');

if ( env == 'production' ) {
  if ( os.platform() != 'win32' ) {
    require('rconsole');
    console.set({ facility: 'local0', title: 'basic', stdout: false, stderr: false })
  }
}

var options = {};
if ( fs.existsSync( './config/server.pfx' ) ) {
    var server_pfx = fs.readFileSync('config/server.pfx');

    options = {
        pfx: server_pfx,
        passphrase: config.certificate_passphrase,
        requestCert: true
    };
};

//Setup server and socket io
var app = express();
var forwarder = express();

var unsecureServer;
var secureServer;
var socketServer;
if ( config.https_port ) {
  unsecureServer = http.createServer(forwarder);
  secureServer = https.createServer(options, app);
  socketServer = secureServer;
}
else {
  unsecureServer = http.createServer(app);
  socketServer = unsecureServer;
}

var sio = require('socket.io').listen(socketServer);

//Init Express configuration
require('./server/config/express')(app, config, passport);

//Init Routing
require('./server/config/routes')(app, config, passport);

//Socket IO routes
sio.sockets.on('connection', require('./server/routes/socket'));

forwarder.get('*',function(req,res) {
  res.redirect('https://' + req.headers.host + req.url)
});

unsecureServer.listen(config.http_port, function(){
  console.log('Server listening on port ' + config.http_port);
});

if ( secureServer ) {
    secureServer.listen( config.https_port, function(){
        console.log('Secure server listening on port ' + config.https_port);
    });
}
