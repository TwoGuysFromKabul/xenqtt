USER=$1
if [ -z "$USER" ]; then
    echo Usage: ./deploy-release.sh username version
    exit 1
fi

VERSION=$2
if [ -z "$VERSION" ]; then
    echo Usage: ./deploy-release.sh username version
    exit 1
fi

echo Deploying jars...
rm -fR target/deploy-jars
mkdir target/deploy-jars
JARDIR=target/deploy-jars/$VERSION
mkdir $JARDIR
cp target/xenqtt-$VERSION*.jar $JARDIR
cp README.txt $JARDIR
rsync -avP -e ssh  --delete-excluded --exclude=.svn $JARDIR $USER,xenqtt@frs.sf.net:/home/frs/project/xenqtt
echo

echo Deploying javadoc...
rm -fR target/deploy-docs
mkdir target/deploy-docs
DOCDIR=target/deploy-docs/$VERSION
mkdir $DOCDIR
cp -a target/site/apidocs/* $DOCDIR
rsync -avP -e ssh  --delete-excluded --exclude=.svn $DOCDIR $USER,xenqtt@web.sf.net:/home/project-web/xenqtt/htdocs/apidocs
echo
