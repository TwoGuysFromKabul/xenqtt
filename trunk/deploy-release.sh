USER=$1
if [ -z "$USER" ]; then
    echo Usage: ./deploy-release.sh username version api-key
    exit 1
fi

VERSION=$2
if [ -z "$VERSION" ]; then
    echo Usage: ./deploy-release.sh username version api-key
    exit 1
fi

API_KEY=$3
if [ -z "$API_KEY" ]; then
    echo Usage: ./deploy-release.sh username version api-key
    exit 1
fi

echo Deploying jars...
rm -fR target/deploy-jars
mkdir target/deploy-jars
JARDIR=target/deploy-jars/$VERSION
mkdir $JARDIR
cp target/xenqtt-$VERSION*.jar $JARDIR
cp $JARDIR/xenqtt-$VERSION.jar $JARDIR/xenqtt.jar
rsync -avP -e ssh  --delete-excluded --exclude=.svn $JARDIR $USER,xenqtt@frs.sf.net:/home/frs/project/xenqtt
scp README.txt $USER@frs.sf.net:/home/frs/project/xenqtt
echo

echo Deploying javadoc...
rm -fR target/deploy-docs
mkdir target/deploy-docs
DOCDIR=target/deploy-docs/$VERSION
mkdir $DOCDIR
cp -a target/site/apidocs/* $DOCDIR
rsync -avP -e ssh  --exclude=.svn --exclude=.htaccess $DOCDIR $USER,xenqtt@web.sf.net:/home/project-web/xenqtt/htdocs/apidocs
echo

echo Configuring default download file...
curl -H "Accept: application/json" -X PUT -d "default=windows&default=mac&default=linux&default=bsd&default=solaris&default=others" -d "api_key=$API_KEY" https://sourceforge.net/projects/xenqtt/files/$VERSION/xenqtt-$VERSION.jar
echo
