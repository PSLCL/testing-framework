## Panasonic Distributed Testing Framework - Portal
Node.js application using [express.js](http://expressjs.com), MySQL, [bootstrap](http://getbootstrap.com), jade templates and stylus.

## Requirements
The following tools must be installed on production systems (or be accessible), and Node.js should be installed on development machines.

1. [Node.js](http://nodejs.org) version 'v0.10.26' or newer. Note that on Windows, you need to disable path modifications because it corrupts your environment.
2. Git
3. [MySQL](http://mysql.com) version 5.7 or newer.
4. ports 80 and/or 443 should be accessible

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

## Configuration 

The following example configuration files can be found under the directory `testing-framework/portal/example-config`:

* `config.js` - Portal configuration
* `server.pfx-readme` - 
* `ivysettings.xml` - Ivy artifact provider configuration

Each of these files should be copied to the `testing-framework/portal/config/` directory and edited appropriately.

### Configure Database
The following environment variables can be used to setup database properties for MySQL. Note that if database
connectivity is not available that the server will fail to start. The values shown are the defaults set
in `testing-framework/portal/config/config.js`.

 - `DTF_DB_HOST     = localhost`
 - `DTF_DB_PORT     = 3306`
 - `DTF_DB_USER     = root`
 - `DTF_DB_PASSWORD =`
 - `DTF_DB_SCHEMA   = qa_portal`

These can be added to the command line call to run the server, or added to `~/.bash_profile`.

### Configure Ivy Artifact Provider

The Ivy artifact provider is configured using an `ivysettings.xml` file in the `testing-framework/portal/config/` directory. On synchronize, the Ivy Artifact Provider will download artifacts provided by included resolvers.

### Configure Server Certificate

You will need to generate a server certificate appropriate to your
site/hostname and place it in `testing-framework/portal/config/server.pfx`. Additionally you will need to set the password for your certificate in the `testing-framework/portal/config/config.js`
file.

## Build the platform
The dtfexec command line tool is included as part of the [testing-framework platform](../platform/README.md).

## Running the server
Windows does not fully support npm scripts, and so the process to launch a development server differs on Windows and Linux. For both linux and Windows change directory to `testing-framework/portal`.

### Windows Development
There are two programs that must be run:

`$ npm run watch`

`$ nodemon app`

### Linux Development
A single program will both run and watch for changes.

`$ npm run`

This now uses nodemon to run the server. It refreshes files when they change, and compiles client side files in the folder
`./client` into a `client.js` file.  Files can then be required like 'require('controllers/my_controller.js');'
this uses Browserify to compile the assets into one minified file.

### Linux Production
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
    - `./lib`
    	- Custom JavaScript Libraries used for the application. (Not for UI, those belong in `
	 - `./routes`
	    - All files used to handle routing of pages within the application.
	    Each page is named for the `objects` it contains routes for.
	 - `./views`
	    - There are only three [Jade](http://jade-lang.com/) HTML templates, a layout, index and error.  Layout contains the main Angular index page setup.	    
- `./config`
 	- `./config.js`
 		- Contains configuration variables for MYSQL, LDAP, page load size etc. Associated by `development` or `production` for a production server which uses LDAP instead of local. This is set by using the NODE_ENV parameter example: `export NODE_ENV=production` otherwise it uses development as its environment.
	- `./ivysettings.xml`
		- Contains Ivy artifact provider settings.
	- `./server.pfx`
		- The server certificate.
 - `./app.js`
    - Main Application, contains Express config, routes, and listener for application.
 - `./package.json`
    - The packages that are installed when running `npm install` and version information for the project.

## Build Using Ivy Repository

The Portal provides an Ivy repository containing the platform .jar files required to build test generators. Include the Portal's Ivy repository by adding the following two lines, setting the correct hostname for the portal, to your ivysettings.xml file:

`<property name="com.pslcl.dtf.url" override="false" value="<hostname>"/>`

`<include file="${com.pslcl.dtf.url}/ivysettings.pslcl-dtf.xml"/>`
    
## Style guide

Use the [Node.js Style Guide](https://github.com/felixge/node-style-guide/blob/master/Readme.md) as a starting point for the project.