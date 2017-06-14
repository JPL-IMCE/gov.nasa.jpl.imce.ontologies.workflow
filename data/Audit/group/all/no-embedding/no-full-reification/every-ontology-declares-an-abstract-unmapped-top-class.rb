#--
#
#    $HeadURL$
#
#    $LastChangedRevision$
#    $LastChangedDate$
#
#    $LastChangedBy$
#
#    Copyright (c) 2008-2014 California Institute of Technology.
#    All rights reserved.
#
#++

name 'every ontology declares an abstract unmapped top class'

query %q{
  
  <%= @namespace_defs %>
  
  select distinct ?ontology ?graph
                  ?thingExists ?thingIsAbstract ?thingNoMapping
                  ?aspectExists ?aspectIsAbstract ?aspectSubThing
                  ?entityExists ?entityIsAbstract ?entitySubThing
                  ?ropExists ?ropIsAbstract ?ropSubThing
                  ?rsdpExists ?rsdpIsAbstract ?sdtSubThing
                  ?sdtExists ?sdtIsAbstract ?rsdpSubThing
                  
  
  <%= @from_named_clauses_by_group['named'] %>
  
  where {
    graph ?graph {
      ?ontology rdf:type owl:Ontology
    }
    
    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#Thing")) as ?thing)
    bind(exists { graph ?graph { ?thing rdf:type owl:Class } } as ?thingExists)
    bind(?thingExists && exists { graph ?graph { ?thing annotation:isAbstract true } } as ?thingIsAbstract)
    bind(?thingExists && exists { graph ?graph { ?thing annotation:noMapping true } } as ?thingNoMapping)

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#Aspect")) as ?aspect)
    bind(exists { graph ?graph { ?aspect rdf:type owl:Class } } as ?aspectExists)
    bind(?aspectExists && exists { graph ?graph { ?aspect annotation:isAbstract true } } as ?aspectIsAbstract)
    bind(?aspectExists && exists { graph ?graph { ?aspect rdfs:subClassOf ?thing } } as ?aspectSubThing)

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#Entity")) as ?entity)
    bind(exists { graph ?graph { ?entity rdf:type owl:Class } } as ?entityExists)
    bind(?entityExists && exists { graph ?graph { ?entity annotation:isAbstract true } } as ?entityIsAbstract)
    bind(?entityExists && exists { graph ?graph { ?entity rdfs:subClassOf ?thing } } as ?entitySubThing)

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#ReifiedObjectProperty")) as ?rop)
    bind(exists { graph ?graph { ?rop rdf:type owl:Class } } as ?ropExists)
    bind(?ropExists && exists { graph ?graph { ?rop annotation:isAbstract true } } as ?ropIsAbstract)
    bind(?ropExists && exists { graph ?graph { ?rop rdfs:subClassOf ?thing } } as ?ropSubThing)

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#ReifiedStructuredDataProperty")) as ?rsdp)
    bind(exists { graph ?graph { ?rsdp rdf:type owl:Class } } as ?rsdpExists)
    bind(?rsdpExists && exists { graph ?graph { ?rsdp annotation:isAbstract true } } as ?rsdpIsAbstract)
    bind(?rsdpExists && exists { graph ?graph { ?rsdp rdfs:subClassOf ?thing } } as ?rsdpSubThing)

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#StructuredDatatype")) as ?sdt)
    bind(exists { graph ?graph { ?sdt rdf:type owl:Class } } as ?sdtExists)
    bind(?sdtExists && exists { graph ?graph { ?sdt annotation:isAbstract true } } as ?sdtIsAbstract)
    bind(?sdtExists && exists { graph ?graph { ?sdt rdfs:subClassOf ?thing } } as ?sdtSubThing)

    filter (
      <%= (@ontologies_by_group['named']  - @ontologies_by_group['named-embedding'] - @ontologies_by_group['named-view']).map { |o| o.to_uriref }.equal_any?('?graph')
    %>)
        
  }
  order by ?ontology
}

prologue do
  @rules = {
    :thingExists => 'no Thing',
    :thingIsAbstract => 'Thing missing annotation:isAbstract',
    :thingNoMapping => 'Thing missing annotation:noMapping',
    :aspectExists => 'no Aspect',
    :aspectIsAbstract => 'Aspect missing annotation:isAbstract',
    :aspectSubThing => 'Aspect not subclass of Thing',
    :entityExists => 'no Entity',
    :entityIsAbstract => 'Entity missing annotation:isAbstract',
    :entitySubThing => 'Entity not subclass of Thing',
    :ropExists => 'no ReifiedObjectProperty',
    :ropIsAbstract => 'ReifiedObjectProperty missing annotation:isAbstract',
    :ropSubThing => 'ReifiedObjectProperty not subclass of Thing',
    :rsdpExists => 'no ReifiedStructuredDataProperty',
    :rsdpIsAbstract => 'ReifiedStructuredDataProperty missing annotation:isAbstract',
    :rsdpSubThing => 'ReifiedStructuredDataProperty not subclass of Thing',
    :sdtExists => 'no StructuredDatatype',
    :sdtIsAbstract => 'StructuredDatatype missing annotation:isAbstract',
    :sdtSubThing => 'StructuredDatatype not subclass of Thing',
  }
end

predicate do |r|
  text = nil
  msg = @rules.inject([]) do |memo, kv|
    method, msg = *kv
    memo << msg + '.' unless r.send(method).true?
    memo
  end.join(' ')
  msg.empty? ? [true, nil] : [false, msg]
end

case_name { |r|  r.ontology }