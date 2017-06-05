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

name 'every class embedded in UML'

query %q{

  <%= @namespace_defs %>
  
  select distinct ?klass ?isAbstract ?embedded ?embedded_concrete ?embedded_ok
  
  <%= @from_clauses_by_group['named'] %>
  <%= @from_named_clauses_by_group['named'] %>
  <%= @from_clauses_by_group['imported'] %>
  <%= @from_clauses_by_group_by_type['named']['ClassEntailments'] %>
  <%= @from_clauses_by_group_by_type['imported']['ClassEntailments'] %>
  <%= @from_named_clauses_by_group['omg'] %>

  where {
  
    graph ?graph { ?klass rdf:type owl:Class . }
  
    bind(exists {
      ?klass rdfs:subClassOf ?metaclass .
      ?metaclass annotation:isMetaclass true.
    } as ?embedded_metaclass)
  
    bind(exists {
      ?klass rdfs:subClassOf ?stereotypeMetaclass .
      ?stereotypeMetaclass annotation:isStereotypeMetaclass true.
    } as ?embedded_stereotypeMetaclass)
  
    bind(exists {
      ?klass rdfs:subClassOf ?metaclassSpecificExtension .
      ?metaclassSpecificExtension annotation:isMetaclassSpecificExtension true.
    } as ?embedded_metaclassSpecificExtension)
  
    bind(exists { ?klass annotation:isAbstract true } as ?isAbstract )
    bind(exists { ?klass rdfs:subClassOf UML:Element } as ?embedded_element)
    bind(exists {
      ?klass rdfs:subClassOf ?omg_superclass .
      graph ?omg_graph { ?omg_superclass rdf:type owl:Class } .
      filter(
           not exists { ?omg_superclass annotation:isAbstract true }
        && <%= @ontologies_by_group['omg'].map { |o| o.to_uriref }.equal_any?('?omg_graph') %>
      )
    } as ?omg_superclass_concrete)
    bind(
      (?embedded_metaclass || ?embedded_stereotypeMetaclass || ?embedded_metaclassSpecificExtension
        || ?omg_superclass_concrete)
      as ?embedded_concrete
    )
    bind(exists { ?klass rdfs:subClassOf owl2-mof2-backbone:ReifiedObjectProperty } as ?reified_op)
    bind(exists { ?klass rdfs:subClassOf owl2-mof2-backbone:ReifiedStructuredDataProperty } as ?reified_sdp)
    bind(?reified_op || ?reified_sdp as ?reified)
    bind((?reified || ?embedded_element) as ?embedded)
    bind((?reified || (?embedded_element && (?isAbstract || ?embedded_concrete))) as ?embedded_ok)
  
    filter (
         <%= @ontologies_by_group['named'].map { |o| o.to_uriref }.equal_any?('?graph') %>
      && ?klass != owl:Thing
      && ?klass != owl:Nothing
      && not exists { ?klass annotation:noMapping true }
    )
  }
  order by ?klass
}

case_name { |r| r.klass.to_qname(@namespace_by_prefix) }
  
predicate do |r|
  embedded_ok = r.embedded_ok.true?
  isAbstract = r.isAbstract.true?
  embedded_concrete = r.embedded_concrete.true?
  text = nil
  unless embedded_ok
    if isAbstract || embedded_concrete
      text = 'not embedded'
    else
      text = 'concrete class without concrete embedding'
    end
  end
  [embedded_ok, text]
end
