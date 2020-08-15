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
package org.nograph.lucene;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
//import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexableFieldType;
//import org.apache.lucene.util.NumericUtils;
import org.nograph.util.json.JSONObject;

/**
 * Some utilities to make working with Lucene more convenient.
 * 
 * @author aholinch
 *
 */
public class LuceneUtil 
{
	private static final Logger logger = Logger.getLogger(LuceneUtil.class.getName());
	
	/**
	 * Converts an object first to JSON, then converts property map to Document.  Objects should be as flat as possible.  Good luck!
	 * @param obj
	 * @return
	 */
	public static Document toDocGeneric(Object obj)
	{
		Map<String,Object> map = null;
		
		try
		{
			JSONObject json = new JSONObject(obj,false);
			map = json.toMap();
		}
		catch(Exception ex)
		{
			logger.log(Level.WARNING, "Error getting property map", ex);
		}
		
		return toDoc(map);
	}
	
	/**
	 * Convert a map of properties to a Lucene document using the appendField conversion rules.  All fields are stored.  The map should be
	 * as flat as possible.  Nested arrays and iterables are handled, but not nested maps.
	 * 
	 * @param map
	 * @return
	 */
    public static Document toDoc(Map<String,Object> map)
    {
    	Document doc = new Document();
    	return toDoc(map,doc,false);
    }
    
    /**
     * Attempts to reuse document object
     * 
     * @param map
     * @param doc
     * @return
     */
    public static Document toDoc(Map<String,Object> map, Document doc, boolean analyzeIDs)
    {
    	if(map == null) return null;
    	
    	if(doc == null) doc = new Document();
    	
    	List<String> keys = new ArrayList<String>(map.keySet());
    	
    	int nk = keys.size();
    	String key = null;
    	Object val = null;

    	for(int i=0; i<nk; i++)
    	{
    		key = keys.get(i);
    		val = map.get(key);
    		if(val == null) continue;
    	
    		appendField(doc,key,val,true,analyzeIDs);    		
    	}
    	
    	return doc;
    }
    
    /**
     * Basic conversions using reflection.  Strings go into text fields.
     * Dates are LongPoints and number are numeric points.  Arrays and Collections are iterated over.  Booleans and
     * everything else become StringFields.
     * 
     * @param key
     * @param val
     * @param store whether to store the value or just make it searchable
     * @return
     */
    @SuppressWarnings("rawtypes")
	public static Field appendField(Document doc, String key, Object val, boolean store, boolean analyzeIDs)
    {
    	if(key == null || val == null) return null;
    	
    	Field f = null;
    	Field sf = null;
		key = key.toLowerCase();
		
		Field.Store fs = Field.Store.NO;
		if(store)
		{
			fs = Field.Store.YES;
		}
		
		if(val instanceof String)
		{
			f = new TextField(key,(String)val, fs);
			/*
			if(key.endsWith("id") && !analyzeIDs)
			{
				// We will force the storage of an unanalyzed ID field as well.
				store = true;
				//sf = new StringField(key,(String)val, fs);
				sf = new StringField(key,(String)val, Field.Store.YES);
			}
			*/
		}
		else if(val instanceof java.util.Date)
		{
			f = new LongPoint(key,((java.util.Date)val).getTime());
			if(store) sf = new StoredField(key,((java.util.Date)val).getTime());
		}
		else if(val instanceof Number)
		{
			
			if(val instanceof Double)
			{
				f = new DoublePoint(key,(Double)val);
				if(store) sf = new StoredField(key,(Double)val);
			}
			else if(val instanceof Integer)
			{
				f = new IntPoint(key,(Integer)val);
				if(store) sf = new StoredField(key,(Integer)val);
			}
			else if(val instanceof Long)
			{
				f = new LongPoint(key,(Long)val);
				if(store) sf = new StoredField(key,(Long)val);
			}
			else
			{
				// why are you using strange types with Lucene?  float, short, seriously?
				f = new DoublePoint(key, ((Number)val).doubleValue());
				if(store) sf = new StoredField(key,((Number)val).doubleValue());
			}
			
			/*
			if(val instanceof Double)
			{
				f = new SortedNumericDocValuesField(key,NumericUtils.doubleToSortableLong(((Number)val).doubleValue()));
				if(store) sf = new StoredField(key,(Double)val);				
			}
			else if(val instanceof Integer)
			{
				f = new SortedNumericDocValuesField(key,((Number)val).intValue());
				if(store) sf = new StoredField(key,(Integer)val);
			}
			else if(val instanceof Long)
			{
				f = new SortedNumericDocValuesField(key,((Number)val).longValue());
				if(store) sf = new StoredField(key,(Long)val);
			}
			else if(val instanceof Float)
			{
				f = new SortedNumericDocValuesField(key,NumericUtils.floatToSortableInt(((Number)val).floatValue()));
				if(store) sf = new StoredField(key,(Float)val);				
			}
			else if(val instanceof Short)
			{
				f = new SortedNumericDocValuesField(key,((Number)val).shortValue());
				if(store) sf = new StoredField(key,(Short)val);								
			}
			*/
		}
		else if(val instanceof Iterable)
		{
			Iterator iter = ((Iterable)val).iterator();
			while(iter.hasNext())
			{
				val = iter.next();
				appendField(doc,key,val,store,analyzeIDs);
			}
			f = null;
		}
		else if(val.getClass().isArray())
		{
			int size = Array.getLength(val);
			for(int i=0; i<size; i++)
			{
				appendField(doc,key,Array.get(val, i),store,analyzeIDs);
			}
			f = null;
		}
		else
		{
			f = new StringField(key,String.valueOf(val), fs);    			
		}
		
		if(f != null)
		{
			doc.add(f);
			if(store && sf != null)
			{
				doc.add(sf);
			}
		}

		return f;
    }
    
    /**
     * Take the stored field values and make a property map from the document.
     * 
     * @param doc
     * @return
     */
    @SuppressWarnings("unchecked")
	public static Map<String,Object> fromDoc(Document doc)
    {
    	if(doc == null) return null;
    	
    	Map<String,Object> map = new HashMap<String,Object>();
    	
    	String key = null;
    	Object val = null;
    	Object prevVal = null;
    	List<Object> tmp = null;
    	IndexableField f = null;
    	IndexableFieldType type = null;
    	List<IndexableField> fields = null;
    	int nf = 0;
    	
    	fields = doc.getFields();
    	if(fields != null) nf = fields.size();
    	
    	for(int i=0; i<nf; i++)
    	{
    		f = fields.get(i);
    		type = f.fieldType();
    		if(!type.stored()) continue; // nothing stored
    		key = f.name();
    		val = f.stringValue(); // just play with strings for now
    		try
    		{
    			prevVal = f.numericValue();
    			if(prevVal != null)
    			{
    				val = prevVal;
    			}
    		}
    		catch(Exception ex)
    		{
    			ex.printStackTrace();
    		}
    		prevVal = map.get(key);
    		if(prevVal == null)
    		{
    			map.put(key, val);
    		}
    		else
    		{
    			if(prevVal instanceof java.util.List)
    			{
    				tmp = (List<Object>)prevVal;
    			}
    			else
    			{
    				tmp = new ArrayList<Object>();
    				tmp.add(prevVal);
    				map.put(key, tmp);
    			}
    			
    			tmp.add(val);
    		}
    	}
    	
    	
    	return map;
    }
}
