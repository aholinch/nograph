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

import java.util.Date;

import org.junit.Test;
import org.nograph.util.DateUtil;

public class BasePropertyHolderTests 
{
	public BasePropertyHolder getBPH()
	{
		return new BasePropertyHolder();
	}
	
	@Test
	public void testID()
	{
		String id="123";
		
		BasePropertyHolder bph = getBPH();
		
		bph.setID(id);
		assertEquals(bph.getID(),"123");
		
		Long lid = bph.getLongID();
		assertEquals(lid,new Long(123));
		
		lid = new Long(456);
		bph.setLongID(lid);
		assertEquals(bph.getLongID(),new Long(456));
		
		id = bph.getID();
		assertEquals(id,"456");
	}
	
	@Test
	public void testType()
	{
		BasePropertyHolder bph = getBPH();
		
		bph.setType("nodetype");
		assertEquals(bph.getType(),"nodetype");
		bph.setType("reltype");
		assertEquals(bph.getType(),"reltype");
	}
	
	@Test
	public void testLabel()
	{
		BasePropertyHolder bph = getBPH();
		bph.setLabel("label");
		assertEquals(bph.getLabel(),"label");
	}
	
	@Test
	public void testStoredValue()
	{
		BasePropertyHolder bph = getBPH();
		bph.setStoredValue("Some value that is stored");
		assertEquals(bph.getStoredValue(),"Some value that is stored");
	}

	@Test
	public void testStr()
	{
		BasePropertyHolder bph = getBPH();
		
		String key = "prop";
		bph.setProperty(key, "val");
		
		assertEquals(bph.getString(key),"val");
	}

	@Test
	public void testLong()
	{
		BasePropertyHolder bph = getBPH();
		
		String key = "prop";
		bph.setProperty(key, new Long(123));
		
		assertEquals(bph.getLong(key),new Long(123));
		
		assertEquals(bph.getString(key),"123");
	}

	@Test
	public void testDouble()
	{
		BasePropertyHolder bph = getBPH();
		
		String key = "prop";
		bph.setProperty(key, new Double(123.5));
		
		assertEquals(bph.getDouble(key),new Double(123.5));
		
		assertEquals(bph.getString(key),"123.5");
	}

	@Test
	public void testInt()
	{
		BasePropertyHolder bph = getBPH();
		
		String key = "prop";
		bph.setProperty(key, new Integer(123));
		
		assertEquals(bph.getInteger(key),new Integer(123));
		
		assertEquals(bph.getString(key),"123");
	}

	@Test
	public void testBoolean()
	{
		BasePropertyHolder bph = getBPH();
		
		String key = "prop";
		bph.setProperty(key, new Boolean(true));
		
		assertTrue(bph.getBoolean(key));
		
		assertEquals(bph.getString(key),"true");
		
		bph.setProperty(key, new Boolean(false));
		
		assertFalse(bph.getBoolean(key));
		
		assertEquals(bph.getString(key),"false");
	}

	@Test
	public void testDate()
	{
		BasePropertyHolder bph = getBPH();
		
		String key = "prop";
		Date d= DateUtil.getDate(2019,1, 23);
		long t = d.getTime();
		
		bph.setProperty(key, d);
		
		assertEquals(new Long(bph.getDate(key).getTime()),new Long(t));
	}
}
