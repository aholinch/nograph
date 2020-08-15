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

/**
 * A relationship connects one node to another.  The relationship may or may not be directional.
 * If directional it is from Node 1 to Node 2.
 * 
 * @author aholinch
 *
 */
public interface Relationship extends PropertyHolder 
{
    public Node getNode1();
    
    public void setNode1(Node node);
    
    public Node getNode2();
    
    public void setNode2(Node node);
    
    public String getNode1ID();
    
    public String getNode2ID();
    
    public String getNode1Type();
    
    public String getNode2Type();
    
}
