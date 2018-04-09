#
#    $HeadURL$
#
#    $LastChangedRevision$
#    $LastChangedDate$
#
#    $LastChangedBy$
#
#    Copyright (c) 2009,2010 California Institute of Technology.
#    All rights reserved.
#

JRUBY ?= jruby
JRUBY_CMD ?= $(JRUBY) --headless
MAKE_MAKEFILE ?= $(TOOLS)/tools/make-makefile
SBT ?= sbt

clean:
	/bin/rm -rf $(WORKFLOW)
	mkdir $(WORKFLOW)
	mkdir $(WORKFLOW)/artifacts
	mkdir $(WORKFLOW)/log


# Copy required files, and create directory target/workflow in the process
# if it does not already exist

$(WORKFLOW)/Makefile.erb: $(WORKFLOW_SOURCES)/Makefile.erb
	cp -p $< $@

$(WORKFLOW)/log4j.properties: $(WORKFLOW_SOURCES)/log4j.properties
	cp -p $< $@

$(WORKFLOW)/logging.properties: $(WORKFLOW_SOURCES)/logging.properties
	cp -p $< $@

$(WORKFLOW)/log:
	mkdir $@

bootstrap: $(WORKFLOW)/Makefile $(WORKFLOW)/.symlinks

$(WORKFLOW)/Makefile: $(WORKFLOW_SOURCES)/Makefile.erb $(WORKFLOW)/logging.properties $(WORKFLOW)/log4j.properties $(WORKFLOW)/Makefile.erb
	@echo $< '->' $@
	(cd $(WORKFLOW); $(JRUBY_CMD) $(MAKE_MAKEFILE) $< > $@.tmp) && mv $@.tmp $@

$(WORKFLOW)/.symlinks:
	echo "# ONTOLOGIES=$(ONTOLOGIES)"
	ls -l $(ONTOLOGIES)
	ln -sv $(ONTOLOGIES)/* $(WORKFLOW)
	touch $(WORKFLOW)/.symlinks

catalogs:
	(cd $(WORKFLOW); nice make ${PARALLEL_MAKE_OPTS} catalogs)

dependencies:
	(cd $(WORKFLOW); nice make ${PARALLEL_MAKE_OPTS} dependencies)

validation-dependencies:
	(cd $(WORKFLOW); nice make ${PARALLEL_MAKE_OPTS} validation-dependencies)

validate-xml:
	(cd $(WORKFLOW); nice make ${PARALLEL_MAKE_OPTS} validate-xml)

validate-owl:
	(cd $(WORKFLOW); nice make ${PARALLEL_MAKE_OPTS} validate-owl)

validate-groups:
	(cd $(WORKFLOW); nice make ${PARALLEL_MAKE_OPTS} validate-groups)

validate-bundles:
	(cd $(WORKFLOW); nice make ${PARALLEL_MAKE_OPTS} validate-bundles)

load-production:
	(cd $(WORKFLOW); nice make ${PARALLEL_MAKE_OPTS} load-production)

digests:
	(cd $(WORKFLOW); nice make ${PARALLEL_MAKE_OPTS} digests)

artifacts:
	(cd $(WORKFLOW); nice make ${PARALLEL_MAKE_OPTS} artifacts)

profiles:
	(find $(WORKFLOW) -type f -name "*.json" | while read line; \
		do \
	        cd $(WORKFLOW)/../..; \
	        $(SBT) '; set javaOptions in Test  +="-D-Ddigest='$$line'"; test-only gov.nasa.jpl.imce.ontologies.workflow.runner.BatchRunProfileGenerator -- -Ddigest='$$line ; \
	    done )
