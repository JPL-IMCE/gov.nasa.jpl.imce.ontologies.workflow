#!/bin/bash

# Read-only
export ETC="$(dirname "$(pwd)")/etc"

# Read-only
export WORKFLOW_SOURCES="$(dirname "$(pwd)")/workflow"

# Read-only
export ONTOLOGIES="$(dirname "$(pwd)")/target/ontologies"

# Read-only
FUSEKI_PROJ="$(dirname $(dirname "$(pwd)"))/gov.nasa.jpl.imce.ontologies.fuseki"
export FUSEKI_HOME="$FUSEKI_PROJ/target/fuseki"

export FUSEKI_BIN="${FUSEKI_HOME}/bin"

# Read-only
export TOOLS="$(dirname "$(pwd)")/target/tools"

# Read/write
export WORKFLOW="$(dirname "$(pwd)")/target/workflow"

# Read/write
export FUSEKI_BASE="$(dirname "$(pwd)")/target/run"

# Read-only
export AUDITS="$(dirname "$(pwd)")/data/Audit"

# SBT
[ -z "$SBT" ] && export SBT=$(which sbt)

[ ! -d "$WORKFLOW" ] && mkdir "$WORKFLOW"

[ ! -d "$WORKFLOW/artifacts" ] && mkdir "$WORKFLOW/artifacts"

export RUBYLIB="$TOOLS/lib/Application":\
"$TOOLS/lib/Audit":\
"$TOOLS/lib/IMCE":\
"$TOOLS/lib/Jena":\
"$TOOLS/lib/Makefile":\
"$TOOLS/lib/OWLAPI":\
"$TOOLS/lib/OntologyBundles":\
"$TOOLS/lib/Pellet":\
"$TOOLS/lib/ruby-jena":\
"$TOOLS/lib/ruby-owlapi":\
"$TOOLS/lib/ruby-pellet":\
"$TOOLS/lib/JGraphT":\
"$TOOLS/lib/OMFMetadata"

[ -z "$GEM_HOME" ] && echo "GEM_HOME environment not set!"
# && exit -1

[ -z "$JRUBY" ] && export JRUBY="$(which jruby)"

export GEM_PATH="${GEM_HOME}:$(dirname "$(pwd)")"

export PARALLEL_MAKE_OPTS="-j8 -l16"

JENA_DATASET_NAME="imce-ontologies"

if [ $# -gt 0 ]; then
  JENA_DATASET_NAME="$1"
fi

export JENA_DATASET=$JENA_DATASET_NAME

export JENA_HOST="localhost"

JENA_PORT_VALUE="8898"

if [ $# -gt 1 ]; then
  JENA_PORT_VALUE="$2"
fi

export JENA_PORT=$JENA_PORT_VALUE

# Add as maven dependency
export DOCBOOK_XHTML_XSL="${TOOLS}/docbook/xhtml/docbook.xsl"
