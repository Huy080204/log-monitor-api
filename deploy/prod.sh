#!/bin/bash
SERVER_DEPLOY=192.168.0.26
TARGET_DIR=/opt/deploy/bbb-api
APP_ID=bbb-api

echo "Build source..."
cd ../source/spring-base-universal
mvn clean package
cd ../../deploy

echo "Update config..."
mkdir release

cp ../source/spring-base-universal/target/spring-base-universal-0.0.1.jar release/app.jar

cp config/* release/
rm -rf release/logback-spring.xml
sed -i '' "s/{ENV}/prod/g" release/application.properties

cp service-template.service release/$APP_ID.service
sed -i '' "s/{CONFIG_LOCATION}/$(printf '%s\n' "$TARGET_DIR" | sed -e 's/[]\/$*.^[]/\\&/g')/g" release/$APP_ID.service
sed -i '' "s/{ENV}/prod/g" release/$APP_ID.service


echo "Compress source..."
gtar -czf api.tar.gz release

echo "Deploy to server... $SERVER_DEPLOY"
echo " ---> Stop old service..."
ssh root@$SERVER_DEPLOY "mkdir -p $TARGET_DIR"
ssh root@$SERVER_DEPLOY "systemctl stop $APP_ID.service"
ssh root@$SERVER_DEPLOY "rm -rf $TARGET_DIR/*"

echo " ---> Remove old service"
ssh root@$SERVER_DEPLOY "rm -rf /lib/systemd/system/$APP_ID.service"

# echo " ---> Upload build..."
scp api.tar.gz root@$SERVER_DEPLOY:$TARGET_DIR/api.tar.gz
ssh root@$SERVER_DEPLOY "cd $TARGET_DIR && tar -xzf api.tar.gz && rm -rf api.tar.gz && mv release/* . && rm -rf release"
ssh root@$SERVER_DEPLOY "cd $TARGET_DIR && cp ../cfg/api/cfg.prop application-prod.properties"

echo " ---> Deploy new service..."
ssh root@$SERVER_DEPLOY "mv $TARGET_DIR/$APP_ID.service /lib/systemd/system/$APP_ID.service"
ssh root@$SERVER_DEPLOY "chmod 644 /lib/systemd/system/$APP_ID.service && systemctl daemon-reload"
ssh root@$SERVER_DEPLOY "systemctl enable $APP_ID.service"
ssh root@$SERVER_DEPLOY "systemctl start $APP_ID.service"

# echo "Cleanup..."
rm -rf release
rm -rf api.tar.gz

# check bbb-api: journalctl -u bbb-api -f
echo "############# DONE #############"