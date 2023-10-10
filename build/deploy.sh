#!/bin/bash

cd "$( dirname "$0" )"
cd ..

current_dir=`pwd`
echo "current_dir : $current_dir"

# VERSION, need to update per version.
version="gateway-4.6.16.1000"
echo ${version}"."`date +%s`  > ${current_dir}/VERSION

mvn clean package -P km -DskipTests -Dmaven.javadoc.skip=true
