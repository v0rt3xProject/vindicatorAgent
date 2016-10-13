#!/usr/bin/env bash

set -xe

BASE_PATH=$(cd $(dirname $0) && pwd)
JAVA=$(which java)
NICE=$(which nice)

PRIORITY=19

if [[ -z "${JAVA}" ]]; then
    echo "Java isn't installed"
    exit 1
fi

pushd ${BASE_PATH} 2> /dev/null 1> /dev/null

MAIN_JAR=$(basename $(find "${BASE_PATH}" -maxdepth 1 -name '*.jar' | tail -1))

${NICE} -n ${PRIORITY} ${JAVA} -cp "${MAIN_JAR}:lib/*" ru.v0rt3x.vindicator.agent.VindicatorAgent "$@"

popd 2> /dev/null 1> /dev/null