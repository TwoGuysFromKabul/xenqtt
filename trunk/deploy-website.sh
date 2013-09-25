USER=$1
if [ -z "$USER" ]; then
    echo Usage: ./deploy-site.sh username
    exit 1
fi

echo Deploying web site...
scp -r website/htdocs $USER@web.sf.net:/home/project-web/xenqtt
echo
