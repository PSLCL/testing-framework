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
var config   = require('./config/config');
var fs       = require('fs');
var os       = require('os');
var CronJob  = require('cron').CronJob;
var spawn    = require('child_process').spawn;
var path     = require('path');

var env = process.env.NODE_ENV || 'production';

if ( env == 'production' ) {
  if ( os.platform() != 'win32' ) {
    require('rconsole');
    console.set({ facility: 'local0', title: 'basic', stdout: false, stderr: false })
  }
}

var job = new CronJob(config.synchronize_schedule, function() {
    var child = spawn('java',
            ['-cp', path.join('platform','*') + path.delimiter + path.join('platform','lib','*'), 'com.pslcl.qa.platform.CommandLine', 'synchronize' ],
            { cwd: path.join(__dirname,'..') });
    child.stdout.on('data', function(data) { console.log('synchronize: '+data); });
    child.stderr.on('data', function(data) { console.log('synchronize (error): '+data); });
    child.on('close', function(code) { console.log("synchronize complete with exit code " + code) });
    }, function() {
    },
    true
);

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
  console.log('Server listening on ' + config.listen_ip + ':' + config.http_port);
});

if ( secureServer ) {
    secureServer.listen( config.https_port, function(){
        console.log('Secure server listening on ' + config.listen_ip + ':' + config.https_port);
    });
}
