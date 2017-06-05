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

name 'no unmapped class is a subclass of a mapped class'

query %q{

  <%= @namespace_defs %>

  select distinct ?subclass ?superclass ?audit_case_ok

  <%= @from_clauses_by_group['named'] %>

  where {
    ?subclass rdf:type owl:Class .
    ?subclass annotation:noMapping true .
    ?subclass rdfs:subClassOf ?superclass .
    ?superclass rdf:type owl:Class .
    
    bind(exists { ?superclass annotation:noMapping true } as ?audit_case_ok)
      
    filter (?superclass != ?subclass && ?superclass != owl:Thing)
  }
}

case_name do |r|
  "#{r.subclass.to_qname(@namespace_by_prefix)}" +
    " subclass of " +
    "#{r.superclass.to_qname(@namespace_by_prefix)}"
end