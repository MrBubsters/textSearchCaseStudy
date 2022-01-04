# Document Search Case Study

In this repository I explore a few different methods for searching for text in a collection of documents. Some methods are good, others are not. The goal of this is to showcase how advanced methods can often be slower when dealing with low scale load. 

## Testing
When running the main script it will prompt with command line arguments. To test each method simply follow the instructions like so:
```console
Would you like to search for a term? (y|n)
y
Enter a search term:
Roman
Search Method: 1) String match 2) Regex 3) Indexed
1
Search results:
hitchhikers.txt - 0 matches
french_armed_forces.txt - 5 matches
warp_drive.txt - 0 matches

Time of execution: 4ms
```

## Methods
In this experiment three different methods from naive to advanced were implemented. 
###naive method:
This method used basic looping over words and checking if the search term is in the word.
###regex method:
This method is using regex to speed up the process. By using a regex matcher the function searches the whole document for the keyword instead of going through each term.
###SQL method
To test a more advanced technique I setup a postgreSQL database in a virtual machine and stored the text in a table. 
The only preprocessing done was basic text indexing like so:
```sql
alter table public.text_documents add column indexed_text tsvector;
update public.text_documents set indexed_text = to_tsvector(sample_text);
create index text_idx on public.text_documents using gin (indexed_text)
```

I tested the indexed text by running the regex command against the ts_query on the index:
```sql
explain analyze select regexp_matches(sample_text, 'Roman', 'g') from public.text_documents td
---
Planning Time: 0.239 ms
Execution Time: 0.459 ms
```

```sql
explain analyze select * from public.text_documents td 
where indexed_text @@ to_tsquery('Roman')
---
Planning Time: 0.494 ms
Execution Time: 0.172 ms
```

## Results
Surprisingly the method that performed the worst was the SQL method. As you can see above the indexed search while execution time was significantly lower, the planning time made up for that. 
When running the text search just in base java, there were we see an even worse performance from using SQL.

I tested the methods using the same search term 'Roman' across all three methods. 
* naive avg: 4ms
* regex avg: 3ms
* SQL   avg: 23ms

When scaling the search terms up to 20 million (searched sequentially) the results remain the same. With regex showing the best performance and SQL having the worst performance.
* naive: ~10 min
* regex: ~3 min
* SQL: ~25 min

## Discussion
I was surprised to see that loading the data into a database and indexing the words actually performed worse. Possible reasons for that are many, but ultimately I think the biggest limitation was the SQL transaction time from the host to client.

As it was detailed above, just one search term request took 23ms to get from the client to host and return results. When running 20 million queries the execution time average dropped closer to the expected 0.5ms we saw running the test on the database. However, this is nowhere near the regex execution time of 0.05ms per search term. 

Ultimately, this test shows how in smaller cases such as this, the advantages that scalable methods are not actualized. While the test used 20 million searches, the total words to be searched through was still only 1144. In conclusion the majority of the time to execute was because of the volume of searches made.