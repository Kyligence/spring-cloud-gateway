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
gateway_properties="${GATEWAY_HOME}/conf/application.yml"
GATEWAY_PORT=$(sed '/^server.port:/!d;s/.*://' "${gateway_properties}")
GATEWAY_PORT=$(eval echo "$GATEWAY_PORT")

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

    gateway_properties="${GATEWAY_HOME}/conf/application.yml"

    cd ${GATEWAY_HOME}/server

    JAVA_OPTS="-Xms4g -Xmx4g -XX:+UseG1GC -XX:G1HeapRegionSize=4m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8"
    JAVA_OPTS="$JAVA_OPTS -XX:+PrintGCDateStamps -XX:+PrintGCDetails -Xloggc:${GATEWAY_HOME}/logs/gc.log -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${GATEWAY_HOME}/logs/heapdump.hprof"
    nohup java ${JAVA_OPTS} -Dspring.profiles.active=prod -Dreactor.netty.http.server.accessLogEnabled=true -Dgateway.home=${GATEWAY_HOME} -Dfile.encoding=UTF-8 -Dlogging.path=${GATEWAY_HOME}/logs -Dspring.config.additional-location=${gateway_properties} -Dloader.path="${GATEWAY_HOME}/server/jars" -jar gateway.jar >> ${GATEWAY_HOME}/logs/gateway.log 2>&1 < /dev/null & echo $! > ${GATEWAY_HOME}/pid &

    PID=`cat ${GATEWAY_HOME}/pid`
    echo $(date "+%Y-%m-%d %H:%M:%S ") "new Gateway process pid is "${PID} >> ${GATEWAY_HOME}/logs/gateway.log
    echo "Gateway is starting. It may take a while."
    retry=0
    while ! curl -k -s -f -o /dev/null http://127.0.0.1:"${GATEWAY_PORT}"/api/gateway/health
    do
       printf "."; sleep 1; let retry=retry+1
       if [ $retry -gt 30 ]; then
           echo ""
           echo "Gateway failed to start, please check logs/gateway.log for the details."
           exit 1
       fi
    done
    echo ""
    echo "Gateway is started, You can start use."
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

function reload() {
    result=$(curl -s -X GET --header 'Accept: application/json' http://127.0.0.1:"${GATEWAY_PORT}"/api/gateway/admin/reload)
    if [[ "${result}" =~ "success" ]]; then
         echo "Upgrade config success..."
    else
         echo "Upgrade config failed, Please check gateway.log or whether server is normal..."
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
elif [[ "$1" == "reload" ]]; then
    echo `date '+%Y-%m-%d %H:%M:%S '`"Reload Gateway config..."
    reload
else
    quit "Usage: start|stop|restart|reload"
fi
