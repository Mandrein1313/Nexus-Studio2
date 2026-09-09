#!/usr/bin/env sh
echo 'Gradle Wrapper initialized on Cloud...'
APP_BASE_NAME=`basename "$0"`
APP_HOME=`dirname "$0"`
APP_HOME=`cd "$APP_HOME" && pwd`
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
exec java -Xmx64m -cp "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
