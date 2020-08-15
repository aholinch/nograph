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
 * A basic interface for getting and setting IDs that can be strings or longs.
 * 
 * @author aholinch
 *
 */
public interface ID 
{
    public String getID();
    
    public void setID(String str);
    
    public Long getLongID();
    
    public void setLongID(Long num);
}
