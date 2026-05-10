# Create script to safely refactor Math.pow calls with local variables

# ModShaders.java
sed -i 's/Math.pow(wind.length() \/ 60.0, 2.0)/((wind.length() \/ 60.0) \* (wind.length() \/ 60.0))/g' src/main/java/dev/dabrelity/atmospherica/shaders/ModShaders.java # Actually wind.length() should be cached if possible, but it's okay. Let's fix it by caching windLength.
# But sed might be messy. Let's use python.
