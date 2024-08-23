# nograph
NoSQL and GraphDB


## Dependencies
This software uses Lucene.  Previously it was written to work with 7.4.0.  The current version supports 9.11.1.  Unfortunately Lucene 9.x cannot read 7.x indexes so you will need to recreate any data stores built with the previous version of Lucene.
If you used a different backend datastore, this change should not affect you.

This software makes use of Sean Leary's https://github.com/stleary/JSON-java.  It has the package name of org.json.  Some other 3rd party libraries also use this code, but with other versions.  To avoid this problem, I've simply changed the package name.  Also, a few minor modifications for java.util.Date handling have been made. 
