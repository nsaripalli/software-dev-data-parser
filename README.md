# Algorithm

Please mount Part1 into `/test` in the docker container

### Steps:
  1. Using a file source iterate over the lines, parsing to figure out the number of elements
  2. Using a fresh source iterator, parse the lines (assuming missing value to reach the needed length) in order to infer the schema.
  3. Based on the given queries, either respond to the trivial one (column type) or parse the value requested and cast to the schema type as needed (with pattern matching).
  
  - We handle parsing with a regex that produces an iterator of the elements casted to the lowest possible type. 
    We use pattern matching on their types to convert as needed and tie them to our custom enum for type.
  
# Design
We took a functional approach to the problem, using immutable data structures and values (though there may be some mutation under the hood, ie iterators)

Each step of the algorithm is done with list abstractions, most often fold, over file sources.

We maintained an O(n) runtime (each step is linear by the number of characters in 500 lines at most) and hold no more than one line in RAM at a time in order to maintain good memory performance

# Design Decisions
 - If any part of the SOR is ambiguous we assume the least intervention in order to stick with what the user likely intended. Ie <><> is two empty values, not "><"

 - We assume queries are based on the whole file and the 500 lines specified is used for schema generation. This is because, it is more useful for a user to be able to 
   query any part of the file. We saw no need to limit the use case. If a user wants to query about the 500 lines only,
   they still can. This is also in line with Piazza post @256
   
 - If the user queries about data that would violate the schema, we inform them of such and then
   throw the parsing / type matching error for logging and bug reporting purposes. We think that it is more
   important to include this information and allow users to submit fully - fledged bug reports if they think 
   a bug occurred (as opposed to an expected error) in order to allow for more open communciation between
   the engineers and the users, especially since this will be an internal tool.
   
 - We only truncate the first line if from is >0 as it is not needed otherwise. 
   We do the same for the last line based on if from + len is greater than the length of the file.
   
 - We allow for at most one of each query. If a query is asked more than once, the behavior is undefined. Multiple different queries can be made. 