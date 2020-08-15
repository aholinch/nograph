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

import java.util.List;
import java.util.Map;

import org.nograph.GraphQuery.Criterion;

/**
 * Implement this interface to manage the connection to graph datastore.
 * 
 * @author aholinch
 *
 */
public interface GraphManager 
{
	/**
	 * Manager name.  Used to sort out config for multiple graphs.
	 * @param str
	 */
	public void setName(String str);
	
	/**
	 * Manager name.  Used to sort out config for multiple graphs.
	 * @return
	 */
	public String getName();
	
	/**
	 * Handles both insert and update.
	 * 
	 * @param n
	 * @throws NoGraphException
	 */
    public void saveNode(Node n) throws NoGraphException;
    
    /**
     * Delete the node.
     * 
     * @param n
     * @throws NoGraphException
     */
    public void deleteNode(Node n) throws NoGraphException;
    
    /**
     * Delete the node specified by the id.
     * 
     * @param id
     * @throws NoGraphException
     */
    public void deleteNode(String id) throws NoGraphException;
    
    /**
     * Retrieve the node specified by the id.
     * 
     * @param id
     * @return
     * @throws NoGraphException
     */
    public Node getNode(String id) throws NoGraphException;
    
    /**
     * Assumes all nodes are to be inserted.  Can be very optimized.
     * 
     * @param nodes
     * @throws NoGraphException
     */
    public void ingestNodes(List<Node> nodes) throws NoGraphException;
    
    /**
     * Handles both insert and update and may be slow.
     * 
     * @param nodes
     * @throws NoGraphException
     */
    public void saveNodes(List<Node> nodes) throws NoGraphException;
    
    /**
     * Delete the list of nodes.
     * 
     * @param nodes
     * @throws NoGraphException
     */
    public void deleteNodes(List<Node> nodes) throws NoGraphException;
    
    /**
     * Delete the list of node ids.
     * 
     * @param ids
     * @throws NoGraphException
     */
    public void deleteNodesByID(List<String> ids) throws NoGraphException;
    
    /**
     * Handles both insert and update.
     * 
     * @param r
     * @throws NoGraphException
     */
    public void saveRelationship(Relationship r) throws NoGraphException;
    
    /**
     * Delete the relationship.
     * 
     * @param r
     * @throws NoGraphException
     */
    public void deleteRelationship(Relationship r) throws NoGraphException;
    
    /**
     * Delete the relationship specified by the id.
     * 
     * @param id
     * @throws NoGraphException
     */
    public void deleteRelationship(String id) throws NoGraphException;
    
    /**
     * Fetch relationship specified by id.  Optionally, fetch the linked nodes.
     * 
     * @param id
     * @param fetchNodes
     * @return
     * @throws NoGraphException
     */
    public Relationship getRelationship(String id, boolean fetchNodes) throws NoGraphException;
    
    /**
     * Ingest the relationships.  IDs will be assigned.
     * 
     * @param rels
     * @throws NoGraphException
     */
    public void ingestRelationships(List<Relationship> rels) throws NoGraphException;
    
    /**
     * Save the relationships by assigning ids to new ones and updating existing ones.
     * 
     * @param rels
     * @throws NoGraphException
     */
    public void saveRelationships(List<Relationship> rels) throws NoGraphException;
    
    /**
     * Delete the relationships.
     * 
     * @param rels
     * @throws NoGraphException
     */
    public void deleteRelationships(List<Relationship> rels) throws NoGraphException;
    
    /**
     * Delete the relationships specified by the ids.
     * 
     * @param ids
     * @throws NoGraphException
     */
    public void deleteRelationshipsByID(List<String> ids) throws NoGraphException;
    
    /**
     * Find the nodes with the given value for the specified field name or key.
     * 
     * @param key
     * @param val
     * @return
     * @throws NoGraphException
     */
    public List<Node> findNodes(String key, Object val) throws NoGraphException;
    
    /**
     * Find the nodes with the given value for the specified field name or key.
     * 
     * @param type
     * @param key
     * @param val
     * @return
     * @throws NoGraphException
     */
    public List<Node> findNodes(String type, String key, Object val) throws NoGraphException;

    /**
     * Find the nodes with the given value for the specified field name or key.
     * 
     * @param type
     * @param key
     * @param val
     * @param maxResults
     * @return
     * @throws NoGraphException
     */
    public List<Node> findNodes(String type, String key, Object val, int maxResults) throws NoGraphException;
    
    /**
     * Find the relationships with the given value for the specified field name or key.  The linked nodes can optionally be fetched.
     * 
     * @param key
     * @param val
     * @param fetchNodes
     * @return
     * @throws NoGraphException
     */
    public List<Relationship> findRelationships(String key, Object val, boolean fetchNodes) throws NoGraphException;
    
    /**
     * Find the relationships with the given value for the specified field name or key.  The linked nodes can optionally be fetched.
     * 
     * @param type
     * @param key
     * @param val
     * @param fetchNodes
     * @return
     * @throws NoGraphException
     */
    public List<Relationship> findRelationships(String type, String key, Object val, boolean fetchNodes) throws NoGraphException;
    
    /**
     * Find the relationships with the given value for the specified field name or key.  The linked nodes can optionally be fetched.
     * 
     * @param type
     * @param key
     * @param val
     * @param fetchNodes
     * @param maxResults
     * @return
     * @throws NoGraphException
     */
    public List<Relationship> findRelationships(String type, String key, Object val, boolean fetchNodes, int maxResults) throws NoGraphException;
    
    /**
     * Find the nodes that match the graph query.
     * 
     * @param query
     * @return
     * @throws NoGraphException
     */
    public List<Node> findNodes(GraphQuery query) throws NoGraphException;
    
    /**
     * Find the relationships that match the graph query.
     * 
     * @param query
     * @return
     * @throws NoGraphException
     */
    public List<Relationship> findRelationships(GraphQuery query) throws NoGraphException;
    
    /**
     * Find the relationships and fetch the linked nodes that connect to this node.
     * 
     * @param n
     * @return
     * @throws NoGraphException
     */
    public List<Relationship> findRelatedNodes(Node n) throws NoGraphException;
    
    /**
     * Find the relationships and fetch the linked nodes that connect to this node.
     * 
     * @param id
     * @return
     * @throws NoGraphException
     */
    public List<Relationship> findRelatedNodes(String id) throws NoGraphException;
    
    /**
     * Count the number of nodes for the specified type.
     * 
     * @param type
     * @return
     * @throws NoGraphException
     */
    public long countNodes(String type) throws NoGraphException;
    
    /**
     * Count the number of relationships for the specified type.
     * 
     * @param type
     * @return
     * @throws NoGraphException
     */
    public long countRelationships(String type) throws NoGraphException;
    
    /**
     * Return the list of node types.
     * 
     * @return
     * @throws NoGraphException
     */
    public List<String> getNodeTypes() throws NoGraphException;
    
    /**
     * Return the list of relationship types.
     * 
     * @return
     * @throws NoGraphException
     */
    public List<String> getRelationshipTypes() throws NoGraphException;
    
    /** 
     * Get the counts of each node type.
     * 
     * @return
     * @throws NoGraphException
     */
    public Map<String,Long> getNodeCountsByType() throws NoGraphException;
    
    /**
     * Get the counts of each relationship type.
     * 
     * @return
     * @throws NoGraphException
     */
    public Map<String,Long> getRelationshipCountsByType() throws NoGraphException;
    
    /**
     * Returns the list of known property names for the specified node type.
     * 
     * @param type
     * @return
     * @throws NoGraphException
     */
    public List<String> getPropertyNamesForNodeType(String type) throws NoGraphException;
    
    /**
     * Returns the list of known property names for the specified relationship type.
     * 
     * @param type
     * @return
     * @throws NoGraphException
     */
    public List<String> getPropertyNamesForRelationshipType(String type) throws NoGraphException;
    
    /**
     * Provide the list of property names for the known node types.
     * 
     * @return
     * @throws NoGraphException
     */
    public Map<String,List<String>> getPropertyNamesByNodeType() throws NoGraphException;
    
    /**
     * Provide the list of property names for the known relationship types.
     * 
     * @return
     * @throws NoGraphException
     */
    public Map<String,List<String>> getPropertyNamesByRelationshipType() throws NoGraphException;
    
    /**
     * Add a data decorator.
     * 
     * @param decorator
     */
    public void setDataDecorator(DataDecorator decorator);
    
    /**
     * Return the current decorator.
     * 
     * @return
     */
    public DataDecorator getDataDecorator();
    
    /**
     * Clear the decorator.
     */
    public void clearDataDecorator();
    
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
    		                    int maxLength, int maxHits) throws NoGraphException;
    
}
