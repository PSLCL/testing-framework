## Panasonic Distributed Testing Framework - Portal
Node.js application using [express.js](http://expressjs.com), MySQL, [bootstrap](http://getbootstrap.com), jade templates and stylus.

## Requirements
The following tools must be installed on production systems (or be accessible), and Node.js should be installed on development machines.

1. [Node.js](http://nodejs.org) version 'v0.10.26'. Note that on Windows, you need to disable path modifications because it corrupts your environment.
2. [MySQL](http://mysql.com)

## Install packages
Packages are installed automatically based on the 'package.json' file.

`$ cd <directory containing this file>`

`$ npm install`

On production machines, install the rconsole module. This module is not supported on Windows, and so is not included
in the 'package.json' file.

`$ npm install rconsole`

### Troubleshooting
If You run into this error:

```
AttributeError: 'module' object has no attribute 'script_main'
gyp ERR! configure error
gyp ERR! stack Error: `gyp` failed with exit code: 1
```

The following can be used to resolve it: (Ubuntu)

```
sudo mv /usr/lib/python2.7/dist-packages/gyp /usr/lib/python2.7/dist-packages/gyp_backup
```

## Configure database
The following environment variables can be used to setup database properties for MySQL. Note that if database
connectivity is not available that the server will fail to start. The values shown are the defaults set
in 'server/config/config.js'.

 - MYSQL_HOST = localhost
 - MYSQL_PORT = 3306
 - MYSQL_USER = root
 - MYSQL_PASS =
 - MYSQL_DB   = qa_portal

These can be added to the command line call to run the server, or added to `~/.bash_profile`.

## Running the server
Windows does not fully support npm scripts, and so the process to launch a development server differ
on Windows and Linux.

### Windows
There are two programs that must be run:

`$ npm run watch`

`$ nodemon app`

### Linux
A single program will both run and watch for changes.

`$ npm run`

This now uses nodemon to run the server. It refreshes files when they change, and compiles client side files in the folder
`./client` into a `client.js` file.  Files can then be required like 'require('controllers/my_controller.js');'
this uses Browserify to compile the assets into one minified file.

### Production
Production systems must be Linux. To keep the server running even if there are failures the 'forever' program is
used. The following lines can be used in the directory that contains 'app.js':

 - `forever start app.js`
 - `forever stop app.js`
 - `forever list`
 
## Test Login
To login to the app use the example:

 - username: **test**
 - password: **test**
 
## Live Reload (development)
Use the `./config.js` variable `config.enable_livereload` to enable or disable live reload.
Using the chrome plugin for livereload and connecting to the server will reload the browser every change.

## Folder structure
 - `./client`
 	- Contains all client side javascript files that are compiled using `main.js`
 public`)
 - `./node_modules`
    - Local copy of the node packages required to run the application, should be kept out of version control.
 - `./public`
    - This folder contains Client output js from [Browserify](http://browserify.org/), and any static resources.
 - `./server`
 	- Contains all server side source files.
 	- `./config`
 		- `./config.js`
 			- Contains configuration variables for MYSQL, LDAP, page load size etc. Associated by `development` or `production` for a production server which uses LDAP instead of local. This is set by using the NODE_ENV parameter example: `export NODE_ENV=production` otherwise it uses development as its environment.
    - `./lib`
    	- Custom JavaScript Libraries used for the application. (Not for UI, those belong in `./
	 - `./routes`
	    - All files used to handle routing of pages within the application.
	    Each page is named for the `objects` it contains routes for.
	 - `./views`
	    - There are only three [Jade](http://jade-lang.com/) HTML templates, a layout, index and error.  Layout contains the main Angular index page setup.	    
 - `./app.js`
    - Main Application, contains Express config, routes, and listener for application.
 - `./package.json`
    - The packages that are installed when running `npm install` and version information for the project.
    
## Style guide

The following is taken from [Node.js Style Guide](https://github.com/felixge/node-style-guide/blob/master/Readme.md) as a starting point for the project.

### 2 Spaces for indention

Use 2 spaces for indenting your code and swear an oath to never mix tabs and
spaces - a special kind of hell is awaiting you otherwise.

### Newlines

Use UNIX-style newlines (`\n`), and a newline character as the last character
of a file. Windows-style newlines (`\r\n`) are forbidden inside any repository.

### No trailing whitespace

Just like you brush your teeth after every meal, you clean up any trailing
whitespace in your JS files before committing. Otherwise the rotten smell of
careless neglect will eventually drive away contributors and/or co-workers.

### Use Semicolons

According to [scientific research][hnsemicolons], the usage of semicolons is
a core values of our community. Consider the points of [the opposition][], but
be a traditionalist when it comes to abusing error correction mechanisms for
cheap syntactic pleasures.

[the opposition]: http://blog.izs.me/post/2353458699/an-open-letter-to-javascript-leaders-regarding
[hnsemicolons]: http://news.ycombinator.com/item?id=1547647

### 80 characters per line

Limit your lines to 80 characters. Yes, screens have gotten much bigger over the
last few years, but your brain has not. Use the additional room for split screen.