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

dir=$(dirname ${0})

cd ${dir}/..

echo "Build Gateway"
mvn clean install -DskipTests $@ || { exit 1; }

timestamp=`date '+%Y%m%d%H%M%S'`
package_dir="Kyligence-Gateway-${timestamp}"

cd build/
rm -rf ${package_dir}
mkdir ${package_dir}
mkdir ${package_dir}/conf
mkdir ${package_dir}/logs

cp -rf conf/gateway-log4j.properties ${package_dir}/conf/gateway-log4j.properties
cp -rf conf/gateway.properties ${package_dir}/conf/gateway.properties

cp -rf bin/ ${package_dir}/bin/

mkdir ${package_dir}/server
cp -rf ../kylin-gateway/target/kylin-gateway-2.2.3.RELEASE.jar ${package_dir}/server/gateway.jar
cp -rf ../kylin-gateway/target/jars ${package_dir}/server

find ${package_dir} -type d -exec chmod 755 {} \;
find ${package_dir} -type f -exec chmod 644 {} \;
find ${package_dir} -type f -name "*.sh" -exec chmod 755 {} \;

package_name="${package_dir}.tar.gz"

rm -rf ../dist
mkdir -p ../dist
tar -cvzf ../dist/${package_name} ${package_dir}
rm -rf ${package_dir}
cd ../dist
echo "Package ready."
ls ${package_name}
