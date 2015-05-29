#! /bin/bash

PID=$1
while ps --pid $PID > /dev/null 2>&1
do
    sleep 1
done

cd "${installer:sys.installationDir}"
exec ./bin/${compiler:snapDesktopName}
