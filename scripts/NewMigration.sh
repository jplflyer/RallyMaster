#!/usr/bin/env bash -e
#
# This creates a new Flyway migration file

function getOutputDir() {
  local raw=$(grep -Eo 'filesystem:[^"]+' RallyServer/flyway.toml | head -1)
  local rel="${raw#filesystem:}"
  outputdir=RallyServer/${rel}
}

getOutputDir

if ! type mvn >/dev/null ; then
  echo "ERROR: Missing mvn command"
  exit 2
fi

function log() {
  echo $* >&2
}

function usage() {
  echo "Usage: $0 some useful comment"
  exit 0
}

function fileName() {
  local now=$(TZ="America/New_York" date +"%Y%m%d%H%M%S")
  local str
  read -ra str <<< "$*"
  local comment=$(printf '%s' "${str[@]^}")
  version=$(gradlew -q printVersion | sed 's/-SNAPSHOT//g' | grep -v '\[.*' | tr . _)
  echo V${version}_${now}__${comment}.sql
}

if [ ! -d ${outputdir} ]; then
  echo Output directory ${outputdir} not found
  exit 0
fi

if [ -z "$*" ]; then
  usage
fi

re="[-]*help"
if [[ "$1" =~ ${re} ]]; then
  usage
fi

fn=$(fileName $*)
fulldest=${outputdir}/${fn}
echo -n "About to create ${fulldest}? (Y/n): "
read -n 1 confirm
echo  # The read kept us on the previous line. Just output a newline.
if [[ -z "${confirm}" || "${confirm^^}" =~ ^(Y)$ ]]; then
  cp scripts/NewMigration.sql ${fulldest}
  echo Filename: ${outputdir}/${fn}
  git add ${outputdir}/${fn}
else
  echo "Cancelled."
  exit 2
fi

