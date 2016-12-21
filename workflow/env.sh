#!/bin/bash

# Read-only
export WORKFLOW_SOURCES=$(dirname $(pwd))/workflow

# Read-only
export ONTOLOGIES=$(dirname $(pwd))/target/ontologies

# Read-only
export TOOLS=$(dirname $(pwd))/target/tools

# Read/write
export WORKFLOW=$(dirname $(pwd))/target/workflow

[ ! -d $WORKFLOW ] && mkdir $WORKFLOW

[ ! -d $WORKFLOW/artifacts ] && mkdir $WORKFLOW/artifacts

export RUBYLIB=$TOOLS/lib/Application:\
$TOOLS/lib/Audit:\
$TOOLS/lib/IMCE:\
$TOOLS/lib/Jena:\
$TOOLS/lib/Makefile:\
$TOOLS/lib/OWLAPI:\
$TOOLS/lib/OntologyBundles:\
$TOOLS/lib/Pellet:\
$TOOLS/lib/ruby-jena:\
$TOOLS/lib/ruby-owlapi:\
$TOOLS/lib/ruby-pellet:\
$TOOLS/lib/JGraphT:\
$TOOLS/lib/OMFMetadata

[ -z "$GEM_HOME" ] && echo "GEM_HOME environment not set!"
# && exit -1

export JRUBY=$(which jruby)
echo "# JRUBY=$JRUBY"

export GEM_PATH="${GEM_HOME}:/home/sjenkins/.rvm/gems/jruby-1.7.19@global"

export PARALLEL_MAKE_OPTS="-j16 -l32"

# From other_jena libraries
export FUSEKI_DIR="/opt/local/jena-fuseki1-1.3.1"

export FUSEKI_HOME="/opt/local/jena-fuseki1-1.3.1"

export JENA_DATASET="imce-ontologies"

export JENA_HOST="localhost"

export JENA_PORT="8888"

# Add as maven dependency
export DOCBOOK_XHTML_XSL="/usr/local/sgml/docbook/xsl-stylesheets-1.78.1/xhtml/docbook.xsl"
