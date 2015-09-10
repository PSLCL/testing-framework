var favicon = require('static-favicon');
var logger  = require('morgan');
var express = require('express');
var path    = require('path');
var compression = require('compression');
var config   = require('../../config/config');
var serveIndex = require('serve-index');
var stylus   = require('stylus');

var directoryList = function(locals, callback) {
    var result = "<html>";
    locals.fileList.forEach( function(file) {
        if ( file.stat.isDirectory() ) {
            result += '<a href="' + locals.directory + '/' + file.name + '">' + file.name + '/</a><br/>';
        }
        else {
            result += '<a href="' + locals.directory + '/' + file.name + '">' + file.name + '</a><br/>';
        }
    });
    
    result += "</html>";
    callback( null, result );
};

module.exports = function (app, config, passport) {
  // all environments
  app.configure(function () {
    /** @namespace process.env.PORT */
    app.set('http_port', config.http_port);
    /** @namespace process.env.SPORT */
    if ( config.https_port != null )
        app.set('https_port', config.https_port);
    app.set('views', path.join(__dirname, '../views'));
    app.set('view engine', 'jade');
    app.use(favicon());
    app.use(express.logger('dev'));
    app.use(express.json());
    app.use(express.urlencoded());
    app.use(express.methodOverride());
    app.use(express.cookieParser(config.cookiesecret));
    app.use(express.session({ secret: 'SECRET' }));
    app.use(passport.initialize());
    app.use(passport.session());
    app.use(stylus.middleware(path.join(__dirname, '../../public')));
    app.use(express.static(path.join(__dirname, '../../public')));
    app.use('/skin', express.static(path.join(__dirname, '../../skin')));
    app.use('/repo', serveIndex(path.join(__dirname, '../../repo'), { template: directoryList }));
    app.use('/repo', express.static(path.join(__dirname, '../../repo')));
    app.use(logger('dev'));
    app.use(app.router);
    app.use(compression({
      threshold: 8192
    }));
  });

  app.use(function(err, req, res, next){
    // treat as 404
    if (err.message
        && (~err.message.indexOf('not found')
            || (~err.message.indexOf('Cast to ObjectId failed')))) {
      return next();
    }

    // log it
    console.error(err.stack);

    // error page
    res.status(500).render('error', { error: err.stack });
  });

  // assume 404 since no middleware responded
  app.use(function(req, res, next){
    res.status(404).render('error', {
      url: req.originalUrl,
      error: 'Not found'
    });
  });
};