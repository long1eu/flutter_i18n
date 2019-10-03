#!/bin/sh

echo ""

if [ "x$1" = "x" ]; then
	echo "Syntax: $0 <version> [publish]"
	echo ""
	echo "<version> is the version of the plugin that'll appear in plugins.jetbrains.com,"
	echo "          for example: 1.1.3"
	echo ""
	echo "[publish] add the word 'publish' after the plugin version to both build and"
	echo "          publish the plugin to plugins.jetbrains.com"
	echo ""
	exit 1
fi

# Supported platform versions.
VERSIONS="181 182 183 191 192"

# Requested version to build.
PLUGIN_VERSION=$1

# Whether to publish as well or not.
SHOULD_PUBLISH=$2

echo "Provisioning for plugin version: $PLUGIN_VERSION"
echo ""

# Build for all platforms.
for VERSION in $VERSIONS; do
	BUILD_COMMAND="./gradlew clean buildPlugin -PideaVersionPrefix=$VERSION -PpluginVersion=$PLUGIN_VERSION"

	echo "Building for IntelliJ platform version: $VERSION"
	$BUILD_COMMAND >/dev/null 2>&1

	if [ $? == 0 ]; then
		if [ "x$SHOULD_PUBLISH" = "xpublish" ]; then
			PUBLISH_COMMAND="./gradlew clean publishPlugin -PideaVersionPrefix=$VERSION -PpluginVersion=$PLUGIN_VERSION"

			echo "Publishing to JetBrains..."
			$PUBLISH_COMMAND >/dev/null 2>&1

			if [ $? != 0 ]; then
				echo ""
				echo "Ooops! Unable to publish to JetBrains for IntelliJ platform version: $VERSION"
				echo "Please re-run the command manually to see what's wrong:"
				echo ""
				echo $PUBLISH_COMMAND
				echo ""
				exit 3
			fi
		fi
	else
		echo ""
		echo "Ooops! Unable to build for IntelliJ platform version: $VERSION"
		echo "Please re-run the command manually to see what's wrong:"
		echo ""
		echo $BUILD_COMMAND
		echo ""
		exit 2
	fi

	echo ""
	echo "All done! Check the status on https://plugins.jetbrains.com/plugin/10128-flutter-i18n"
	echo ""
done
