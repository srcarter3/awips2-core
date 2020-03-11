#!/usr/bin/bash
# run ignite for awips2.

usage="Usage: $(basename "$0") developer|production [debug] [ncep]

Where:
    developer   Run using less memory and without connecting to a cluster
    production  Run using more memory and attempt to cluster with nodes 
                on hosts cache1,cache2,cache3
    debug       Allow socket connections from a java debugger
    ncep        Include cache4, cache5 and cache6 in the cluster
"

# When the args come in through a systemd template it is simpler to pass in a
# single argument. If multiple modes are needed then allow multiple modes in a
# single argument with any non-alphanumeric separator. For example passing in
# 'developer-debug' is equivalent to passing in 'developer' and 'debug'.
ARGS=()
for arg in "$@"
do
    ARGS+=( $(echo "$arg" | grep -E -o '[a-zA-Z0-9]+') )
done

path_to_script=`readlink -f $0`
dir=$(dirname $path_to_script)
set -a
IGNITE_HOME=${IGNITE_HOME:-`dirname $dir`}

AWIPS_HOME=${AWIPS_HOME:-`dirname $IGNITE_HOME`}
JAVA_HOME=${JAVA_HOME:-${AWIPS_HOME}/java}
EDEX_HOME=${EDEX_HOME:-${AWIPS_HOME}/edex}

PYPIES_PORT=${PYPIES_PORT:-9582}
PYPIES_COMPATIBILITY_PORT=${PYPIES_COMPATIBILITY_PORT:-9585}
DEBUG_PORT=${DEBUG_PORT:-5102}

for arg in "${ARGS[@]}"
do
    case "${arg}" in
        production)
            IGNITE_SERVERS=${IGNITE_SERVERS:-cache1,cache2,cache3}
            JVM_OPTS+="-Xms8g -Xmx8g -server -XX:MaxMetaspaceSize=256m -XX:+UseG1GC"
            IGNITE_DATA_REGION_MAX_SIZE_GB=${IGNITE_DATA_REGION_MAX_SIZE_GB:-64}
            IGNITE_DATA_REGION_INITIAL_SIZE_GB=${IGNITE_DATA_REGION_INITIAL_SIZE_GB:-64}
            # The largest objects I have seen are about 374MiB, ignite documentation suggests we need enough pages to
            # hold twice that amount. I rounded this up to allow 1024MiB of empty 16KiB pages so we have a little room
            # to grow.
            IGNITE_DATA_REGION_EMPTY_PAGES_POOL_SIZE=${IGNITE_DATA_REGION_EMPTY_PAGES_POOL_SIZE:-65536}
            IGNITE_CACHE_BACKUPS=${IGNITE_CACHE_BACKUPS:-1}
            PYPIES_HOST=${PYPIES_HOST:-dx2}
            ;;
        developer)
            IGNITE_SERVERS=${IGNITE_SERVERS:-localhost}
            JVM_OPTS+="-Xms512m -Xmx1g -server -XX:MaxMetaspaceSize=256m -XX:+UseG1GC"
            IGNITE_DATA_REGION_MAX_SIZE_GB=${IGNITE_DATA_REGION_MAX_SIZE_GB:-2}
            IGNITE_DATA_REGION_INITIAL_SIZE_GB=${IGNITE_DATA_REGION_INITIAL_SIZE_GB:-1}
            IGNITE_DATA_REGION_EMPTY_PAGES_POOL_SIZE=${IGNITE_DATA_REGION_EMPTY_PAGES_POOL_SIZE:-8192}
            IGNITE_CACHE_BACKUPS=${IGNITE_CACHE_BACKUPS:-0}
            PYPIES_HOST=${PYPIES_HOST:-localhost}
            ;;
        ncep)
            IGNITE_SERVERS+=",cache4,cache5,cache6"
            ;;
        debug)
            JVM_OPTS+=" -Xdebug -Xrunjdwp:transport=dt_socket,address=${DEBUG_PORT},server=y,suspend=n"
            ;;
        *)
            stdbuf -e0 printf "Unrecognized argument: %s\n" ${arg} >&2
            stdbuf -o0 printf "$usage"
            exit 2
            ;;
    esac
done

CLASSPATH=${EDEX_HOME}/lib/plugins/*

for DIRECTORY in ${EDEX_HOME}/lib/dependencies/*
do
    CLASSPATH+=":$DIRECTORY/*"
done

exec ${JAVA_HOME}/bin/java \
            ${JVM_OPTS} \
            -DIGNITE_HOME=${IGNITE_HOME} \
            -Dlogback.configurationFile=${IGNITE_HOME}/config/ignite-logback.xml \
            org.apache.ignite.startup.cmdline.CommandLineStartup \
            config/awips2-config.xml
