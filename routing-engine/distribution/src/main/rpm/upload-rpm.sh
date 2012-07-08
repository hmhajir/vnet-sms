#!/bin/sh

set -e
set -o nounset

VERSION="$1"
RELEASE="$2"
DIST="$3"
ARCH="$4"
REPO="$5"

MODULEDIR=$( cd "$( dirname "$0" )" && pwd )
RPMBUILD=${MODULEDIR}/target/rpm/routing-engine
PACKAGE=routing-engine-${VERSION}-${RELEASE}.${DIST}.${ARCH}.rpm

echo ""
echo "------------------------------------------------------------------------"
echo "Uploading rpm to pulp server ..."
/usr/bin/pulp-admin -u admin -p admin content upload --nosig --verbose --dir ${RPMBUILD}/RPMS/${ARCH}
echo "Uploaded rpm to pulp server"
echo "------------------------------------------------------------------------"

echo ""
echo "------------------------------------------------------------------------"
echo "Associating generated rpm ${PACKAGE} with pulp repository ${REPO} ..."
/usr/bin/pulp-admin -u admin -p admin repo add_package --id ${REPO} --package=${PACKAGE}
echo "Associated generated rpm ${PACKAGE} with pulp repository ${REPO}"
echo "------------------------------------------------------------------------"

echo ""
echo "------------------------------------------------------------------------"
echo "Scheduling metadata update for pulp repository ${REPO} ..."
/usr/bin/pulp-admin -u admin -p admin repo generate_metadata --id ${REPO}
echo "Scheduled metadata update for pulp repository ${REPO}"
echo "------------------------------------------------------------------------"