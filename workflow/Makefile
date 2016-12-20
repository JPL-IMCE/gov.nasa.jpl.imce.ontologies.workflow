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

clean:
	/bin/rm -rf $(WORKFLOW)
	mkdir $(WORKFLOW)
	mkdir $(WORKFLOW)/artifacts


$(WORKFLOW)/Makefile.erb: $(WORKFLOW_SOURCES)/Makefile.erb
	cp $< $@

$(WORKFLOW)/log4j.properties: $(WORKFLOW_SOURCES)/log4j.properties
	cp $< $@

$(WORKFLOW)/logging.properties: $(WORKFLOW_SOURCES)/logging.properties
	cp $< $@

bootstrap: $(WORKFLOW)/Makefile

$(WORKFLOW)/Makefile: $(WORKFLOW_SOURCES)/Makefile.erb $(WORKFLOW)/logging.properties $(WORKFLOW)/log4j.properties $(WORKFLOW)/Makefile.erb
	@echo $< '->' $@
	(cd $(WORKFLOW); $(JRUBY_CMD) $(MAKE_MAKEFILE) $< > $@.tmp) && mv $@.tmp $@

$(WORKFLOW)/.symlinks:
	ln -s $(ONTOLOGIES)/* $(WORKFLOW)
	touch $(WORKFLOW)/.symlinks

dependencies: bootstrap $(WORKFLOW)/.symlinks
	(cd $(WORKFLOW); nice make ${PARALLEL_MAKE_OPTS} dependencies)

validate: dependencies
	(cd $(WORKFLOW); nice make ${PARALLEL_MAKE_OPTS} validate-xml validate-owl)

loadprod: validate
	(cd $(WORKFLOW); nice make ${PARALLEL_MAKE_OPTS} load-production artifacts)