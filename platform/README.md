## Panasonic Distributed Testing Framework - Platform

## Requirements
The following tools must be installed on production systems (or be accessible)

1. [STAF](http://prdownloads.sourceforge.net/staf/STAF3424-setup-linux-amd64-NoJVM.bin?download) version 'v3.4.24'.

## Install packages
STAF is installed via a STAF's InstallAnywhere installation application.

`$ cd ~`
`$ wget http://prdownloads.sourceforge.net/staf/STAF3424-setup-linux-amd64-NoJVM.bin`
`$ chmod +x STAF3424-setup-linux-amd64-NoJVM.bin`
`$ sudo ./STAF3424-setup-linux-amd64-NoJVM.bin`

Take all the defaults (skip "Allow STAF to Register" with a 0 if desired).
edit the configuration file /usr/local/staf/bin/STAF.cfg
1. add "option ConnectTimeout=60000" to the end of both "interface ..." lines.
2. add a new line with "trust level 5 default" 

STAF is started with /usr/local/startSTAFProc.sh
a nohup.out log file is created at /home/ec2-user

Setup your machine to auto start STAF on reboot (i.e. /etc/rc.local)