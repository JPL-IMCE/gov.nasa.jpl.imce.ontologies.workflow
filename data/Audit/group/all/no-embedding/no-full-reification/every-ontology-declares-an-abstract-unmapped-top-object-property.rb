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

name 'every ontology declares an abstract unmapped top object property'

query %q{

  <%= @namespace_defs %>

  select distinct ?ontology ?top_exists ?top_isAbstract ?top_noMapping
    ?toprop_exists ?toprop_isAbstract ?toprop_noMapping ?toprop_subprop
    ?toprops_exists ?toprops_isAbstract ?toprops_noMapping ?toprops_subprop
    ?topropt_exists ?topropt_isAbstract ?topropt_noMapping ?topropt_subprop
    ?topuop_exists ?topuop_isAbstract ?topuop_noMapping ?topuop_subprop
    ?toprsdp_exists ?toprsdp_isAbstract ?toprsdp_noMapping ?toprsdp_subprop
    ?toprsdps_exists ?toprsdps_isAbstract ?toprsdps_noMapping ?toprsdps_subprop
    ?toprsdpt_exists ?toprsdpt_isAbstract ?toprsdpt_noMapping ?toprsdpt_subprop

  <%= @from_named_clauses_by_group['named'] %>

  where {
    graph ?graph {
      ?ontology rdf:type owl:Ontology .
    }

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topObjectProperty")) as ?top)
    bind(exists { graph ?graph { ?top rdf:type owl:ObjectProperty } } as ?top_exists)
    bind(?top_exists && exists { graph ?graph { ?top annotation:isAbstract true } } as ?top_isAbstract)
    bind(?top_exists && exists { graph ?graph { ?top annotation:noMapping true } } as ?top_noMapping)

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topReifiedObjectProperty")) as ?toprop)
    bind(exists { graph ?graph { ?toprop rdf:type owl:ObjectProperty } } as ?toprop_exists)
    bind(?toprop_exists && exists { graph ?graph { ?toprop annotation:isAbstract true } } as ?toprop_isAbstract)
    bind(?toprop_exists && exists { graph ?graph { ?toprop annotation:noMapping true } } as ?toprop_noMapping)
    bind(?toprop_exists && exists { graph ?graph { ?toprop rdfs:subPropertyOf ?top } } as ?toprop_subprop)

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topReifiedObjectPropertySource")) as ?toprops)
    bind(exists { graph ?graph { ?toprops rdf:type owl:ObjectProperty } } as ?toprops_exists)
    bind(?toprops_exists && exists { graph ?graph { ?toprops annotation:isAbstract true } } as ?toprops_isAbstract)
    bind(?toprops_exists && exists { graph ?graph { ?toprops annotation:noMapping true } } as ?toprops_noMapping)
    bind(?toprops_exists && exists { graph ?graph { ?toprops rdfs:subPropertyOf ?top } } as ?toprops_subprop)
    
    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topReifiedObjectPropertyTarget")) as ?topropt)
    bind(exists { graph ?graph { ?topropt rdf:type owl:ObjectProperty } } as ?topropt_exists)
    bind(?topropt_exists && exists { graph ?graph { ?topropt annotation:isAbstract true } } as ?topropt_isAbstract)
    bind(?topropt_exists && exists { graph ?graph { ?topropt annotation:noMapping true } } as ?topropt_noMapping)
    bind(?topropt_exists && exists { graph ?graph { ?topropt rdfs:subPropertyOf ?top } } as ?topropt_subprop)

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topUnreifiedObjectProperty")) as ?topuop)
    bind(exists { graph ?graph { ?topuop rdf:type owl:ObjectProperty } } as ?topuop_exists)
    bind(?topuop_exists && exists { graph ?graph { ?topuop annotation:isAbstract true } } as ?topuop_isAbstract)
    bind(?topuop_exists && exists { graph ?graph { ?topuop annotation:noMapping true } } as ?topuop_noMapping)
    bind(?topuop_exists && exists { graph ?graph { ?topuop rdfs:subPropertyOf ?top } } as ?topuop_subprop)

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topReifiedStructuredDataProperty")) as ?toprsdp)
    bind(exists { graph ?graph { ?toprsdp rdf:type owl:ObjectProperty } } as ?toprsdp_exists)
    bind(?toprsdp_exists && exists { graph ?graph { ?toprsdp annotation:isAbstract true } } as ?toprsdp_isAbstract)
    bind(?toprsdp_exists && exists { graph ?graph { ?toprsdp annotation:noMapping true } } as ?toprsdp_noMapping)
    bind(?toprsdp_exists && exists { graph ?graph { ?toprsdp rdfs:subPropertyOf ?top } } as ?toprsdp_subprop)

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topReifiedStructuredDataPropertySource")) as ?toprsdps)
    bind(exists { graph ?graph { ?toprsdps rdf:type owl:ObjectProperty } } as ?toprsdps_exists)
    bind(?toprsdps_exists && exists { graph ?graph { ?toprsdps annotation:isAbstract true } } as ?toprsdps_isAbstract)
    bind(?toprsdps_exists && exists { graph ?graph { ?toprsdps annotation:noMapping true } } as ?toprsdps_noMapping)
    bind(?toprsdps_exists && exists { graph ?graph { ?toprsdps rdfs:subPropertyOf ?top } } as ?toprsdps_subprop)
    
    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topReifiedStructuredDataPropertyTarget")) as ?toprsdpt)
    bind(exists { graph ?graph { ?toprsdpt rdf:type owl:ObjectProperty } } as ?toprsdpt_exists)
    bind(?toprsdpt_exists && exists { graph ?graph { ?toprsdpt annotation:isAbstract true } } as ?toprsdpt_isAbstract)
    bind(?toprsdpt_exists && exists { graph ?graph { ?toprsdpt annotation:noMapping true } } as ?toprsdpt_noMapping)
    bind(?toprsdpt_exists && exists { graph ?graph { ?toprsdpt rdfs:subPropertyOf ?top } } as ?toprsdpt_subprop)

    filter (
      <%= (@ontologies_by_group['named']  - @ontologies_by_group['named-embedding'] - @ontologies_by_group['named-view']).map { |o| o.to_uriref }.equal_any?('?graph')
    %>)
        
  }
  order by ?ontology
}

prologue do
  @rules = {
    :top_exists => 'no topObjectProperty',
    :top_isAbstract => 'topObjectProperty missing annotation:isAbstract',
    :top_noMapping => 'topObjectProperty missing annotation:noMapping',
    :toprop_exists => 'no topReifiedObjectProperty',
    :toprop_isAbstract => 'topReifiedObjectProperty missing annotation:isAbstract',
    :toprop_noMapping => 'topReifiedObjectProperty missing annotation:noMapping',
    :toprop_subprop => 'topReifiedObjectProperty not subproperty of topObjectProperty',
    :toprops_exists => 'no topReifiedObjectPropertySource',
    :toprops_isAbstract => 'topReifiedObjectPropertySource missing annotation:isAbstract',
    :toprops_noMapping => 'topReifiedObjectPropertySource missing annotation:noMapping',
    :toprops_subprop => 'topReifiedObjectPropertySource not subproperty of topObjectProperty',
    :topropt_exists => 'no topReifiedObjectPropertyTarget',
    :topropt_isAbstract => 'topReifiedObjectPropertyTarget missing annotation:isAbstract',
    :topropt_noMapping => 'topReifiedObjectPropertyTarget missing annotation:noMapping',
    :topropt_subprop => 'topReifiedObjectPropertyTarget not subproperty of topObjectProperty',
    :topuop_exists => 'no topUnreifiedObjectProperty',
    :topuop_isAbstract => 'topUnreifiedObjectProperty missing annotation:isAbstract',
    :topuop_noMapping => 'topUnreifiedObjectProperty missing annotation:noMapping',
    :topuop_subprop => 'topUnreifiedObjectProperty not subproperty of topObjectProperty',
    :toprsdp_exists => 'no topReifiedStructuredDataProperty',
    :toprsdp_isAbstract => 'topReifiedStructuredDataProperty missing annotation:isAbstract',
    :toprsdp_noMapping => 'topReifiedStructuredDataProperty missing annotation:noMapping',
    :toprsdp_subprop => 'topReifiedStructuredDataProperty not subproperty of topObjectProperty',
    :toprsdps_exists => 'no topReifiedStructuredDataPropertySource',
    :toprsdps_isAbstract => 'topReifiedStructuredDataPropertySource missing annotation:isAbstract',
    :toprsdps_noMapping => 'topReifiedStructuredDataPropertySource missing annotation:noMapping',
    :toprsdps_subprop => 'topReifiedStructuredDataPropertySource not subproperty of topObjectProperty',
    :toprsdpt_exists => 'no topReifiedStructuredDataPropertyTarget',
    :toprsdpt_isAbstract => 'topReifiedStructuredDataPropertyTarget missing annotation:isAbstract',
    :toprsdpt_noMapping => 'topReifiedStructuredDataPropertyTarget missing annotation:noMapping',
    :toprsdpt_subprop => 'topReifiedStructuredDataPropertyTarget not subproperty of topObjectProperty',
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