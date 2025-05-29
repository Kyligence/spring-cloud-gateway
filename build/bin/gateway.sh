#!/bin/bash

##
## Copyright (C) 2020 Kyligence Inc. All rights reserved.
##
## http://kyligence.io
##
## This software is the confidential and proprietary information of
## Kyligence Inc. ("Confidential Information"). You shall not disclose
## such Confidential Information and shall use it only in accordance
## with the terms of the license agreement you entered into with
## Kyligence Inc.
##
## THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
## "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
## LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
## A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
## OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
## SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
## LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
## DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
## THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
## (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
## OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
##

CURRENT_HOME=$(cd -P -- "$(dirname -- "$0")" && pwd -P)
GATEWAY_HOME=`dirname ${CURRENT_HOME}`

function quit {
    echo "$@"
    exit 1
}

function start_gateway() {
    if [[ -f "${GATEWAY_HOME}/pid" ]]; then
        PID=`cat ${GATEWAY_HOME}/pid`
        if ps -p ${PID} > /dev/null; then
            quit "Gateway is running, stop it first, PID is $PID"
        fi
    fi
    echo `date '+%Y-%m-%d %H:%M:%S '`"Starting Gateway..."

    gateway_properties="${GATEWAY_HOME}/conf/gateway.properties"

    cd ${GATEWAY_HOME}/server

    JAVA_OPTS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${GATEWAY_HOME}/logs -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:${GATEWAY_HOME}/logs/gc-%t-%p.log"

    nohup java $JAVA_OPTS -Xms2g -Xmx4g -Dspring.profiles.active=prod -Dreactor.netty.http.server.accessLogEnabled=true -Dgateway.home=${GATEWAY_HOME} -Dfile.encoding=UTF-8 -Dlogging.path=${GATEWAY_HOME}/logs -Dspring.config.additional-location=${gateway_properties} -Dloader.path="${GATEWAY_HOME}/server/jars,${GATEWAY_HOME}/server/ext" -jar gateway.jar >> ${GATEWAY_HOME}/logs/gateway.log 2>&1 < /dev/null & echo $! > ${GATEWAY_HOME}/pid &

    PID=`cat ${GATEWAY_HOME}/pid`
    echo $(date "+%Y-%m-%d %H:%M:%S ") "new Gateway process pid is "${PID} >> ${GATEWAY_HOME}/logs/gateway.log
    echo "Gateway is starting. It may take a while."
    echo "You may also check status via: PID:`cat ${GATEWAY_HOME}/pid`, or Log: ${GATEWAY_HOME}/logs/gateway.log"
}

function stop_gateway() {
    if [[ -f ${GATEWAY_HOME}/pid ]]; then
        PID=`cat ${GATEWAY_HOME}/pid`
        if ps -p ${PID} > /dev/null; then
            echo `date '+%Y-%m-%d %H:%M:%S '`"Stopping Gateway: ${PID}"
            kill ${PID}
            for i in $(seq 1 10) ; do
                sleep 2
                if ps -p ${PID} -f | grep gateway > /dev/null; then
                    if [[ "${i}" == "10" ]]; then
                        echo `date '+%Y-%m-%d %H:%M:%S '`"Killing Gateway: $PID"
                        kill -9 ${PID}
                    fi
                    continue
                fi
                break
            done
            rm ${GATEWAY_HOME}/pid
            return 0
        else
            return 1
        fi
    else
        return 1
    fi
}

function backup_gateway() {
    if [[ -d ${GATEWAY_HOME}/backup ]]; then
        if [[ -z $(ls -A ${GATEWAY_HOME}/backup) ]]; then
            echo "backup 文件已存在，但是为空"
        else
            filename_suffix=$(date + "%Y%m%d%H%M%S")
            filename="backup_history_${filename_suffix}"
            mv ${GATEWAY_HOME}/backup ${filename}
        fi
    fi
    mkdir -p ${GATEWAY_HOME}/backup
    mv ${GATEWAY_HOME}/commit_SHA1 ${GATEWAY_HOME}/conf ${GATEWAY_HOME}/server ${GATEWAY_HOME}/VERSION ${GATEWAY_HOME}/backup
}


function rollback_gateway() {
    if [[ -d ${GATEWAY_HOME}/backup ]]; then
        if [[ -z $(ls -A ${GATEWAY_HOME}/backup) ]]; then
            echo "backup 文件已存在，但是为空"
            return 1
        else
            filename_suffix=$(date + "%Y%m%d%H%M%S")
            filename="rollback_${filename_suffix}"
            mkdir -p ${GATEWAY_HOME}/${filename}
            mv ${GATEWAY_HOME}/commit_SHA1 ${GATEWAY_HOME}/conf ${GATEWAY_HOME}/server ${GATEWAY_HOME}/VERSION ${GATEWAY_HOME}/${filename}
            mv ${GATEWAY_HOME}/backup/commit_SHA1 ${GATEWAY_HOME}/backup/conf ${GATEWAY_HOME}/backup/server ${GATEWAY_HOME}/backup/VERSION ${GATEWAY_HOME}
        fi
    else
        return 1
    fi
}

if [[ "$1" == "start" ]]; then
    start_gateway
elif [[ "$1" == "stop" ]]; then
    stop_gateway
elif [[ "$1" == "restart" ]]; then
    echo `date '+%Y-%m-%d %H:%M:%S '`"Restarting Gateway..."
    stop_gateway
    start_gateway
elif [[ "$1" == "backup" ]]; then
    backup_gateway
elif [[ "$1" == "rollback" ]]; then
    rollback_gateway
else
    quit "Usage: 'gateway.sh start' or 'gateway.sh stop' or 'gateway.sh restart'"
fi