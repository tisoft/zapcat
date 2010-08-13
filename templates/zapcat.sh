#!/bin/sh

if [ $# -lt 2 ] ; then
	echo "UNAVAILABLE"
	exit 1
fi

ZAPCAT_PORT=$1
shift

# hack when use
# zapcat[zapcat_port,jmx[toto=pouet,name=coucou]]
# => zabbix will remove the , in "pouet,name" ...
# so zapcat.sh will get 3 arguments:
# zapcat[zapcat_port jmx[toto=pouet name=coucou]]
#
# here, we rebuild it 
KEY=$1
shift
while [ ! -z "$1" ] ; do
	KEY=$KEY",$1"
	shift
done

zabbix_get -s 127.0.0.1 -p "$ZAPCAT_PORT" -k "$KEY"

