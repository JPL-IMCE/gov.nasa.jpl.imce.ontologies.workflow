#--
#
#    $HeadURL: https://sscae-cm.jpl.nasa.gov/svn/ruby-docbook/trunk/lib/jpl/docbook.rb $
#
#    $Revision: 103 $
#    $Date: 2015-05-04 10:55:22 -0700 (Mon, 04 May 2015) $
#
#    $Author: sjenkins $
#
#    Copyright (c) 2009 California Institute of Technology.
#    All rights reserved.
#
#++
#
require 'rexml/document'

include REXML

module REXML
  class Index < Element
    def initialize(title = 'Index', options = {})
      id = (options['xml:id'] ||= nil)
      userlevel = (options['userlevel'] ||= nil)
      super('index')
      add_attribute('xml:id', id) if id
      add_attribute('userlevel', userlevel) if userlevel
      options['xml:id'] = id ? id + '.title' : nil
      self << Title.new(title, options)
    end
  end

  class Para < Element
    def initialize(text = nil, options = {})
      userlevel = (options['userlevel'] ||= nil)
      super('para')
      add_attribute('userlevel', userlevel) if userlevel
      add_text(text) if text
    end
  end

  class SimPara < Element
    def initialize(text = nil, options = {})
      userlevel = (options['userlevel'] ||= nil)
      super('simpara')
      add_attribute('userlevel', userlevel) if userlevel
      add_text(text) if text
    end
  end

  class Article < Element
    def initialize()
      super('article')
    end
  end

  class Section < Element

    @@count = 0
    def initialize(title, options = {})
      options = options.dup
      options['xml:id'] ||= ("section%08d" % (@@count += 1))
      userlevel = (options['userlevel'] ||= nil)
      appendix = (options['appendix'] ||= false)
      super(appendix ? 'appendix' : 'section')
      add_attribute('xml:id', options['xml:id'])
      add_attribute('userlevel', userlevel) if userlevel
      options['xml:id'] = options['xml:id'] + '.title'
      self << Title.new(title, options)
    end
  end

  class Appendix < Section
    def initialize(title, options = {})
      options = options.dup
      options['appendix'] = true
      super
    end
  end

  class Abstract < Element
    def initialize(title, options = {})
      options = options.dup
      id = (options['xml:id'] ||= nil)
      userlevel = (options['userlevel'] ||= nil)
      super('abstract')
      add_attribute('xml:id', id) if id
      add_attribute('userlevel', userlevel) if userlevel
      options['xml:id'] = id ? id + '.title' : nil
      self << Title.new(title, options)
    end
  end

  class Term < Element
    def initialize(text = nil, options = {})
      super('term')
      self.text = text if text
    end
  end

  class Title < Element
    def initialize(title, options = {})
      id = (options['xml:id'] ||= nil)
      super('title')
      add_attribute('xml:id', id) if id
      self.text = title
    end
  end

  class Subtitle < Element
    def initialize(subtitle, options = {})
      id = (options['xml:id'] ||= nil)
      super('subtitle')
      add_attribute('xml:id', id) if id
      self.text = subtitle
    end
  end

  class IndexTerm < Element
    def initialize(pt, pk = nil, st = nil, sk = nil, tt = nil, tk = nil)
      super('indexterm')
      self << (pr = Element.new('primary'))
      pr.text = pt
      pr.add_attribute('sortas', pk) if pk
      if st
        self << (se = Element.new('secondary'))
        se.text = st
        se.add_attribute('sortas', sk) if sk
      end
      if tt
        self << (te = Element.new('tertiary'))
        te.text = tt
        te.add_attribute('sortas', tk) if tk
      end
    end
  end

  class PreferredIndexTerm < IndexTerm
    def initialize(pt, pk = nil, st = nil, sk = nil, tt = nil, tk = nil)
      super
      add_attribute('significance', 'preferred')
    end
  end

  class Figure < Element

    @@count = 0
    def initialize(title, options = {})
      options = options.dup
      options['xml:id'] ||= ("figure%08d" % (@@count += 1))
      userlevel = (options['userlevel'] ||= nil)
      landscape = (options['landscape'] ||= false)
      super('figure')
      add_attribute('xml:id', options['xml:id'])
      add_attribute('userlevel', userlevel) if userlevel
      add_attribute('orient', 'land') if landscape
      options['xml:id'] = options['xml:id'] + '.title'
      self << Title.new(title, options)
    end
  end

  class Table < Element

    @@count = 0
    def initialize(title, options = {})
      options = options.dup
      options['xml:id'] ||= ("table%08d" % (@@count += 1))
      userlevel = (options['userlevel'] ||= nil)
      landscape = (options['landscape'] ||= false)
      super('table')
      add_attribute('xml:id', options['xml:id'])
      add_attribute('userlevel', userlevel) if userlevel
      add_attribute('role', 'longtable')
      add_attribute('orient', 'land') if landscape
      options['xml:id'] = options['xml:id'] + '.title'
      self << Title.new(title, options)
    end
  end

  class Tgroup < Element
    def initialize(ncols)
      super('tgroup')
      add_attribute('cols', ncols.to_s)
    end
  end

  class Thead < Element
    def initialize
      super('thead')
    end
  end

  class Tbody < Element
    def initialize
      super('tbody')
    end
  end

  class Row < Element
    def initialize
      super('row')
    end
  end

  class Entry < Element
    def initialize
      super('entry')
    end
  end

  class Colspec < Element
    def initialize
      super('colspec')
    end
  end

  class ItemizedList < Element
    def initialize(options = {})
      super('itemizedlist')
      role = options['role']
      userlevel = options['userlevel']
      add_attribute('role', role) if role
      add_attribute('userlevel', userlevel) if userlevel
    end
  end

  class OrderedList < Element
    def initialize(options = {})
      super('orderedlist')
      role = options['role']
      userlevel = options['userlevel']
      numeration = options['numeration']
      add_attribute('role', role) if role
      add_attribute('userlevel', userlevel) if userlevel
      add_attribute('numeration', numeration) if numeration
    end
  end

  class VariableList < Element
    def initialize(options = {})
      super('variablelist')
      role = options['role']
      userlevel = options['userlevel']
      add_attribute('role', role) if role
      add_attribute('userlevel', userlevel) if userlevel
    end
  end

  class VarListEntry < Element
    def initialize(options = {})
      id = options['xml:id']
      super('varlistentry')
      add_attribute('xml:id', id) if id
    end
  end

  class ListItem < Element
    def initialize(options = {})
      super('listitem')
    end
  end

  class SimpleList < Element
    def initialize(type = 'inline')
      super('simplelist')
      add_attribute('type', type)
    end
  end

  class Member < Element
    def initialize
      super('member')
    end
  end

  class Literal < Element
    def initialize(text)
      super('literal')
      self.text = text
    end
  end

  class Emphasis < Element
    def initialize(text, options = {})
      super('emphasis')
      role = options['role']
      add_attribute('role', role) if role
      self.text = text
    end
  end

  class Xref < Element
    def initialize(linkend, endterm = nil, xrefstyle = nil)
      super('xref')
      add_attribute('linkend', linkend)
      add_attribute('endterm', endterm) if endterm
      add_attribute('xrefstyle', xrefstyle) if xrefstyle
    end
  end

  class MediaObject < Element
    def initialize(options = {})
      super('mediaobject')
      id = options['xml:id']
      add_attribute('xml:id', id) if id
    end
  end

  class Caption < Element
    def initialize(text, options = {})
      super('caption')
      self << Para.new(text, options)
    end
  end

  class ImageObject < Element
    def initialize(options = {})
      super('imageobject')
    end
  end

  class ImageData < Element
    def initialize(fileref, format, options = {})
      super('imagedata')
      add_attribute('fileref', fileref)
      add_attribute('format', format)
    end
  end

  class Link < Element
    def initialize(linkend = nil, endterm = nil, type = nil, xrefstyle = nil)
      super('link')
      add_attribute('linkend', linkend) if linkend
      add_attribute('endterm', endterm) if endterm
      add_attribute('type', type) if type
      add_attribute('xrefstyle', xrefstyle) if xrefstyle
    end
  end

  class ULink < Element
    def initialize(url, type = nil)
      super('ulink')
      add_attribute('url', url)
      add_attribute('type', type) if type
    end
  end

  class Classname < Element
    def initialize(name)
      super('classname')
      self << Text.new(name)
    end
  end

  class Property < Element
    def initialize(name)
      super('property')
      self << Text.new(name)
    end
  end

  class BeginPage < Element
    def initialize
      super('beginpage')
    end
  end

end

module DocBook

  RELEASE = '1.0.7'
  REVISION = '$Revision: 103 $'.split[1] # :nodoc:
  class Document
    def initialize(log)

      @elstack = []        # output element stack
      @log_debug = lambda { |m| log.debug(m) }
      @log_info = lambda { |m| log.info(m) }

      xmldoc = REXML::Document.new

      xmldoc << REXML::XMLDecl.new
      xmldoc << dt = DocType.new('article PUBLIC ' +
      '"-//OASIS//DTD DocBook XML V4.4//EN" ' +
      '"http://www.oasis-open.org/docbook/xml/4.4/docbookx.dtd"'
      )

      @elstack << xmldoc

    end

    attr_reader :template, :work_dir

    def article_start(title, subtitle, num, date, options = {})
      ar = Article.new(title, subtitle, num, date, options)
      @elstack.last << ar
      @elstack << ar
    end

    def article_end
      @elstack.pop
    end

    def section_start(title, options = {})
      se = Section.new(title, options)
      @elstack.last << se
      @elstack << se
    end

    def section_end
      @elstack.pop
    end
    alias :appendix_end :section_end

    def appendix_start(title, options = {})
      section_start(title, options.merge({'appendix' => true}))
    end

    def abstract_start(title, options = {})
      se = Abstract.new(title, options)
      @elstack.last << se
      @elstack << se
    end

    def abstract_end
      @elstack.pop
    end

    def formalpara(title, text, options = {})
      id = options['xml:id']
      userlevel = options['userlevel']
      fp = Element.new('formalpara')
      fp.add_attribute('xml:id', id) if id
      fp.add_attribute('userlevel', userlevel) if userlevel
      @elstack << fp
      title(title, id ? id + '.title' : nil)
      paragraphs('<para>' + text + '</para>', options)
      @elstack.pop
      @elstack.last << fp
      fp
    end

    def variablelist_start(options = {})
      vl = VariableList.new(options)
      @elstack.last << vl
      @elstack << vl
    end

    def variablelist_end
      @elstack.pop
    end

    def requirement_list(rqts, options = {})
      empty_text = options['empty_text']
      if rqts.empty? and empty_text
        paragraph(empty_text)
      else
        variablelist_start(options.merge({'role' => 'requirement'}))
        rqts.each do |rqt|
          rqt = rqt.individual
          requirement_description(rqt, options)
        end
        variablelist_end
      end
    end

    def requirement_description(number, title, desc = nil, rat = nil,
      options = {})
      le = VarListEntry.new(options)
      le << te = Term.new(number, options)
      te << PreferredIndexTerm.new('requirement', nil, number, number.sortas(8))
      le << li = ListItem.new(options)
      @elstack.push(li)
      fp = formalpara(title, desc)
      if options['rationale'] && rat
        variablelist_start(options.merge({'role' => 'rationale'}))
        rationale(rat)
        variablelist_end
      end
      @elstack.pop
      @elstack.last << le
    end

    def requirement_start(num, name, text, options = {})
      id = options['xml:id']
      le = VarListEntry.new(options)
      le.add_attribute('xml:id', id) if id
      le << te = Term.new(num, options)
      te.text = num
      le << li = ListItem.new(options)
      @elstack.push(li)
      formalpara(name, text)
      @elstack.pop
      @elstack.last << le
      @elstack << li
    end

    def requirement_end
      @elstack.pop
    end

    def rationale(text)
      le = VarListEntry.new
      le << te = Term.new('')
      te.text = ''
      le << li = ListItem.new
      @elstack.push(li)
      formalpara('', text)
      @elstack.pop
      @elstack.last << le
      le
    end

    def index_start(title, options = {})
      options['userlevel'] ||= nil
      options['auto'] ||= true
      ix = Index.new(title, options)
      @elstack.last << ix
      @elstack << ix
    end

    def index_end
      @elstack.pop
    end

    def self.parse_fragment(text)
      return [] unless text
      try = 0
      begin
        case try
        when 0
          t1 = text
        when 1
          t1 = '<para>' + t1 + '</para>'
        else
          raise "invalid DocBook content: #{text}"
        end
        t2 = '<section>' + t1 + '</section>'
        tree = REXML::Document.new(t2)
        raise ParseException, nil if tree[0].children.any? do |c|
          Text === c && c.to_s =~ /\S/
        end
      rescue ParseException
        try += 1
        retry
      end
      tree.get_elements('/section/para')
    end

    def paragraph(text = nil, options = {})
      options['userlevel'] ||= nil
      pa = Para.new(text.to_s, options)
      @elstack.last << pa
      pa
    end
    alias :para :paragraph

    def para_start(text = nil, options = {})
      pa = Para.new(text.to_s, options)
      @elstack.last << pa
      @elstack << pa
    end

    def para_end
      @elstack.pop
    end

    def paragraphs(text, options = {})
      options['userlevel'] ||= nil
      self.class.parse_fragment(text.to_s).each do |e|
        @elstack.last << e
      end
    end
    alias :paras :paragraphs

    def relationship_table(map, title, options = {})
      return unless map && map.keys.first
      @log_debug.call("relationship_table #{title}")
      options = options.dup

      colors = (options['colors'] ||= %w{ #FFFFFF })
      ci = ColorIterator.new(colors)

      key_text_ncols = map.keys.first['rm_text'].length
      value_text_ncols = map.values.first.to_a.first['rm_text'].length

      comment("begin relationship table: #{title}")
      table_start(title, options)

      # Set layout parameters.

      ncols = 2
      span = nil
      case key_text_ncols
      when 1
        case value_text_ncols
        when 1
          span = [1, 2]
        when 2
          ncols = 3
          span = [1, 3]
        end
      when 2
        case value_text_ncols
        when 1
        when 2
          ncols = 3
          span = [2, 3]
        end
      end

      # Define table group with column identifiers.

      tgroup_start(ncols)
      ncols.times do |cn|
        colnum = cn + 1
        colspec({'colname' => "c#{colnum}", 'align' => 'left', 'colnum' => colnum.to_s})
      end

      # Generate table body.

      tbody_start
      unless map.empty?
        map.keys.sort_by { |i| i['rm_sort_key'] } .uniq.each do |k|
          first = true
          row_start
          k['rm_text'].each_with_index do |t, i|
            if span && (i + 1 == key_text_ncols)
              entry_start(t, {'namest' => "c#{span[0]}", 'nameend' => "c#{span[1]}"})
            else
              entry_start(t)
            end
            if first
              indexterm(*k['rm_index_terms'])
              first = false
            end
            bgcolor('dbhtml', ci.value)
            entry_end
          end
          row_end
          map[k].sort_by { |i| i['rm_sort_key'] } .uniq.each do |v|
            first = true
            row_start
            entry_start
            bgcolor('dbhtml', ci.value)
            entry_end
            v['rm_text'].each do |t|
              entry_start(t)
              bgcolor('dbhtml', ci.value)
              if first
                indexterm(*v['rm_index_terms'])
                first = false
              end
              entry_end
            end
            row_end
          end
          ci.step
        end
      else
        row_start
        element
        row_end
      end
      tbody_end
      tgroup_end
      table_end
      beginpage
      comment("end relationship table: #{title}")
    end

    def relationship_matrix(map, title, options = {})
      return unless map && map.keys.first
      @log_debug.call("relationship_matrix #{title}")
      options = options.dup

      colors = (options['colors'] ||= %w{ #FFFFFF })
      ci = ColorIterator.new(colors)

      key_text_ncols = map.keys.first['rm_text'].length
      values = map.values.map { |i| i.to_a } .flatten.sort_by { |i| i['rm_sort_key'] }.uniq
      value_text_ncols = values.length

      comment("begin relationship matrix: #{title}")
      table_start(title, options)

      # Define table group with column identifiers.

      ncols = key_text_ncols + value_text_ncols
      tgroup_start(ncols)
      cn = 0
      key_text_ncols.times do |i|
        colnum = cn += 1
        colspec({'colname' => "c#{colnum}", 'align' => 'left',
          'colnum' => colnum.to_s})
      end
      value_text_ncols.times do |i|
        colnum = cn += 1
        colspec({'colname' => "c#{colnum}", 'align' => 'center',
          'colnum' => colnum.to_s})
      end

      # Define table head.

      thead_start
      value_text_ncols.downto(1) do |r|
        row_start
        key_text_ncols.times do |c|
          entry
        end
        values.each do |v|
          entry(v['rm_text'][r - 1], {'rotate' => '1'})
        end
        row_end
      end
      thead_end

      # Generate table body.

      tbody_start
      unless map.empty?
        map.keys.sort_by { |i| i['rm_sort_key'] } .uniq.each do |k|
          first = true
          row_start
          k['rm_text'].each do |t|
            entry_start(t)
            bgcolor('dbhtml', ci.value)
            if first
              indexterm(*k['rm_index_terms'])
              first = false
            end
            entry_end
          end
          values.each do |v|
            if map[k].any? { |x| x.eql?(v) }
              sym = '&#8226;'
              indterm = v['rm_index_terms']
            else
              sym = ''
              indterm = nil
            end
            entry_start(sym, {:raw => true})
            indexterm(*indterm) if indterm
            bgcolor('dbhtml', ci.value)
            entry_end
          end
          ci.step
          row_end
        end
      else
        row_start
        element
        row_end
      end
      tbody_end
      tgroup_end
      table_end
      beginpage
      comment("end relationship matrix: #{title}")
    end

    def orderedlist_start(options = {})
      ol = OrderedList.new(options)
      @elstack.last << ol
      @elstack << ol
    end

    def orderedlist_end
      @elstack.pop
    end

    def listitem_start(options = {})
      li = ListItem.new(options)
      @elstack.last << li
      @elstack << li
    end

    def listitem_end
      @elstack.pop
    end

    def varlistentry_start(options = {})
      le = VarListEntry.new(options)
      @elstack.last << le
      @elstack << le
    end

    def varlistentry_end
      @elstack.pop
    end

    def term_start(term, options = {})
      te = Term.new(term, options)
      @elstack.last << te
      @elstack << te
    end

    def term_end
      @elstack.pop
    end

    def term(term, options = {})
      term_start(term, options)
      term_end
    end

    def write(stream, indent = -1)
      @elstack[0].write(stream, indent)
    end

    def comment(text)
      @elstack.last << Comment.new(text)
    end

    def figure_start(title, options = {})
      @elstack.last << ta = Figure.new(title, options)
      @elstack << ta
    end

    def figure_end
      @elstack.pop
    end

    def table_start(title, options = {})
      @elstack.last << ta = Table.new(title, options)
      @elstack << ta
    end

    def table_end
      @elstack.pop
    end

    def tgroup_start(ncols)
      @elstack.last << tg = Tgroup.new(ncols)
      @elstack << tg
    end

    def tgroup_end
      @elstack.pop
    end

    def colspec(options = {})
      @elstack.last << cs = Element.new('colspec')
      options.each { |k, v| cs.add_attribute(k, v) }
    end

    def thead_start
      @elstack.last << th = Thead.new
      @elstack << th
    end

    def thead_end
      @elstack.pop
    end

    def row_start
      @elstack.last << ro = Row.new
      @elstack << ro
    end

    def row_end
      @elstack.pop
    end

    def entry_start(text = nil, options = {})
      raw = options[:raw]
      options.delete(:raw)
      @elstack.last << en = Entry.new
      en << Text.new(text.to_s, false, false, raw) if text
      options.each { |k, v| en.add_attribute(k, v) }
      @elstack << en
    end

    def entry_end
      @elstack.pop
    end

    def entry(text = nil, options = {})
      entry_start(text, options)
      entry_end
    end

    def tbody_start
      @elstack.last << tb = Tbody.new
      @elstack << tb
    end

    def tbody_end
      @elstack.pop
    end

    def instruction(name, content = nil)
      @elstack.last << Instruction.new(name, content)
    end

    # Not really a DocBook construct, but useful.

    def bgcolor(name, color)
      instruction(name, "bgcolor='#{color}'")
    end

    def beginpage
      @elstack.last << BeginPage.new
    end

    def indexterm(pt, pk = nil, st = nil, sk = nil, tt = nil, tk = nil)
      @elstack.last << IndexTerm.new(pt, pk, st, sk, tt, tk) if pt
    end

    def preferredindexterm(pt, pk = nil, st = nil, sk = nil, tt = nil, tk = nil)
      @elstack.last << PreferredIndexTerm.new(pt, pk, st, sk, tt, tk) if pt
    end

    def ulink_start(url, type = nil)
      ul = ULink.new(url, type)
      @elstack.last << ul
      @elstack << ul
    end

    def ulink_end
      @elstack.pop
    end

    def ulink(url, type = nil)
      ulink_start(url, type)
      ulink_end
    end

    def text(text)
      @elstack.last << Text.new(text.to_s)
    end

  end

  class ColorIterator
    def initialize(colors = %w{ #FFFFFF })
      @colors = colors
      reset
    end

    def reset
      @index = 0
    end

    def step
      @index += 1
      @index = 0 if @index >= @colors.length
    end

    def value
      @colors[@index]
    end

  end

end
