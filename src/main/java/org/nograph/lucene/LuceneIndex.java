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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.nograph.NoGraphException;

/**
 * Manages an IndexWriter and an IndexSearcher for the same index on disk.
 * 
 * @author aholinch
 *
 */
public class LuceneIndex 
{
	private static final Logger logger = Logger.getLogger(LuceneIndex.class.getName());
	
    protected IndexWriter writer = null;
    protected SearcherManager searcherMan = null;
    protected Analyzer analyzer = null;
    
    protected int defaultMaxHits = 100000;
    protected String defaultField = "content";
    protected boolean readOnly = false;
    
    
    public LuceneIndex()
    {
    	init("test",null,false);
    }

    public LuceneIndex(String dir)
    {
    	init(dir,null,false);
    }
    
    public LuceneIndex(String dir, boolean readOnlyFlag)
    {
    	init(dir,null,readOnlyFlag);
    }
    
    public LuceneIndex(String dir, Analyzer analyzer)
    {
    	init(dir,analyzer,false);
    }
    
    public LuceneIndex(String dir, Analyzer analyzer, boolean readOnlyFlag)
    {
    	init(dir,analyzer,readOnlyFlag);
    }
    
    public LuceneIndex(Directory dir, Analyzer analyzer, boolean readOnlyFlag)
    {
    	init(dir,analyzer,readOnlyFlag);
    }
    
    protected void init(String dir, Analyzer analyzer, boolean readOnlyFlag)
    {
    	try
    	{
    		Directory diro = FSDirectory.open(Paths.get(dir));
    		init(diro,analyzer,readOnlyFlag);
    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.SEVERE, "Error initializing directory", ex);
    	}
    }
    
    protected void init(Directory dir, Analyzer analyzer, boolean readOnlyFlag)
    {
    	try
    	{
	    	if(analyzer == null)
	    	{
	    		analyzer = new StandardAnalyzer();
	    	}
	    	this.analyzer = analyzer;
	    	this.readOnly = readOnlyFlag;
	    	
	    	if(!readOnly)
	    	{
		        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		        iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
		        
		        writer = new IndexWriter(dir, iwc);
		        
		        searcherMan = new SearcherManager(writer,true,true,null);
	    	}
	    	else
	    	{
	    		// read only mode means just the searcher manager is created
	    		searcherMan = new SearcherManager(dir,null);
	    	}
    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.SEVERE, "Error initializing index", ex);
    	}
    }
    
    public Analyzer getAnalyzer()
    {
    	return analyzer;
    }
    
    public Directory getDirectory()
    {
    	return writer.getDirectory();
    }

    public int getDefaultMaxHits()
    {
    	return defaultMaxHits;
    }
    
    public void setDefaultMaxHits(int max)
    {
    	defaultMaxHits = max;
    }
    
    public String getDefaultField()
    {
    	return defaultField;
    }
    
    public void setDefaultFied(String field)
    {
    	defaultField = field;
    }
    
    public void commit()
    {
    	try
    	{
    		writer.commit();
    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.SEVERE, "Error commiting writer", ex);	
    	}
    }
    
    public void close()
    {
    	if(writer != null)
    	{
	    	try
	    	{
	    		writer.commit();
	    		writer.close();
	    	}
	    	catch(Exception ex)
	    	{
	    		logger.log(Level.SEVERE, "Error closing writer", ex);	
	    	}
    	}
    	
    	try
    	{
    		searcherMan.close();
    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.SEVERE, "Error closing searchers", ex);
    	}
    }
    
    public void saveDocument(Document doc) throws NoGraphException
    {
    	try
    	{
    		writer.addDocument(doc);
    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.WARNING, "Error saving doc", ex);
    		throw new NoGraphException("Error saving doc",ex);
    	}
    }
    
    public void saveDocuments(List<Document> docs) throws NoGraphException
    {
    	try
    	{
    		writer.addDocuments(docs);
    		

    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.WARNING, "Error saving docs", ex);
    		throw new NoGraphException("Error saving docs",ex);
    	}
    }
    
    public void deleteDocuments(Query q) throws NoGraphException
    {
    	try
    	{
    		writer.deleteDocuments(q);
    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.WARNING, "Error deleting document", ex);
    		throw new NoGraphException("Error deletint document", ex);
    	}
    }
    
    public void deleteDocument(String field, String val) throws NoGraphException
    {
    	Term t = new Term(field,val);
    	deleteDocuments(t);
    }
    
    public void deleteDocuments(Term t) throws NoGraphException
    {
    	try
    	{
    		writer.deleteDocuments(t);
    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.WARNING, "Error deleting document", ex);
    		throw new NoGraphException("Error deletint document", ex);
    	}
    }

    
    public void close(IndexSearcher is)
    {
    	try
    	{
    		searcherMan.release(is);
    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.WARNING, "Error closing searcher", ex);
    	}
    }
    
    public long deleteAll()
    {
    	long out = -1;

    	try
    	{
    		out = writer.deleteAll();
    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.WARNING, "Error deleting all", ex);
    	}
    	
    	return out;
    }
    
    public int maxDoc()
    {
    	int max = -1;
    	IndexSearcher is = null;
    	IndexReader ir = null;
    	try
    	{
    		searcherMan.maybeRefresh();

    		is = searcherMan.acquire();
    		ir = is.getIndexReader();
    		max = ir.maxDoc();
    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.WARNING, "Error getting max", ex);
    	}
    	finally
    	{
    		close(is);
    	}
    	
    	return max;
    }
    
    public Document doc(int id)
    {
    	Document doc = null;
    	IndexSearcher is = null;
    	try
    	{
    		searcherMan.maybeRefresh();

    		is = searcherMan.acquire();
    		is.getIndexReader().maxDoc();
    		doc = is.doc(id);
    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.WARNING, "Error getting doc", ex);
    	}
    	finally
    	{
    		close(is);
    	}
    	
    	return doc;
    }
    
    public int count(Query query)
    {
    	int count = 0;
    	IndexSearcher is = null;
    	try
    	{
    		searcherMan.maybeRefresh();

    		is = searcherMan.acquire();
    		
    		count = is.count(query);
    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.WARNING, "Error getting count", ex);
    	}
    	finally
    	{
    		close(is);
    	}
    	return count;
    }
    
    /**
     * Uses default analyzer to parse query.
     * 
     * @param str
     * @return
     */
    public List<Document> search(String str)
    {
    	return search(str,defaultMaxHits);
    }
    
    public List<Document> search(String str, String val)
    {
    	Query q = new TermQuery(new Term(str,val));
    	return search(q);
    }
    
    public List<Document> search(String str, int max)
    {
    	Query q = null;
    	try
    	{
    		QueryParser qp = new QueryParser(defaultField,analyzer);
    		q = qp.parse(str);
    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.SEVERE, "Error parsing query", ex);
    	}
    	
    	if(q == null)
    	{
    		return null;
    	}
    	
    	return search(q,max);
    }
    
    public List<Document> search(Query query)
    {
    	return search(query,defaultMaxHits);
    }
    
    public List<Document> search(Query query, int max)
    {
    
    	List<Document> docs = null;
    	
    	IndexSearcher is = null;
    	try
    	{
    		searcherMan.maybeRefresh();

    		is = searcherMan.acquire();
    		
    		TopDocs td = null;
    		
    		td = is.search(query, max);
    			
    		docs = new ArrayList<Document>((int)td.totalHits);
    		
    		ScoreDoc sds[] = td.scoreDocs;
    		int len = sds.length;
    		ScoreDoc sd = null;
    		for(int i=0; i<len; i++)
    		{
    			sd = sds[i];
    			docs.add(is.doc(sd.doc));
    		}
    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.WARNING, "Error getting count", ex);
    	}
    	finally
    	{
    		close(is);
    	}
    	
    	return docs;
    }
    
    /**
     * Use with care.
     * 
     * @return
     */
    public IndexWriter getWriter()
    {
    	return writer;
    }
    
    /**
     * Use with care, call close(IndexSearcher) on this class when done.
     * 
     * @return
     */
    public IndexSearcher getSearcher()
    {
    	IndexSearcher is = null;
    	
    	try
    	{
    		searcherMan.maybeRefresh();
    		is = searcherMan.acquire();
    	}
    	catch(Exception ex)
    	{
    		logger.log(Level.SEVERE,"Error getting searcher",ex);
    	}
    	
    	return is;
    }
    
    public List<String> getTermsForField(String field)
    {
    	List<String> terms = null;
    	
    	try
    	{
	        IndexReader reader = searcherMan.acquire().getIndexReader();
	        Terms ts = MultiFields.getTerms(reader, field);
	        TermsEnum te = ts.iterator();
	        terms = new ArrayList<String>(100);
	        
	        BytesRef br = null;
	        while(te.next() != null)
	        {
	        	br = te.term();
	        	terms.add(br.utf8ToString());
	        }
		}	
    	catch(Exception ex)
    	{
    		logger.log(Level.SEVERE,"Error getting terms",ex);
    	}
    	
    	return terms;
    }
}
