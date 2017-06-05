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

name 'every ontology declares an abstract unmapped top datatype property'

query %q{

  <%= @namespace_defs %>

  select distinct ?ontology ?exists ?isAbstract ?noMapping

  <%= @from_named_clauses_by_group['named'] %>

  where {
    graph ?graph {
      ?ontology rdf:type owl:Ontology .
    }

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topDataProperty")) as ?tdp)
    bind(exists { graph ?graph { ?tdp rdf:type owl:DatatypeProperty } } as ?exists)
    bind(?exists && exists { graph ?graph { ?tdp annotation:isAbstract true } } as ?isAbstract)
    bind(?exists && exists { graph ?graph { ?tdp annotation:noMapping true } } as ?noMapping)

    filter (
      <%= (@ontologies_by_group['named']  - @ontologies_by_group['named-embedding'] - @ontologies_by_group['named-view']).map { |o| o.to_uriref }.equal_any?('?graph')
    %>)

  }
  order by ?ontology
}

prologue do
  @rules = {
    :exists => 'no topDataProperty',
    :isAbstract => 'topDataProperty missing annotation:isAbstract',
    :noMapping => 'topDataProperty missing annotation:noMapping'
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

case_name { |r| r.ontology }