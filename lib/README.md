# Why a lib dir?
Dependency management quickly gets complicated, even for small projects.

Whenever this project needs a 3rd party library, it goes in this folder.  If a library requires multiple jar files, then put it in a subdir.
Include any relevant LICENSE files as well.

# But this is not the WAY it is done
Well, I don't always have an internet connection on the machines I develop.  Also, Maven, Gradle, pip, nm, etc. have all taken me to places where ciruclar dependencies could not be resolved.

# Won't you miss out on critical updates?
It's a possibility.  Periodic checks for new versions of third party libraries are important.  However, not all updates are "good."  For example, Lucene likes to make updates that break backwards compatibility.  So even if I was using Maven, I'd need to evaluate all new versions, and then set a maximum version number in my build scripts.

There are pros and cons to both approaches.  This is how I'm doing things.