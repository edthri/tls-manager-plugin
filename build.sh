#!/usr/bin/env bash

function main() {

  echo "########################################"
  echo
  echo "   mvn cleaning..."
  echo
  echo "########################################"
  mvn clean

  echo "########################################"
  echo
  echo "   Building jars..."
  echo
  echo "########################################"
  mvn install package -DskipTests

  PLUGIN_PATH=$(mvn exec:exec --non-recursive --quiet -Dexec.executable="echo" -Dexec.args='${mirth.plugin.path}')
  ARTIFACT_ID=$(mvn exec:exec --non-recursive --quiet -Dexec.executable="echo" -Dexec.args='${project.artifactId}')

  echo "########################################"
  echo
  echo "   Copying libraries..."
  echo
  echo "########################################"
  mkdir -p "$STAGING_DIR/libs"

  modules=("server" "client" "shared")
  for module in "${modules[@]}"; do
    if find "libs/runtime/$module/" -maxdepth 1 -iname "*.jar" | grep -q .; then
      cp libs/runtime/"$module"/*.jar "$STAGING_DIR/libs/"
    else
      echo "No .jar files in libs/runtime/$module/"
    fi
  done

  echo "########################################"
  echo
  echo "   Generating plugin.xml..."
  echo
  echo "########################################"
  mvn -N com.kaurpalang:mirth-plugin-maven-plugin:3.0.0:generate-plugin-xml

  mv plugin.xml "$STAGING_DIR"

  echo "########################################"
  echo
  echo "   Packaging plugin..."
  echo
  echo "########################################"
  cp {client,server,shared}/target/*.jar "$STAGING_DIR/"
  cp web-ui/target/tls-manager.war "$STAGING_DIR/"

  pushd target
  mv staging "$PLUGIN_PATH"
  zip -r "$PLUGIN_PATH" "$PLUGIN_PATH"
  popd
}

set -euxo pipefail

STAGING_DIR=target/staging
main
