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
package org.nograph;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the criterion and other query options.
 * 
 * @author aholinch
 *
 */
public class GraphQuery 
{
	protected int maxResults;
	protected Criterion crit;
	protected boolean fetchNodesForRels;
	
	public GraphQuery()
	{
		
	}
	
	public int getMaxResults()
	{
		return maxResults;
	}
	
	public void setMaxResults(int max)
	{
		maxResults = max;
	}
	
	public Criterion getCriterion()
	{
		return crit;
	}
	
	public void setCriterion(Criterion crit)
	{
		this.crit = crit;
	}
	
	public boolean getFetchNodesForRelationships()
	{
		return fetchNodesForRels;
	}
	
	public void setFetchNodesForRelationships(boolean flag)
	{
		fetchNodesForRels = flag;
	}
	
	public static Criterion createEqualsCriterion(String key, Object val)
	{
		return new SimpleCriterion(key,val,SimpleCriterion.OP_EQUAL);
	}
	
	public static Criterion createRangeCriterion(String key, Object valMin, Object valMax)
	{
		RangeCriterion rc = new RangeCriterion();
		rc.setKey(key);
		rc.setMinValue(valMin);
		rc.setMaxValue(valMax);
		return rc;
	}
	
	/**
	 * Empty marker interface.  Implementations are simple or boolean
	 * @author aholinch
	 *
	 */
    public static interface Criterion
    {
    	
    }
    
    /**
     * A simple criterion relating a value to a key with an operator: =, !=, >, <, >=, <=, ~.
     * 
     * @author aholinch
     *
     */
    public static class SimpleCriterion implements Criterion
    {
    	public static final int OP_EQUAL = 1;
    	public static final int OP_NOT_EQUAL = 2;
    	public static final int OP_GT = 3;
    	public static final int OP_LT = 4;
    	public static final int OP_GE = 5;
    	public static final int OP_LE = 6;
    	public static final int OP_LIKE = 7;
    	
    	protected String key;
    	protected Object value;
    	protected int operator;
    	
    	public SimpleCriterion()
    	{
    		
    	}
    	
    	public SimpleCriterion(String key, Object value, int operator)
    	{
    		this.key = key;
    		this.value = value;
    		this.operator = operator;
    	}
    	
    	public String getKey()
    	{
    		return key;
    	}
    	
    	public void setKey(String str)
    	{
    		key = str;
    	}
    	
    	public Object getValue()
    	{
    		return value;
    	}
    	
    	public void setValue(Object val)
    	{
    		value = val;
    	}
    	
    	public int getOperator()
    	{
    		return operator;
    	}
    	
    	public void setOperator(int op)
    	{
    		operator = op;
    	}
    }
    
    /**
     * Groups criteria together in a set using either AND or OR.
     * 
     */
    public static class SetCriterion implements Criterion
    {
    	public static final int COMB_AND = 1;
    	public static final int COMB_OR = 2;
    	
    	protected int setOp = 0;
    	
    	protected List<Criterion> crits = null;
    	
    	public SetCriterion()
    	{
    	    crits = new ArrayList<Criterion>();	
    	}
    	
    	public int getSetOperation()
    	{
    		return setOp;
    	}
    	
    	public void setSetOperation(int op)
    	{
    		setOp = op;
    	}
    	
    	public int getNumCriteria()
    	{
    		return crits.size();
    	}
    	
    	public void addCriterion(Criterion crit)
    	{
    		crits.add(crit);
    	}
    	
    	public Criterion getCriterion(int ind)
    	{
    		return crits.get(ind);
    	}
    	
    	public void clearCriteria()
    	{
    		crits.clear();
    	}
    }
    
    /**
     * Some engines implement a range query in a better than than a set of > and < queries.
     * 
     * @author aholinch
     *
     */
    public static class RangeCriterion implements Criterion
    {
    	protected String key;
    	protected Object minValue;
    	protected Object maxValue;
    	protected boolean minInclusive = false;
    	protected boolean maxInclusive = false;
    	
    	public RangeCriterion()
    	{
    		
    	}
    	
    	public void setKey(String str)
    	{
    		key = str;
    	}
    	
    	public String getKey()
    	{
    		return key;
    	}
    	
    	public void setMinValue(Object value)
    	{
    		setMinValue(value,false);
    	}
    	
    	public void setMinValue(Object value, boolean inclusive)
    	{
    		minValue = value;
    		minInclusive = inclusive;
    	}
    	
    	public Object getMinValue()
    	{
    		return minValue;
    	}
    	
    	public void setMinInclusive(boolean flag)
    	{
    		minInclusive = flag;
    	}
    	
    	public boolean getMinInclusive()
    	{
    		return minInclusive;
    	}
    	
    	
    	public void setMaxValue(Object value)
    	{
    		setMaxValue(value,false);
    	}
    	
    	public void setMaxValue(Object value, boolean inclusive)
    	{
    		maxValue = value;
    		maxInclusive = inclusive;
    	}
    	
    	public Object getMaxValue()
    	{
    		return maxValue;
    	}
    	
    	public void setMaxInclusive(boolean flag)
    	{
    		maxInclusive = flag;
    	}
    	
    	public boolean getMaxInclusive()
    	{
    		return maxInclusive;
    	}

    }
}
