USER=$1
if [ -z "$USER" ]; then
    echo Usage: ./deploy-site.sh username
    exit 1
fi

echo Deploying web site...
rsync -avP -e ssh --delete --exclude=.svn --exclude=apidocs website/htdocs $USER,xenqtt@web.sf.net:/home/project-web/xenqtt
echo
