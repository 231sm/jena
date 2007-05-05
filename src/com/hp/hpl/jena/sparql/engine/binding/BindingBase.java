/*
 * (c) Copyright 2004, 2005, 2006, 2007 Hewlett-Packard Development Company, LP
 * [See end of file]
 */

package com.hp.hpl.jena.sparql.engine.binding;

import java.util.* ;

import org.apache.commons.logging.*;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.util.FmtUtils;
import com.hp.hpl.jena.util.iterator.ConcatenatedIterator;

/** Machinary encapsulating a mapping from a name to a value.
 * 
 * @author   Andy Seaborne
 * @version  $Id: BindingBase.java,v 1.1 2007/02/06 17:06:05 andy_seaborne Exp $
 */


abstract public class BindingBase implements Binding
{
    static Log log = LogFactory.getLog(BindingBase.class) ;
    
    static boolean CHECKING = true ;
    static boolean UNIQUE_NAMES_CHECK = true ;
    
    // This is a set of bindings, each binding being one pair (var, value).
    Binding parent ;
    
    // Tracking children is for flexiblity.
    
    // It is not needed for flatten results sets (i.e. with nulls in)
    // but is needed for nested result set that record subqueries.
    // and have nested results.
    
    // But keeping the child reference means that used bindings are not freed
    // to the GC until the parent is freed and hence the root is finished with -
    // which is all results.
    
    
    List children = new ArrayList() ; 
    
    protected BindingBase(Binding _parent)
    {
        parent = _parent ;
    }
        
    public Iterator getChildren() { return children.listIterator() ; }
    public Binding getParent() { return parent ; }

    private void addChild(BindingBase child) {  children.add(child) ; }
    
    /** Add a (var,value) - the value is never null */
    final public void add(Var var, Node node)
    { 
        if ( node == null )
        {
            log.warn("Binding.add: null value - ignored") ;
            return ;
        }
        checkAdd(var, node) ;
        add1(var, node) ;
    }
        
    protected abstract void add1(Var name, Node node) ;

    /** Iterate over all the names of variables. */
    final public Iterator vars()
    {
        Iterator iter = vars1() ;
        if ( parent != null )
            iter = new ConcatenatedIterator(parent.vars(), iter ) ;
        return iter ;
    }
    protected abstract Iterator vars1() ;
    
    final public int size()
    {
        int x = size1() ;
        if ( parent != null )
            x = x + parent.size() ;
        return x ;
    }
    
    protected abstract int size1() ;
    
    public boolean isEmpty() { return size() == 0 ; }
    
    /** Test whether a name is bound to some object */
    public boolean contains(Var var)
    {
        if ( contains1(var) )
            return true ;
        if ( parent == null )
            return false ; 
        return parent.contains(var) ; 
    }
    
    protected abstract boolean contains1(Var var) ;

    /** Return the object bound to a name, or null */
    final public Node get(Var var)
    {
        Node node = get1(var) ;
        
        if ( node != null )
            return node ;
        
        if ( parent == null )
            return null ; 
        
        return parent.get(var) ; 

    }
    protected abstract Node get1(Var var) ;
    
    public String toString()
    {
        StringBuffer sbuff = new StringBuffer() ;
        format1(sbuff) ;

        if ( parent != null )
        {
            String tmp = parent.toString() ;
            if ( tmp != null && (tmp.length() != 0 ) )
            {
                sbuff.append(" -> ") ;
                sbuff.append(tmp) ;
            }
        }
        return sbuff.toString() ;
    }

    // Do one level of binding 
    public void format1(StringBuffer sbuff)
    {
        String sep = "" ;
        for ( Iterator iter = vars1() ; iter.hasNext() ; ) 
        {
            Object obj = iter.next() ;
            Var var = (Var)obj ;
            
            //String name = (String)iter.next() ;
            // Skip system variables.
            if ( Var.isSystemVar(var) )
                continue ;
            sbuff.append(sep) ;
            sep = " " ;
            format(sbuff, var) ;
        }
    }
    
    protected void format(StringBuffer sbuff, Var var)
    {
        Node node = get(var) ;
        String tmp = FmtUtils.stringForObject(node) ;
        sbuff.append("( ?"+var.getVarName()+" = "+tmp+" )") ;
    }
    
    // Do one level of binding 
    public String toString1()
    {
        StringBuffer sbuff = new StringBuffer() ;
        format1(sbuff) ;
        return sbuff.toString() ;
    }

    private void checkAdd(Var var, Node node)
    {
        if ( ! CHECKING )
            return ;
        if ( node == null )
            log.warn("check("+var+", "+node+"): null node value" ) ;
        if ( UNIQUE_NAMES_CHECK && get(var) != null )
            log.warn("check("+var+", "+node+"): Duplicate variable: "+var) ;

        checkAdd1(var, node) ;
    }

    protected abstract void checkAdd1(Var var, Node node) ;
    
    public static boolean same(Binding bind1, Binding bind2)
    {
        // Same variables?
        Iterator iter1 = bind1.vars() ;
        
        for ( ; iter1.hasNext() ; )
        {
            Var var = (Var)iter1.next() ; 
            Node n1 = bind1.get(var) ;
            Node n2 = bind2.get(var) ;
            
            if ( n1 == null && n2 == null )
                continue ;
            if (n1 == null )
                return false ;      // n2 not null
            if (n2 == null )
                return false ;      // n1 not null

            if ( !n1.equals(n2) )
                return false ;
        }
        
        // Now check the other way round.
        // Need only check the names match.
        Iterator iter2 = bind2.vars() ;
        for ( ; iter2.hasNext() ; )
        {
            Var var = (Var)iter1.next() ; 
            if ( !  bind1.contains(var) )
                return false ;
        }
        
        return true ;
    }
  
}

/*
 *  (c) Copyright 2004, 2005, 2006, 2007 Hewlett-Packard Development Company, LP
 *  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
