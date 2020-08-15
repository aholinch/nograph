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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.nograph.DataDecorator;
import org.nograph.GraphManager;
import org.nograph.GraphQuery;
import org.nograph.GraphQuery.Criterion;
import org.nograph.GraphQuery.RangeCriterion;
import org.nograph.GraphQuery.SetCriterion;
import org.nograph.GraphQuery.SimpleCriterion;
import org.nograph.NoGraph;
import org.nograph.NoGraphConfig;
import org.nograph.NoGraphException;
import org.nograph.Node;
import org.nograph.Path;
import org.nograph.Relationship;
import org.nograph.lucene.LuceneIndex;
import org.nograph.lucene.LuceneUtil;
import org.nograph.util.FileUtil;

/**
 * Manages a graph on top of a set of Lucene indexes.
 * 
 * @author aholinch
 *
 */
public class LuceneGraphManager implements GraphManager 
{
	private static final Logger logger = Logger.getLogger(LuceneGraphManager.class.getName());
	
	// ids
	protected static final AtomicLong idgen = new AtomicLong();
	protected static String idsync = "mutex";
	
	// instance
	protected static String instsync = "mutex";
	protected static Map<String,LuceneGraphManager> graphMap = null;
	
	// name
	protected String name = null;
	
	// directories and files
	protected String nodeDir = null;
	protected String relDir = null;
	protected String metaDir = null;
	protected String idFile = null;
	
	// indexes
	protected LuceneIndex nodeIndex = null;
	protected LuceneIndex relIndex = null;
	protected boolean readOnlyIndex = false;
	
	public static final String PROP_ND = "node.dir";
	public static final String PROP_RD = "rel.dir";
	public static final String PROP_MD = "meta.dir";
	public static final String PROP_READONLY = "index.readonly";
	
	public static final String ID_KEY = BasePropertyHolder.ID_KEY;
	public static final String TYPE_KEY = BasePropertyHolder.TYPE_KEY;
	public static final String N1_KEY = GenericRelationship.N1_KEY;
	public static final String N2_KEY = GenericRelationship.N2_KEY;
	public static final String N1_TYPEKEY = GenericRelationship.N1_TYPEKEY;
	public static final String N2_TYPEKEY = GenericRelationship.N2_TYPEKEY;

	// meta info
	protected GraphMeta graphMeta = null;
	protected String metaFile = null;
	protected static String metasync = "mutex";

	// decorator
	protected DataDecorator decorator = null;
	protected boolean decorateNodes = false;
	protected boolean decorateRels = false;
	
	
	private LuceneGraphManager(String graphName)
	{
	    init(graphName);	
	}
	
	/**
	 * Get directory information and create LuceneIndexes
	 */
	protected void init(String graphName)
	{
		name = graphName;
		NoGraphConfig config = NoGraph.getInstance().getConfig();
	
		if(name == null || name.trim().length() == 0 || name.equals(NoGraphConfig.DEFAULT_NAME))
		{
			nodeDir = config.getProperty(PROP_ND);
			relDir  = config.getProperty(PROP_RD);
			metaDir  = config.getProperty(PROP_MD);
			readOnlyIndex = config.getBoolProperty(PROP_READONLY);
		}
		else
		{
			nodeDir = config.getProperty(name+"."+PROP_ND);
			relDir  = config.getProperty(name+"."+PROP_RD);			
			metaDir  = config.getProperty(name+"."+PROP_MD);			
			readOnlyIndex = config.getBoolProperty(name + "." + PROP_READONLY);
		}
		
		if(nodeDir == null)
		{
			nodeDir = "graphdata/nodes/";
		}
		
		if(relDir == null)
		{
			relDir = "graphdata/rels/";
		}
		
		nodeDir = nodeDir.replace('\\', '/');
		relDir = relDir.replace('\\', '/');
		
		if(!nodeDir.endsWith("/")) nodeDir+="/";
		if(!relDir.endsWith("/")) relDir+="/";
		
		if(metaDir == null)
		{
			int ind = nodeDir.lastIndexOf('/',nodeDir.length()-2);
			metaDir = nodeDir.substring(0,ind+1)+"meta/";
		}
		
		try
		{
			File md = new File(metaDir);
			md.mkdirs();
		}
		catch(Exception ex)
		{
			logger.log(Level.WARNING,"Error creating meta dir");
		}
		
		idFile = metaDir+"id.store";
		metaFile = metaDir+"graphmeta.json";
		
		// ideally we'd let people configure this
		Analyzer analyzer = null;
		
		analyzer = createDefaultAnalyzer();
		nodeIndex = new LuceneIndex(nodeDir,analyzer,readOnlyIndex);
		
		analyzer = createDefaultAnalyzer();
		relIndex = new LuceneIndex(relDir,analyzer,readOnlyIndex);
		
		// read id from idFile
		synchronized(idsync)
		{
			FileReader fr = null;
			BufferedReader br = null;
			try
			{
				File f = new File(idFile);
				if(f.exists())
				{
					fr = new FileReader(idFile);
					br = new BufferedReader(fr);
					
					String line = br.readLine();
					
					long id = Long.parseLong(line.trim());
					idgen.set(id);
				}
			}
			catch(Exception ex)
			{
				logger.log(Level.WARNING, "Error getting id", ex);
			}
			finally
			{
				if(fr != null)try{fr.close();}catch(Exception ex){}
				if(br != null)try{br.close();}catch(Exception ex){}
			}
		}
		
		loadGraphMeta();
	}
	
	/**
	 * This is inspired by Neo4j's whitespace, lowercase analyzer.
	 * 
	 * @return
	 */
	protected Analyzer createDefaultAnalyzer()
	{
		/*
		Analyzer anlzr = new Analyzer()
	    {
	        @Override
	        protected TokenStreamComponents createComponents( String fieldName )
	        {
	            Tokenizer source = new WhitespaceTokenizer();
	            TokenStream filter = new LowerCaseFilter( source );
	            return new TokenStreamComponents( source, filter );
	        }

	        @Override
	        public String toString()
	        {
	            return "LOWER_CASE_WHITESPACE_ANALYZER";
	        }
	    };
	    
	    return anlzr;
	    */
		//stop words may be too much, but I really like seeing the punctuation going away
	    return new StandardAnalyzer();
	}
	
	public static LuceneGraphManager getInstance(String name)
	{
		if(name == null || name.trim().length() == 0) name = NoGraphConfig.DEFAULT_NAME;
		
		if(graphMap == null)
		{
			synchronized(instsync)
			{
				// yes, check again
				if(graphMap == null)
				{
					graphMap = new HashMap<String,LuceneGraphManager>();
				}
			}
		}
		
		LuceneGraphManager instance = null;
		
		instance = graphMap.get(name);
		
		if(instance == null)
		{
			synchronized(instsync)
			{
				// yes, check again
				instance = graphMap.get(name);
				if(instance == null)
				{
					instance = new LuceneGraphManager(name);
					graphMap.put(name, instance);
				}
			}			
		}
		
		return instance;
	}
	
	@Override
	public String getName()
	{
		return name;
	}
	
	@Override
	public void setName(String str)
	{
		name = str;
	}

	@Override
	public void saveNode(Node n) throws NoGraphException 
	{
		if(n == null) return;
		
		boolean doDelete = true;
		if(n.getID() == null)
		{
			String id = getNextNodeID();
			n.setID(id);
			doDelete = false;
		}
			
		try
		{
			decorateNode(n);
			
			Document doc = nodeToDoc(n,null);
			if(doDelete)
			{
				// there is no update, only delete and save again
				nodeIndex.deleteDocument(ID_KEY,n.getID());
			}
			nodeIndex.saveDocument(doc);
			
			nodeIndex.commit();
			
			graphMeta.updateNodeMeta(n);
			// if there's and index commit then write the meta
			writeGraphMeta();
		}
		catch(Exception ex)
		{
			logger.log(Level.WARNING, "Error saving node", ex);
			throw new NoGraphException("Error saving node", ex);
		}
	}

	@Override
	public void deleteNode(Node n) throws NoGraphException 
	{
		if(n == null) return;
		if(n.getID() == null) throw new NoGraphException("No node id");

		deleteNode(n.getID());
	}

	@Override
	public void deleteNode(String id) throws NoGraphException 
	{
		try
		{
			nodeIndex.deleteDocument(ID_KEY, id);
			relIndex.deleteDocuments(new Term(N1_KEY,id));
			relIndex.deleteDocuments(new Term(N2_KEY,id));
			nodeIndex.commit();
			relIndex.commit();
		}
		catch(Exception ex)
		{
			throw new NoGraphException("Error deleting",ex);
		}
	}

	@Override
	public Node getNode(String id) throws NoGraphException 
	{
		Node n = null;
		try
		{
			TermQuery tq = new TermQuery(new Term(ID_KEY,id));
			List<Document> docs = nodeIndex.search(tq);
			if(docs != null && docs.size() > 0)
			{
				Document doc = docs.get(0);
				n = docToNode(doc,n);
			}
		}
		catch(Exception ex)
		{
			logger.log(Level.WARNING, "Error getting node", ex);
			throw new NoGraphException("Error getting node",ex);
		}
		return n;
	}

	@Override
	public void ingestNodes(List<Node> nodes) throws NoGraphException 
	{
		if(nodes == null || nodes.size() == 0) return;
			
		try
		{
			Document doc = new Document();
			int size = nodes.size();
			List<String> ids = getNextNodeIDs(size);
			Document d = null;
			Node n = null;
			
			decorateNodes(nodes);
			
			sampleNodeMeta(nodes);
			
			for(int i=0; i<size; i++)
			{
				doc.clear();
				
				n = nodes.get(i);
				n.setID(ids.get(i));
				
				d = nodeToDoc(n,doc);
				
				if(d != null)
				{
					// d is the marker, doc is the actual reused instance
					nodeIndex.saveDocument(doc);
				}				
			}
			
			nodeIndex.commit();
		}
		catch(Exception ex)
		{
			logger.log(Level.WARNING, "Error ingesting nodes", ex);
			throw new NoGraphException("Error ingesting nodes", ex);
		}
	}

	@Override
	public void saveNodes(List<Node> nodes) throws NoGraphException 
	{
		if(nodes == null || nodes.size() == 0) return;
		
		int size = nodes.size();
		
		List<Node> newNodes = new ArrayList<Node>(size/2);
		List<Node> existingNodes = new ArrayList<Node>(size/2);
		List<String> delIDs = new ArrayList<String>(size/2);
		
		Node n = null;
		for(int i=0; i<size; i++)
		{
			n = nodes.get(i);
			if(n.getID() == null)
			{
				newNodes.add(n);
			}
			else
			{
				existingNodes.add(n);
				delIDs.add(n.getID());
			}
		}
		
		if(newNodes.size() > 0)
		{
			// bulk insert with id assignment
			ingestNodes(newNodes);
		}
		
		if(existingNodes.size() > 0)
		{
			// we have to remove existing and add again
			deleteNodesByID(delIDs);
			try
			{
				Document doc = new Document();
				size = existingNodes.size();
				Document d = null;
				
				// new nodes were decorated by ingestNodes
				decorateNodes(existingNodes);
				sampleNodeMeta(existingNodes);
				
				for(int i=0; i<size; i++)
				{
					doc.clear();
					
					n = existingNodes.get(i);
					
					d = nodeToDoc(n,doc);
					
					if(d != null)
					{
						// d is the marker, doc is the actual reused instance, expected to be the same
						nodeIndex.saveDocument(doc);
					}
				}

				nodeIndex.commit();
				
			}
			catch(Exception ex)
			{
				logger.log(Level.WARNING, "Error saving nodes", ex);
				throw new NoGraphException("Error saving nodes", ex);
			}
		}
	}

	@Override
	public void deleteNodes(List<Node> nodes) throws NoGraphException 
	{
		if(nodes == null || nodes.size() == 0) return;
		int size = nodes.size();
		List<String> ids = new ArrayList<String>(size);
		String id = null;
		for(int i=0; i<size; i++)
		{
			id = nodes.get(i).getID();
			if(id != null)
			{
				ids.add(id);
			}
		}
		
		deleteNodesByID(ids);
	}

	@Override
	public void deleteNodesByID(List<String> ids) throws NoGraphException 
	{
		if(ids == null || ids.size() == 0) return;
		
		Term t = null;
		int size = ids.size();
		String id = null;
		try
		{
			for(int i=0; i<size; i++)
			{
				id = ids.get(i);
				t = new Term(ID_KEY,id);
				nodeIndex.deleteDocuments(t);
				relIndex.deleteDocuments(new Term(N1_KEY,id));
				relIndex.deleteDocuments(new Term(N2_KEY,id));
			}
			
			nodeIndex.commit();
			relIndex.commit();

		}
		catch(Exception ex)
		{
			throw new NoGraphException(ex);
		}
	}

	@Override
	public void saveRelationship(Relationship r) throws NoGraphException 
	{
		if(r == null) return;
		
		boolean doDelete = true;
		if(r.getID() == null)
		{
			String id = getNextRelID();
			r.setID(id);
			doDelete = false;
		}
			
		try
		{
			decorateRel(r);
			Document doc = relToDoc(r,null);
			if(doDelete)
			{
				// there is no update, only delete and save again
				relIndex.deleteDocument(ID_KEY,r.getID());
			}
			relIndex.saveDocument(doc);
			
			graphMeta.updateRelationshipMeta(r);

			relIndex.commit();
			
			writeGraphMeta();
		}
		catch(Exception ex)
		{
			logger.log(Level.WARNING, "Error saving rel", ex);
			throw new NoGraphException("Error saving rel", ex);
		}
	}

	@Override
	public void deleteRelationship(Relationship r) throws NoGraphException 
	{
		if(r == null) return;
		if(r.getID() == null) throw new NoGraphException("No rel id");

		deleteRelationship(r.getID());
	}

	@Override
	public void deleteRelationship(String id) throws NoGraphException
	{
		try
		{
			relIndex.deleteDocument(ID_KEY, id);
			
			relIndex.commit();

		}
		catch(Exception ex)
		{
			throw new NoGraphException("Error deleting",ex);
		}
	}

	@Override
	public Relationship getRelationship(String id, boolean fetchNodes) throws NoGraphException 
	{
		Relationship r = null;
		try
		{
			TermQuery tq = new TermQuery(new Term(ID_KEY,id));
			List<Document> docs = relIndex.search(tq);
			if(docs != null && docs.size() > 0)
			{
				Document doc = docs.get(0);
				r = docToRel(doc,r);
				
				List<Relationship> tmp = new ArrayList<Relationship>();
				tmp.add(r);
				
				if(fetchNodes)
				{
					populateNodesForRels(tmp);
				}
			}
		}
		catch(Exception ex)
		{
			logger.log(Level.WARNING, "Error getting rel", ex);
			throw new NoGraphException("Error getting rel",ex);
		}
		return r;
	}

	@Override
	public void ingestRelationships(List<Relationship> rels) throws NoGraphException 
	{
		if(rels == null || rels.size() == 0) return;
		
		try
		{
			Document doc = new Document();
			int size = rels.size();
			List<String> ids = getNextRelIDs(size);
			Document d = null;
			Relationship r = null;
			
			decorateRels(rels);
			sampleRelMeta(rels);
			
			for(int i=0; i<size; i++)
			{
				doc.clear();
				
				r = rels.get(i);
				r.setID(ids.get(i));
				
				d = relToDoc(r,doc);
				
				if(d != null)
				{
					// d is the marker, doc is the actual reused instance
					relIndex.saveDocument(doc);
				}
			}
			
			relIndex.commit();
		}
		catch(Exception ex)
		{
			logger.log(Level.WARNING, "Error ingesting rels", ex);
			throw new NoGraphException("Error ingesting rels", ex);
		}

	}

	@Override
	public void saveRelationships(List<Relationship> rels) throws NoGraphException 
	{
		if(rels == null || rels.size() == 0) return;
		
		int size = rels.size();
		
		List<Relationship> newRels = new ArrayList<Relationship>(size/2);
		List<Relationship> existingRels = new ArrayList<Relationship>(size/2);
		List<String> delIDs = new ArrayList<String>(size/2);
		
		Relationship r = null;
		for(int i=0; i<size; i++)
		{
			r = rels.get(i);
			if(r.getID() == null)
			{
				newRels.add(r);
			}
			else
			{
				existingRels.add(r);
				delIDs.add(r.getID());
			}
		}
		
		if(newRels.size() > 0)
		{
			// bulk insert with id assignment
			ingestRelationships(newRels);
		}
		
		if(existingRels.size() > 0)
		{
			// we have to remove existing and add again
			deleteNodesByID(delIDs);
			try
			{
				Document doc = new Document();
				size = existingRels.size();
				Document d = null;

				// new rels are decorated by ingestRelationships
				decorateRels(existingRels);
				sampleRelMeta(existingRels);
				
				for(int i=0; i<size; i++)
				{
					doc.clear();
					
					r = existingRels.get(i);
					
					d = relToDoc(r,doc);
					
					if(d != null)
					{
						// d is the marker, doc is the actual reused instance, expected to be the same
						relIndex.saveDocument(doc);
					}
				}

				relIndex.commit();
			}
			catch(Exception ex)
			{
				logger.log(Level.WARNING, "Error saving rels", ex);
				throw new NoGraphException("Error saving rels", ex);
			}
		}
	}

	@Override
	public void deleteRelationships(List<Relationship> rels) throws NoGraphException 
	{
		if(rels == null || rels.size() == 0) return;
		int size = rels.size();
		List<String> ids = new ArrayList<String>(size);
		String id = null;
		for(int i=0; i<size; i++)
		{
			id = rels.get(i).getID();
			if(id != null)
			{
				ids.add(id);
			}
		}
		
		deleteRelationshipsByID(ids);
	}

	@Override
	public void deleteRelationshipsByID(List<String> ids) throws NoGraphException 
	{
		if(ids == null || ids.size() == 0) return;
		
		Term t = null;
		int size = ids.size();
		String id = null;
		try
		{
			for(int i=0; i<size; i++)
			{
				id = ids.get(i);
				t = new Term(ID_KEY,id);
				relIndex.deleteDocuments(t);
			}
			
			relIndex.commit();

		}
		catch(Exception ex)
		{
			throw new NoGraphException(ex);
		}
	}

	@Override
	public List<Node> findNodes(String key, Object val) throws NoGraphException 
	{
		if(val == null) return null;
		if(key == null) return null;
		
		List<Node> nodes = null;
		
		try
		{
			List<Document> docs = null;			
			
			Query q = getQuery(key,val,nodeIndex.getAnalyzer());
			
			docs = nodeIndex.search(q);
			
			nodes = getNodesFromDocs(docs);
		}
		catch(Exception ex)
		{
			logger.log(Level.SEVERE, "Error searching nodes", ex);
			throw new NoGraphException("Error searching nodes", ex);
		}
		
		return nodes;
	}

	@Override
	public List<Relationship> findRelationships(String key, Object val, boolean fetchNodes) throws NoGraphException 
	{
		if(val == null) return null;
		if(key == null) return null;
		
		List<Relationship> rels = null;
		
		try
		{
			List<Document> docs = null;
			
			Query q = getQuery(key,val,relIndex.getAnalyzer());
			
			docs = relIndex.search(q);
			
			rels = getRelsFromDocs(docs, fetchNodes);
		}
		catch(Exception ex)
		{
			logger.log(Level.SEVERE, "Error searching rels", ex);
			throw new NoGraphException("Error searching rels", ex);
		}
		
		return rels;
	}
	
	protected Query getQuery(String key, Object val, Analyzer analyzer)
	{
		Query q = null;
		if(val instanceof Number)
		{
			if(val instanceof Double)
			{
				q = DoublePoint.newExactQuery(key, (Double)val);
			}
			else if(val instanceof Integer)
			{
				q = IntPoint.newExactQuery(key, (Integer)val);
			}
			else if(val instanceof Long)
			{
				q = LongPoint.newExactQuery(key, (Long)val);
			}
			else if(val instanceof Float)
			{
				q = FloatPoint.newExactQuery(key, (Float)val);
			}
		}
		else
		{
			try
			{
				QueryParser qp = new QueryParser(key,analyzer);
				
				// need this for some queries
				q = qp.parse(key+":"+String.valueOf(val));
				
				// need this for other queries!!!!!!
				//q = new TermQuery(new Term(key,String.valueOf(val)));
			}
			catch(Exception ex)
			{
				logger.warning("Error parsing query:"+ex.getMessage());
			}
		}
		
		return q;
	}
	
	protected List<Node> getNodesFromDocs(List<Document> docs) throws NoGraphException
	{
		List<Node> nodes = null;
		
		if(docs != null)
		{
			int size = docs.size();
			nodes = new ArrayList<Node>(size);
			
			Node n = null;
			Document d = null;
			
			for(int i=0; i<size; i++)
			{
				d = docs.get(i);
				n = docToNode(d,null);
				if(n != null)
				{
					nodes.add(n);
				}
			}	
		}
		
		return nodes;
	}
	
	protected List<Relationship> getRelsFromDocs(List<Document> docs, boolean fetchNodes) throws NoGraphException
	{
		List<Relationship> rels = null;
		
		if(docs != null)
		{
			int size = docs.size();
			rels = new ArrayList<Relationship>(size);
			
			Relationship r = null;
			Document d = null;
			
			for(int i=0; i<size; i++)
			{
				d = docs.get(i);
				r = docToRel(d,null);
				if(r != null)
				{
					rels.add(r);
				}
			}
			
			if(fetchNodes)
			{
				populateNodesForRels(rels);	
			}
		}		
		
		return rels;
	}
	
    public List<Node> findNodes(GraphQuery query) throws NoGraphException
    {
    	List<Node> nodes = null;
    	if(query == null)
    	{
    		logger.warning("Null query");
    		return null;
    	}
    	
		try
		{
			List<Document> docs = null;			

			
			String qs = queryToString(query);
			logger.info(qs);
			
			// use the same parser on ingest and query and it's all good
			QueryParser qp = new QueryParser(TYPE_KEY,nodeIndex.getAnalyzer());
			Query q = qp.parse(qs);
			
			//Query q = getQ(query.getCriterion(),nodeIndex.getAnalyzer());
			//logger.info(q.toString());
			
			int maxResults = query.getMaxResults();
			
			if(maxResults < 1)
			{
				docs = nodeIndex.search(q);
			}
			else
			{
				docs = nodeIndex.search(q,maxResults);
			}
			
			nodes = getNodesFromDocs(docs);
		}
		catch(Exception ex)
		{
			logger.log(Level.SEVERE, "Error searching nodes", ex);
			throw new NoGraphException("Error searching nodes", ex);
		}
    	    	
    	return nodes;
    }
    
    public List<Relationship> findRelationships(GraphQuery query) throws NoGraphException
    {
    	List<Relationship> rels = null;

    	if(query == null)
    	{
    		logger.warning("Null query");
    		return null;
    	}
    	
		try
		{
			List<Document> docs = null;			

			String qs = queryToString(query);
			logger.info(qs);
			
			QueryParser qp = new QueryParser(TYPE_KEY,relIndex.getAnalyzer());
			Query q = qp.parse(qs);
			
			int maxResults = query.getMaxResults();
			
			if(maxResults < 1)
			{
				docs = relIndex.search(q);
			}
			else
			{
				docs = relIndex.search(q,maxResults);
			}
			
			rels = getRelsFromDocs(docs,query.getFetchNodesForRelationships());
		}
		catch(Exception ex)
		{
			logger.log(Level.SEVERE, "Error searching rels", ex);
			throw new NoGraphException("Error searching rels", ex);
		}

    	return rels;
    }

	protected String queryToString(GraphQuery query)
	{
		Criterion crit = query.getCriterion();
		if(crit == null)
		{
			logger.warning("No criterion specified");
			return null;
		}
		
		String qs = getQS(crit);
		
		return qs;
	}

	protected String getQS(Criterion crit)
	{
		String qs = null;
		
		if(crit instanceof SimpleCriterion)
		{
			qs = getSimpleCriterion((SimpleCriterion)crit);
		}
		else if(crit instanceof SetCriterion)
		{
			qs = getSetCriterion((SetCriterion)crit);
		}
		else if(crit instanceof RangeCriterion)
		{
			qs = getRangeCriterion((RangeCriterion)crit);
		}
		
		return qs;
	}
	
	protected Query getQ(Criterion crit, Analyzer lyzer)
	{
		Query q = null;
		
		if(crit instanceof SimpleCriterion)
		{
			q = getSimpleCriterionAsQ((SimpleCriterion)crit,lyzer);
		}
		else if(crit instanceof SetCriterion)
		{
			q = getSetCriterionAsQ((SetCriterion)crit,lyzer);
		}
		else if(crit instanceof RangeCriterion)
		{
			q = getRangeCriterionAsQ((RangeCriterion)crit,lyzer);
		}
		
		return q;
	}

	/**
	 * Convert the simple criterion to a lucene style query string.
	 * 
	 * @param crit
	 * @return
	 */
	protected String getSimpleCriterion(SimpleCriterion crit)
	{
		String valueStr = null;
		
		Object val = crit.getValue();
		if(val == null)
		{
			valueStr = "null";
		}
		else if(val instanceof Criterion)
		{
			valueStr = "("+getQS((Criterion)val)+")";
		}
		else
		{
			valueStr = String.valueOf(val);
		}
		
		String qs = null;
		
		if(crit.getOperator()==SimpleCriterion.OP_EQUAL)
		{
			qs = crit.getKey()+":"+valueStr;
		}
		
		return qs;
	}
	
	protected String getRangeCriterion(RangeCriterion crit)
	{
		String valueStr1 = null;
		String valueStr2 = null;
		
		Object val = crit.getMinValue();
		if(val == null)
		{
			valueStr1 = "null";
		}
		else
		{
			valueStr1 = String.valueOf(val);
		}
		
		val = crit.getMaxValue();
		if(val == null)
		{
			valueStr2 = "null";
		}
		else
		{
			valueStr2 = String.valueOf(val);
		}
		
		String minB = "{";
		String maxB = "}";
		if(crit.getMinInclusive())
		{
			minB = "[";
		}
		if(crit.getMaxInclusive())
		{
			maxB = "]";
		}
		
		String qs = crit.getKey()+":"+minB + valueStr1 + " TO " + valueStr2 + maxB;
		
		
		return qs;
	}
	
	protected List<String> analyze(String text, Analyzer analyzer)
	{
	    List<String> out = new ArrayList<String>();
	    TokenStream ts = null;
	    try
	    {
		    ts = analyzer.tokenStream("", text);
		    CharTermAttribute attr = ts.addAttribute(CharTermAttribute.class);
		    ts.reset();
		    while(ts.incrementToken()) {
		       out.add(attr.toString());
		    }
	    }
	    catch(Exception ex)
	    {
	    	logger.info("Error analyzing text:" + ex.getMessage());
	    }
	    finally
	    {
	    	if(ts != null)try{ts.close();}catch(Exception ex){};
	    }
	    return out;
	}
	
	protected Query getSimpleCriterionAsQ(SimpleCriterion crit, Analyzer lyzer)
	{
		Query q = null;
		
		String valueStr = null;
		
		Object val = crit.getValue();
		if(val == null)
		{
			valueStr = "null";
		}
		else if(val instanceof Criterion)
		{
			valueStr = "("+getQS((Criterion)val)+")";
		}
		else
		{
			valueStr = String.valueOf(val);
			if(lyzer != null)
			{
				List<String> txts = analyze(valueStr,lyzer);
				if(txts != null && txts.size() > 0)
				{
					//logger.info(txts.toString());
					valueStr = txts.get(0);
				}
			}
		}
		
		
		
		if(crit.getOperator()==SimpleCriterion.OP_EQUAL)
		{
			q = new TermQuery(new Term(crit.getKey(),valueStr));
		}
		
		return q;
	}
	
	protected Query getRangeCriterionAsQ(RangeCriterion crit, Analyzer lyzer)
	{
		Query q = null;
		Object val1 = crit.getMinValue();
		Object val2 = crit.getMaxValue();
		String key = crit.getKey();
		
		if(val1 instanceof Number)
		{
			Number n1 = (Number)val1;
			Number n2 = (Number)val2;
			if(val1 instanceof Integer)
			{
				q = IntPoint.newRangeQuery(key, n1.intValue(), n2.intValue());
			}
			if(val1 instanceof Double)
			{
				q = DoublePoint.newRangeQuery(key, n1.doubleValue(), n2.doubleValue());
			}
			if(val1 instanceof Long)
			{
				q = LongPoint.newRangeQuery(key, n1.longValue(), n2.longValue());
			}
			if(val1 instanceof Float)
			{
				q = FloatPoint.newRangeQuery(key, n1.floatValue(), n2.floatValue());
			}
		}
		else
		{
			String minS = String.valueOf(val1);
			if(lyzer != null)
			{
				List<String> txts = analyze(minS,lyzer);
				if(txts != null && txts.size() > 0)
				{
					//logger.info(txts.toString());
					minS = txts.get(0);
				}
			}
			
			String maxS = String.valueOf(val2);
			if(lyzer != null)
			{
				List<String> txts = analyze(maxS,lyzer);
				if(txts != null && txts.size() > 0)
				{
					//logger.info(txts.toString());
					maxS = txts.get(0);
				}
			}
			
			q = TermRangeQuery.newStringRange(key, minS, maxS, crit.getMinInclusive(), crit.getMaxInclusive());
		}
		
		return q;
	}
	
	/**
	 * Convert the set criterion to a lucene style query string.
	 * 
	 * @param crit
	 * @return
	 */
	protected String getSetCriterion(SetCriterion crit)
	{
		int op = crit.getSetOperation();
		String opStr = " AND ";
		if(op == SetCriterion.COMB_OR)
		{
			opStr = " OR ";
		}
		
		int numcrit = crit.getNumCriteria();
		if(numcrit < 1)
		{
			logger.warning("No criteria");
			return null;
		}
		
		Criterion tmpcrit = null;
		String tmp = null;
		String qs = null;
		
		tmpcrit = crit.getCriterion(0);
		tmp = getQS(tmpcrit);
		if(numcrit > 1)
		{
			qs = "("+tmp+")";
			for(int i=1; i<numcrit; i++)
			{
				tmpcrit = crit.getCriterion(i);
				tmp = getQS(tmpcrit);
				qs += opStr+"("+tmp+")";
			}
		}
		else
		{
			qs = tmp;
		}
		return qs;
	}

	/**
	 * Convert the set criterion to a lucene style query string.
	 * 
	 * @param crit
	 * @return
	 */
	protected Query getSetCriterionAsQ(SetCriterion crit, Analyzer lyzer)
	{
		Query q = null;
		
		int op = crit.getSetOperation();
		
		BooleanClause.Occur occur = BooleanClause.Occur.MUST;

		if(op == SetCriterion.COMB_OR)
		{
			occur = BooleanClause.Occur.SHOULD;
		}
		
		int numcrit = crit.getNumCriteria();
		if(numcrit < 1)
		{
			logger.warning("No criteria");
			return null;
		}
		
		BooleanQuery.Builder bqb  = new BooleanQuery.Builder();
		
		Criterion tmpcrit = null;
		Query tmp = null;
		
		tmpcrit = crit.getCriterion(0);
		tmp = getQ(tmpcrit,lyzer);
		if(numcrit > 1)
		{
			bqb.add(tmp,occur);
			for(int i=1; i<numcrit; i++)
			{
				tmpcrit = crit.getCriterion(i);
				tmp = getQ(tmpcrit,lyzer);
				bqb.add(tmp,occur);
			}
		}
		else
		{
			q = tmp;
		}
		
		q = bqb.build();
		return q;
	}

	/**
	 * Fetch the nodes by id and then return as a map.
	 * 
	 * @param ids
	 * @return
	 * @throws NoGraphException
	 */
	protected Map<String,Node> buildNodeMap(List<String> ids) throws NoGraphException
	{
		Map<String,Node> m = null;
		if(ids == null || ids.size() == 0) return m;
		
		try
		{
			BooleanQuery.Builder bqb  = new BooleanQuery.Builder();
			int size = ids.size();
			logger.info("Building node map for " + size + " nodes");
			
			BooleanQuery.setMaxClauseCount(size);
			
			String id = null;
			for(int i=0; i<size; i++)
			{
				id = ids.get(i);
				bqb.add(new TermQuery(new Term(ID_KEY,id)), BooleanClause.Occur.SHOULD);
			}
			
			BooleanQuery q = bqb.build();
			
			List<Document> docs = null;			

			
			docs = nodeIndex.search(q);
			
			List<Node> nodes = getNodesFromDocs(docs);
			
			size = nodes.size();
			m = new HashMap<String,Node>(size);
			Node n = null;
			for(int i=0; i<size; i++)
			{
				n = nodes.get(i);
				id = n.getID();
				m.put(id, n);
			}
		}
		catch(Exception ex)
		{
			logger.log(Level.SEVERE, "Error searching nodes", ex);
			throw new NoGraphException("Error searching nodes", ex);
		}

		
		return m;
	}
	
	/**
	 * The relationships have node ids, now let's turn them into full nodes.
	 * 
	 * @param rels
	 * @throws NoGraphException
	 */
	protected void populateNodesForRels(List<Relationship> rels) throws NoGraphException
	{
		if(rels == null || rels.size() == 0) return;
		
		Map<String,Node> nodeMap = new HashMap<String,Node>();
		
		int size = rels.size();
		Node n = null;
		String id = null;
		Relationship r = null;
		
		Map<String,String> mids = new HashMap<String,String>();
		
		String hold = "";
		
		// first pass is to build ids
        for(int i=0; i<size; i++)
        {
        	r = rels.get(i);
        	mids.put(r.getNode1ID(),hold);
        	mids.put(r.getNode2ID(),hold);
        }
        
        List<String> ids = new ArrayList<String>(mids.keySet());
        nodeMap = buildNodeMap(ids);
        if(nodeMap == null)
        {
        	nodeMap = new HashMap<String,Node>();
        }
        
        for(int i=0; i<size; i++)
        {
        	r = rels.get(i);

        	// we should do a bulk query, which is what buildNodeMap does, but slow and steady will win the race for now
        	id = r.getNode1ID();
        	n = nodeMap.get(id);
        	if(n == null)
        	{
        		n = getNode(id);
        		nodeMap.put(id, n);
        	}
        	r.setNode1(n);
        	
        	id = r.getNode2ID();
        	n = nodeMap.get(id);
        	if(n == null)
        	{
        		n = getNode(id);
        		nodeMap.put(id, n);
        	}
        	r.setNode2(n);
        }
	}
	
	/**
	 * Convert Lucene document to node, filling the instance if not null, creating one otherwise.
	 * 
	 * @param doc
	 * @param n
	 * @return
	 */
	protected Node docToNode(Document doc, Node n)
	{
		if(doc == null) return null;
		if(n == null) n = new GenericNode();
		
		Map<String,Object> map = LuceneUtil.fromDoc(doc);
		
		Object val = null;
		
		val = map.get(ID_KEY);
		if(val != null)
		{
			n.setID(String.valueOf(val));
		}
		map.remove(ID_KEY);
		
		val = map.get(TYPE_KEY);
		if(val != null)
		{
			n.setType(String.valueOf(val));
		}
		map.remove(TYPE_KEY);
		
		n.setPropertyMap(map);
		
		return n;
	}
	
	protected Document nodeToDoc(Node n, Document doc)
	{
		if(n == null) return null;
		if(doc == null) doc = new Document();
		
		Map<String,Object> map = n.getPropertyMap();
		map.put(ID_KEY, n.getID());
		map.put(TYPE_KEY, n.getType());
		
		// stored value
		if(map.containsKey(BasePropertyHolder.SV_KEY))
		{
			String sv = n.getStoredValue();
			map.remove(BasePropertyHolder.SV_KEY);
			if(sv != null)
			{
				doc.add(new StoredField(BasePropertyHolder.SV_KEY,sv));
			}
		}
		
		LuceneUtil.toDoc(map, doc, true);
		
		return doc;
	}
	
	protected Relationship docToRel(Document doc, Relationship r)
	{
		if(doc == null) return null;
		if(r == null) r = new GenericRelationship();
		
		Map<String,Object> map = LuceneUtil.fromDoc(doc);
		
		Object val = null;
		
		val = map.get(ID_KEY);
		if(val != null)
		{
			r.setID(String.valueOf(val));
		}
		map.remove(ID_KEY);
		
		val = map.get(TYPE_KEY);
		if(val != null)
		{
			r.setType(String.valueOf(val));
		}
		map.remove(TYPE_KEY);
		
		val = map.get(N1_KEY);
		if(val != null)
		{
			Node n = new GenericNode();
			n.setID(String.valueOf(val));
			r.setNode1(n);
			val = map.get(N1_TYPEKEY);
			if(val != null)
			{
				n.setType(String.valueOf(val));
			}
		}
		map.remove(N1_KEY);
		map.remove(N1_TYPEKEY);
		
		val = map.get(N2_KEY);
		if(val != null)
		{
			Node n = new GenericNode();
			n.setID(String.valueOf(val));
			r.setNode2(n);
			val = map.get(N2_TYPEKEY);
			if(val != null)
			{
				n.setType(String.valueOf(val));
			}
		}
		map.remove(N2_KEY);
		map.remove(N2_TYPEKEY);
		
		r.setPropertyMap(map);
		
		return r;
	}
	
	protected Document relToDoc(Relationship r, Document doc)
	{
		if(r == null) return null;
		
		Map<String,Object> map = r.getPropertyMap();
		map.put(ID_KEY, r.getID());
		map.put(TYPE_KEY, r.getType());
		Node n = null;
		
		n = r.getNode1();
		if(n == null) return null; // must have a node1
		if(n.getID() == null) return null;
		map.put(N1_KEY, n.getID());
		if(n.getType() != null) map.put(N1_TYPEKEY, n.getType()); // type can be null?
		
		n = r.getNode2();
		if(n == null) return null; // must have a node2
		if(n.getID() == null) return null;
		map.put(N2_KEY, n.getID());
		if(n.getType() != null) map.put(N2_TYPEKEY, n.getType()); // type can be null?
		
		if(doc == null) doc = new Document();
		
		// stored value
		if(map.containsKey(BasePropertyHolder.SV_KEY))
		{
			String sv = r.getStoredValue();
			map.remove(BasePropertyHolder.SV_KEY);
			if(sv != null)
			{
				doc.add(new StoredField(BasePropertyHolder.SV_KEY,sv));
			}
		}
		
		LuceneUtil.toDoc(map, doc, true);
		
		return doc;
	}

	protected String getNextNodeID()
	{
		String id = null;
		synchronized(idsync)
		{
			long iid = idgen.incrementAndGet();
			id = String.valueOf(iid);
			writeNewID();
		}
		return id;
	}
	
	protected String getNextRelID()
	{
		// share a common incrementer
		return getNextNodeID();
	}
	
	protected List<String> getNextNodeIDs(int count)
	{
		List<String> out = new ArrayList<String>(count);
		synchronized(idsync)
		{
			long iid = 0;
			for(int i=0; i<count; i++)
			{
				iid = idgen.incrementAndGet();
				out.add(String.valueOf(iid));
			}
			writeNewID();
		}
		
		return out;
	}
	
	protected List<String> getNextRelIDs(int count)
	{
		// share a common incrementer
		return getNextNodeIDs(count);
	}
	
	/**
	 * Commit the new id to disk
	 */
	protected void writeNewID()
	{
		// should only be called from within a synchronized block, but confirm
		synchronized(idsync)
		{
			// TODO consider using the write lock mechanism on the Lucene directory
			FileWriter fw = null;
			try
			{
				fw = new FileWriter(idFile);
				fw.write(String.valueOf(idgen.get()));
				fw.flush();
				fw.close();
				fw = null;
			}
			catch(Exception ex)
			{
				logger.log(Level.SEVERE, "Error commiting id to disk", ex);
			}
			finally
			{
				if(fw != null)try{fw.close();}catch(Exception ex){};
			}
			
		} // end sync
	}

	protected void loadGraphMeta()
	{
		synchronized(metasync)
		{
			graphMeta = new GraphMeta();
			File f = new File(metaFile);
			if(f.exists())
			{
				String json = FileUtil.getStringFromFile(metaFile);
				graphMeta.fromJSONString(json);
			}	
		}		
	}
	
	/**
	 * Most bulk inserts are going to be for identical property types, so we don't need to see each one.
	 * 
	 * @param nodes
	 */
	protected void sampleNodeMeta(List<Node> nodes) 
	{
		if(nodes == null || nodes.size() == 0) return;
		
		int size = nodes.size();
		if(size < 10)
		{
			for(int i=0; i<size; i++)
			{
				graphMeta.updateNodeMeta(nodes.get(i));
			}
		}
		else
		{
			graphMeta.updateNodeMeta(nodes.get(0));
			graphMeta.updateNodeMeta(nodes.get(size-1));
			graphMeta.updateNodeMeta(nodes.get(size/2));
			graphMeta.updateNodeMeta(nodes.get(size/4));
			graphMeta.updateNodeMeta(nodes.get(3*size/4));
		}
		writeGraphMeta();
	}

	protected void sampleRelMeta(List<Relationship> rels) 
	{
		if(rels == null || rels.size() == 0) return;
		// sample first middle and last
		int size = rels.size();
		if(size < 5)
		{
			for(int i=0; i<size; i++)
			{
				graphMeta.updateRelationshipMeta(rels.get(i));
			}
		}
		else
		{
			graphMeta.updateRelationshipMeta(rels.get(0));
			graphMeta.updateRelationshipMeta(rels.get(size-1));
			graphMeta.updateRelationshipMeta(rels.get(size/2));
		}
		writeGraphMeta();
	}

	
	protected void writeGraphMeta()
	{
		synchronized(metasync)
		{
			FileWriter fw = null;
			try
			{
				fw = new FileWriter(metaFile);
				fw.write(graphMeta.toJSONString());
				fw.flush();
				fw.close();
				fw = null;
			}
			catch(Exception ex)
			{
				logger.log(Level.SEVERE, "Error commiting meta to disk", ex);
			}
			finally
			{
				if(fw != null)try{fw.close();}catch(Exception ex){};
			}
			
		} // end sync
	}
	
	@Override
	public List<Relationship> findRelatedNodes(Node n) throws NoGraphException 
	{
		return findRelatedNodes(n.getID());
	}

	@Override
	public List<Relationship> findRelatedNodes(String id) throws NoGraphException 
	{
		List<Relationship> rels = null;
		
		//String str = "("+N1_KEY+"="+id + ") OR (" + N2_KEY +"="+id+")";
		
		TermQuery tq1 = new TermQuery(new Term(N1_KEY,id));
		TermQuery tq2 = new TermQuery(new Term(N2_KEY,id));
		
		
		BooleanQuery bq = new BooleanQuery.Builder()
			    .add(tq1, BooleanClause.Occur.SHOULD)
			    .add(tq2, BooleanClause.Occur.SHOULD)
			    .build();
		
		List<Document> docs = relIndex.search(bq);
		
		if(docs == null || docs.size() == 0)
		{
			logger.info("No relationships found");
		}
		else
		{
			logger.info("Found " + docs.size() + " relationships");
		}
		
		rels = getRelsFromDocs(docs,true);
		
		return rels;
	}

	@Override
	public long countNodes(String type) throws NoGraphException 
	{
		// the type may need to be analyzed
		if(type != null)
		{
			List<String> strs = analyze(type,nodeIndex.getAnalyzer());
			if(strs != null && strs.size() > 0)
			{
				type = strs.get(0);
			}
		}
		int count = nodeIndex.count(new TermQuery(new Term(TYPE_KEY,type)));
		return count;
	}

	@Override
	public long countRelationships(String type) throws NoGraphException 
	{
		// the type may need to be analyzed
		if(type != null)
		{
			List<String> strs = analyze(type,relIndex.getAnalyzer());
			if(strs != null && strs.size() > 0)
			{
				type = strs.get(0);
			}
		}

		int count = relIndex.count(new TermQuery(new Term(TYPE_KEY,type)));
		return count;
	}

	@Override
	public Map<String, Long> getNodeCountsByType() throws NoGraphException {
		Map<String,Long> m = new HashMap<String,Long>();
		
		try
		{
			Term t = null;
			TermQuery tq = null;
			
			List<String> types = nodeIndex.getTermsForField(TYPE_KEY);
			if(types != null)
			{
				int size = types.size();
				
				String type = null;
				Long cnt = null;
				for(int i=0; i<size; i++)
				{
					type = types.get(i);
					t = new Term(TYPE_KEY,type);
					tq = new TermQuery(t);
					cnt = (long)nodeIndex.count(tq);
					m.put(type, cnt);
				}
			}
			else
			{
				logger.warning("Types were NULL!");
			}
		}
		catch(Exception ex)
		{
			logger.log(Level.WARNING, "Error getting counts", ex);
		}
		return m;
	}

	@Override
	public Map<String, Long> getRelationshipCountsByType() throws NoGraphException {
		Map<String,Long> m = new HashMap<String,Long>();
		
		try
		{
			Term t = null;
			TermQuery tq = null;
			
			List<String> types = relIndex.getTermsForField(TYPE_KEY);
			int size = types.size();
			
			String type = null;
			Long cnt = null;
			for(int i=0; i<size; i++)
			{
				type = types.get(i);
				t = new Term(TYPE_KEY,type);
				tq = new TermQuery(t);
				cnt = (long)relIndex.count(tq);
				m.put(type, cnt);
			}
		}
		catch(Exception ex)
		{
			logger.log(Level.WARNING, "Error getting counts", ex);
		}
		
		return m;
	}

	@Override
	public List<Node> findNodes(String type, String key, Object val) throws NoGraphException 
	{
		return findNodes(type,key,val,0);
	}

	@Override
	public List<Node> findNodes(String type, String key, Object val, int maxResults) throws NoGraphException 
	{
		boolean tnull = type==null;
		boolean vnull = val == null;
		boolean knull = key == null;
		
		if(tnull && knull) return null;
		
		if(!knull && vnull)
		{
			logger.warning("Unable to search for null values");
			return null;
		}
		
		
		List<Node> nodes = null;
		
		try
		{
			List<Document> docs = null;			
			
			Query q = getQuery(key,val,nodeIndex.getAnalyzer());
			QueryParser qp = new QueryParser(key,nodeIndex.getAnalyzer());
			Query q2 = qp.parse(TYPE_KEY+":"+String.valueOf(type));
			
			if(!(knull || tnull))
			{
				BooleanQuery booleanQuery = new BooleanQuery.Builder()
					    .add(q, BooleanClause.Occur.MUST)
					    .add(q2, BooleanClause.Occur.MUST)
					    .build();
				
				q = booleanQuery;
			}
			else
			{
				// if the key is null, just use the type query
				// if the type is null, then q is already the key/value query
				if(knull)
				{
					q = q2;
				}
			}
			
			if(maxResults < 1)
			{
				docs = nodeIndex.search(q);
			}
			else
			{
				docs = nodeIndex.search(q,maxResults);
			}
			
			nodes = getNodesFromDocs(docs);
		}
		catch(Exception ex)
		{
			logger.log(Level.SEVERE, "Error searching nodes", ex);
			throw new NoGraphException("Error searching nodes", ex);
		}
		
		return nodes;
	}

	@Override
	public List<Relationship> findRelationships(String type, String key, Object val, boolean fetchNodes)
			throws NoGraphException 
	{
		return findRelationships(type,key,val,fetchNodes,0);
	}

	@Override
	public List<Relationship> findRelationships(String type, String key, Object val, boolean fetchNodes, int maxResults)
			throws NoGraphException {
		boolean tnull = type==null;
		boolean vnull = val == null;
		boolean knull = key == null;
		
		if(tnull && knull) return null;
		
		if(!knull && vnull)
		{
			logger.warning("Unable to search for null values");
			return null;
		}
				
		List<Relationship> rels = null;
		
		try
		{
			List<Document> docs = null;
						
			Query q = getQuery(key,val,relIndex.getAnalyzer());
			QueryParser qp = new QueryParser(key,relIndex.getAnalyzer());
			Query q2 = qp.parse(TYPE_KEY+":"+String.valueOf(type));
		
			
			if(!(knull || tnull))
			{
				BooleanQuery booleanQuery = new BooleanQuery.Builder()
					    .add(q, BooleanClause.Occur.MUST)
					    .add(q2, BooleanClause.Occur.MUST)
					    .build();
				
				q = booleanQuery;
			}
			else
			{
				// if the key is null, just use the type query
				// if the type is null, then q is already the key/value query
				if(knull)
				{
					q = q2;
				}
			}

			
			if(maxResults < 1)
			{
				docs = relIndex.search(q);
			}
			else
			{
				docs = relIndex.search(q,maxResults);
			}
			
			rels = getRelsFromDocs(docs, fetchNodes);
		}
		catch(Exception ex)
		{
			logger.log(Level.SEVERE, "Error searching rels", ex);
			throw new NoGraphException("Error searching rels", ex);
		}
		
		return rels;
	}

	@Override
	public List<String> getNodeTypes() throws NoGraphException 
	{
		List<String> out = null;
		
		try
		{			
			List<String> types = nodeIndex.getTermsForField(TYPE_KEY);
			if(types != null)
			{
				out = new ArrayList<String>(types);
			}
			else
			{
				logger.warning("Types were NULL!");
			}
		}
		catch(Exception ex)
		{
			logger.log(Level.WARNING, "Error getting node types", ex);
		}
		
		return out;
	}

	@Override
	public List<String> getRelationshipTypes() throws NoGraphException 
	{
		List<String> out = null;
		
		try
		{			
			List<String> types = relIndex.getTermsForField(TYPE_KEY);
			if(types != null)
			{
				out = new ArrayList<String>(types);
			}
			else
			{
				logger.warning("Types were NULL!");
			}
		}
		catch(Exception ex)
		{
			logger.log(Level.WARNING, "Error getting rel types", ex);
		}
		
		return out;
	}

	@Override
	public List<String> getPropertyNamesForNodeType(String type) throws NoGraphException 
	{
		if(graphMeta == null) return null;
		
		return graphMeta.getPropertiesForNodeType(type);
	}

	@Override
	public List<String> getPropertyNamesForRelationshipType(String type) throws NoGraphException 
	{
		if(graphMeta == null) return null;
		
		return graphMeta.getPropertiesForRelationshipType(type);
	}

	@Override
	public Map<String, List<String>> getPropertyNamesByNodeType() throws NoGraphException 
	{
		Map<String,List<String>> out = new HashMap<String,List<String>>();
		
		List<String> types = this.getNodeTypes();
		if(types != null && graphMeta != null)
		{
			List<String> props = null;
			String type = null;
			int size = types.size();
			for(int i=0; i<size; i++)
			{
				type = types.get(i);
				props = graphMeta.getPropertiesForNodeType(type);
				out.put(type, props);
			}
		}
		return out;
	}

	@Override
	public Map<String, List<String>> getPropertyNamesByRelationshipType() throws NoGraphException 	
	{
		Map<String,List<String>> out = new HashMap<String,List<String>>();
		
		List<String> types = this.getRelationshipTypes();
		if(types != null && graphMeta != null)
		{
			List<String> props = null;
			String type = null;
			int size = types.size();
			for(int i=0; i<size; i++)
			{
				type = types.get(i);
				props = graphMeta.getPropertiesForRelationshipType(type);
				out.put(type, props);
			}
		}
		return out;
	}
	
	/**
	 * Randomly samples the node and rel index to generate GraphMeta.
	 * 
	 * @param sampleSizeAsPercent
	 * @return
	 */
	public GraphMeta generateMetaFromIndexSample(double sampleSizeAsPercent)
	{
		GraphMeta gm = new GraphMeta();
		Random rand = new Random();
		try
		{
			int max = nodeIndex.maxDoc();
			int numSamples = (int)(((double)max)*sampleSizeAsPercent);
			
			// do at least 100 if there are at least that many in the index
			if(numSamples < 100)numSamples = Math.min(100, max);
			
			int docID = -1;
			Document doc = null;
			Node n = null;
			
			for(int i=0; i<numSamples; i++)
			{
				docID = rand.nextInt(max);
				doc = nodeIndex.doc(docID);
				n = docToNode(doc,null);
				gm.updateNodeMeta(n);
			}
		}
		catch(Exception ex)
		{
			logger.log(Level.WARNING,"Error sampling nodes",ex);
		}
		
		try
		{
			int max = relIndex.maxDoc();
			int numSamples = (int)(((double)max)*sampleSizeAsPercent);
			
			// do at least 100 if there are at least that many in the index
			if(numSamples < 100)numSamples = Math.min(100, max);
			
			int docID = -1;
			Document doc = null;
			Relationship rel = null;
			
			for(int i=0; i<numSamples; i++)
			{
				docID = rand.nextInt(max);
				doc = relIndex.doc(docID);
				rel = docToRel(doc,null);
				gm.updateRelationshipMeta(rel);
			}
		}
		catch(Exception ex)
		{
			logger.log(Level.WARNING,"Error sampling nodes",ex);
		}
		return gm;
	}
	
	/**
	 * Replace this managers graph meta data with the provided one.
	 * 
	 * @param gm
	 */
	public void replaceGraphMeta(GraphMeta gm)
	{
		try
		{
			synchronized(metasync)
			{
				graphMeta = gm;
				writeGraphMeta();
				loadGraphMeta();
			}
		}
		catch(Exception ex)
		{
			logger.log(Level.SEVERE,"Error updating meta",ex);
		}
	}

	@Override
	public void setDataDecorator(DataDecorator decorator) 
	{
		this.decorator = decorator;
		if(decorator != null)
		{
			this.decorateNodes = decorator.decoratesNodes();
			this.decorateRels = decorator.decoratesRelationships();
		}
	}

	@Override
	public DataDecorator getDataDecorator() 
	{
		return decorator;
	}

	@Override
	public void clearDataDecorator() 
	{
		decorator = null;
	}

	protected void decorateNode(Node n)
	{
		if(!decorateNodes) return;
		decorator.decorateNode(n);
	}

	protected void decorateRel(Relationship r)
	{
		if(!decorateRels) return;
		decorator.decorateRelationship(r);
	}

	protected void decorateNodes(List<Node> nodes)
	{
		if(!decorateNodes) return;
		Node n = null;
		int size = nodes.size();
		for(int i=0; i<size; i++)
		{
			n = nodes.get(i);
			decorator.decorateNode(n);
		}
	}

	protected void decorateRels(List<Relationship> rels)
	{
		if(!decorateRels) return;
		Relationship r = null;
		int size = rels.size();
		for(int i=0; i<size; i++)
		{
			r = rels.get(i);
			decorator.decorateRelationship(r);
		}
	}
	
    /**
     * Search for paths that link one node to other nodes.
     * 
     * @param startCriterion
     * @param relationshipCriterion
     * @param endCriterion
     * @param maxLength
     * @param maxHits
     * @throws NoGraphException
     * @return
     */
    public List<Path> findPaths(Criterion startCriterion, Criterion relationshipCriterion, Criterion endCriterion,
    		                    int maxLength, int maxHits) throws NoGraphException
    {
    	return null;
    }
    

}
