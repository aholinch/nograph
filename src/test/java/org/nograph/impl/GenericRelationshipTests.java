/* 

Copyright 2020 aholinch

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/
package org.nograph.impl;

import static org.junit.Assert.*;

import org.junit.Test;
import org.nograph.Node;
import org.nograph.Relationship;

public class GenericRelationshipTests 
{
    public Relationship getRelationship()
    {
    	return new GenericRelationship();
    }

    
	@Test
	public void testNode1ID()
	{
		Node n = new GenericNode();
		n.setID("123");
		
		Relationship rel = getRelationship();
		rel.setNode1(n);
		
		assertEquals(rel.getNode1ID(),"123");
	}
    
	@Test
	public void testNode1Type()
	{
		Node n = new GenericNode();
		n.setType("nodetype");
		
		Relationship rel = getRelationship();
		rel.setNode1(n);
		
		assertEquals(rel.getNode1Type(),"nodetype");
	}
    
	@Test
	public void testNode2ID()
	{
		Node n = new GenericNode();
		n.setID("123");
		
		Relationship rel = getRelationship();
		rel.setNode2(n);
		
		assertEquals(rel.getNode2ID(),"123");
	}
    
	@Test
	public void testNode2Type()
	{
		Node n = new GenericNode();
		n.setType("nodetype");
		
		Relationship rel = getRelationship();
		rel.setNode2(n);
		
		assertEquals(rel.getNode2Type(),"nodetype");
	}
	

}
