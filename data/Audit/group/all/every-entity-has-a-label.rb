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

name 'every entity has a label'

prologue do
  @owl_object_property = @namespace_by_prefix['owl'] + 'ObjectProperty'
  @owl_datatype_property = @namespace_by_prefix['owl'] + 'DatatypeProperty'
    
  @result_by_entity_by_graph = Hash.new { |h, k| h[k] = {} }
end
  
query %q{
  
  <%= @namespace_defs %>
  
  select distinct ?graph ?entity ?rdf_type ?label
  
  <%= @from_clauses_by_group['named-no-embedding'] %>
  <%= @from_clauses_by_group_by_type['named-no-embedding']['ClassEntailments'] %>
  <%= @from_clauses_by_group_by_type['named-no-embedding']['PropertyEntailments'] %>
    
  <%= @from_named_clauses_by_group['named-no-embedding'] %>
  <%= @from_named_clauses_by_group_by_type['named-no-embedding']['ClassEntailments'] %>
  <%= @from_named_clauses_by_group_by_type['named-no-embedding']['PropertyEntailments'] %>
  
  where {
    graph ?graph {
      ?entity rdf:type ?rdf_type .
      optional {
        ?entity rdfs:label ?label
      }
    }
    ?entity ?sub_of ?top_entity .

    bind(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#Entity") as ?top_entity_string)
    bind(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#ReifiedObjectProperty") as ?top_rop_string)
    bind(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#ReifiedStructuredDataProperty") as ?top_rsdp_string)
    bind(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#StructuredDatatype") as ?top_sdt_string)
    bind(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topReifiedObjectProperty") as ?htop_rop_string)
    bind(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topDataProperty") as ?htop_tdp_string)
   
    filter (
         !isblank(?entity)
      && ?entity != ?top_entity
      && (
        ?rdf_type = owl:Class && ?sub_of = rdfs:subClassOf && (
             str(?top_entity) = ?top_entity_string
          || str(?top_entity) = ?top_rop_string
          || str(?top_entity) = ?top_sdt_string
          || str(?top_entity) = ?top_rdsp_string
        ) || ?rdf_type = owl:ObjectProperty && ?sub_of = rdfs:subPropertyOf && (
             str(?top_entity) = ?htop_rop_string
        ) || ?rdf_type = owl:DatatypeProperty && ?sub_of = rdfs:subPropertyOf && (
             str(?top_entity) = ?htop_tdp_string
        )
      )
    )
  }
  order by ?graph ?entity
}

filter do |r, emit|
  if r
    graph = r.graph.to_s
    entity = r.entity
    unless os = @result_by_entity_by_graph[graph][entity]
      os = @result_by_entity_by_graph[graph][entity] = QuerySolutionMap.new
      os.graph = graph
      os.entity = entity
      os.rdf_type = r.rdf_type.to_string
      os.labels = Set.new
    end
    os.labels << r.label.get_string if r.label
  else
    @result_by_entity_by_graph.each_value do |result_by_entity|
      result_by_entity.each_value do |result|
        emit.call(result)
      end
    end
  end
end

case_name { |r| r.entity.to_qname(@namespace_by_prefix) }
predicate do |r|
  label = r.entity.get_local_name.gsub(/([a-z])([A-Z])/, '\1 \2')
  case r.rdf_type
  when @owl_object_property, @owl_datatype_property
    label.downcase!
  end
  if r.labels.include?(label)
    [true, nil]
  else
    [false, "ontology #{r.graph} entity #{r.entity} lacks label '#{label}'."]
  end
end