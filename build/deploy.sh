#!/bin/bash

cd "$(dirname "$0")"
cd ..

current_dir=`pwd`
echo "current directory : $current_dir"

for i in $*
do
    case "$i" in
    --version*)
        export version=${i#*=}
        ;;
    *)
        ;;
    esac
done

if [ -z "$version" ]; then
    echo "version not set, use default version instead."
    version="2.0.0.1000"
fi

echo "version: $version"
echo ${version}"."`date +'%Y%m%d%H%M%S'`  > ${current_dir}/VERSION

mvn clean package -P km -DskipTests -Dmaven.javadoc.skip=true
