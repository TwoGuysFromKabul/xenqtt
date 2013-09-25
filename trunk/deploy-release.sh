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
scp -r $JARDIR $USER@frs.sf.net:/home/frs/project/xenqtt
echo

echo Deploying javadoc...
rm -fR target/deploy-docs
mkdir target/deploy-docs
DOCDIR=target/deploy-docs/$VERSION
mkdir $DOCDIR
cp -a target/site/apidocs/* $DOCDIR
scp -r $DOCDIR $USER@web.sf.net:/home/project-web/xenqtt/htdocs/apidocs
echo
