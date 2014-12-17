package de.ids_mannheim.korap.query.serialize;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.ids_mannheim.korap.query.poliqarp.PoliqarpPlusParser;
import de.ids_mannheim.korap.util.QueryException;
import de.ids_mannheim.korap.utils.JsonUtils;
import de.ids_mannheim.korap.utils.KorAPLogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author bingel, hanl
 */
public class QuerySerializer {
	
	public enum QueryLanguage {
		POLIQARPPLUS, ANNIS, COSMAS2, CQL, CQP
	}
	
	static HashMap<String, Class<? extends AbstractSyntaxTree>> qlProcessorAssignment;

	static {
		qlProcessorAssignment  = new HashMap<String, Class<? extends AbstractSyntaxTree>>();
		qlProcessorAssignment.put("poliqarpplus", PoliqarpPlusTree.class);
		qlProcessorAssignment.put("cosmas2", CosmasTree.class);
		qlProcessorAssignment.put("annis", AqlTree.class);
		qlProcessorAssignment.put("cql", CQLTree.class);
	}
	
	
    private Logger qllogger = KorAPLogger.initiate("ql");
    public static String queryLanguageVersion;

    private AbstractSyntaxTree ast;
    private Object collection;
    private Map meta;
    private List errors;
    private List warnings;
    private List messages;
    private org.slf4j.Logger log = LoggerFactory
            .getLogger(QuerySerializer.class);

    /**
     * @param args
     * @throws QueryException
     */
    public static void main(String[] args) {
        /*
         * just for testing...
		 */
        QuerySerializer jg = new QuerySerializer();
        int i = 0;
        String[] queries;
        if (args.length == 0) {
            queries = new String[]{
            };
        } else
            queries = new String[]{args[0]};
        
        for (String q : queries) {
            i++;
            try {
                System.out.println(q);
                String ql = "cosmas2";
                jg.setCollection("pubDate=2014");
                jg.run(q, ql, System.getProperty("user.home") + "/" + ql + "_" + i + ".jsonld");
                System.out.println();
            } catch (NullPointerException npe) {
                npe.printStackTrace();
                System.out.println("null\n");
            } catch (JsonGenerationException e) {
                e.printStackTrace();
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (QueryException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Runs the QuerySerializer by initializing the relevant AbstractSyntaxTree implementation (depending on specified query language)
     * and transforms and writes the tree's requestMap to the specified output file.
     *
     * @param outFile       The file to which the serialization is written
     * @param query         The query string
     * @param queryLanguage The query language. As of 13/11/20, this must be either 'poliqarp' or 'poliqarpplus'. Some extra maven stuff needs to done to support CosmasII ('cosmas') [that maven stuff would be to tell maven how to build the cosmas grammar and where to find the classes]
     * @throws IOException
     * @throws QueryException
     */
    public void run(String query, String queryLanguage, String outFile)
            throws IOException, QueryException {
        if (queryLanguage.equals("poliqarp")) {
            ast = new PoliqarpPlusTree(query);
        } else if (queryLanguage.toLowerCase().equals("cosmas2")) {
            ast = new CosmasTree(query);
        } else if (queryLanguage.toLowerCase().equals("poliqarpplus")) {
            ast = new PoliqarpPlusTree(query);
        } else if (queryLanguage.toLowerCase().equals("cql")) {
            ast = new CQLTree(query);
        } else if (queryLanguage.toLowerCase().equals("annis")) {
            ast = new AqlTree(query);
        } else {
            throw new QueryException(queryLanguage + " is not a supported query language!");
        }
        toJSON();
    }

    public QuerySerializer setQuery(String query, String ql, String version)
            throws QueryException {

        if (query == null || query.isEmpty())
            throw new QueryException(406, "No Content!");

        try {
            if (ql.equalsIgnoreCase("poliqarp")) {
                ast = new PoliqarpPlusTree(query);
            } else if (ql.equalsIgnoreCase("cosmas2")) {
                ast = new CosmasTree(query);
            } else if (ql.equalsIgnoreCase("poliqarpplus")) {
                ast = new PoliqarpPlusTree(query);
            } else if (ql.equalsIgnoreCase("cql")) {
                if (version == null)
                    ast = new CQLTree(query);
                else
                    ast = new CQLTree(query, version);
            } else if (ql.equalsIgnoreCase("annis")) {
                ast = new AqlTree(query);
            } else {
                throw new QueryException(ql + " is not a supported query language!");
            }
        } catch (QueryException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new QueryException("UNKNOWN: Query could not be parsed (" + query + ")");
        }
        return this;
    }

    public QuerySerializer setQuery(String query, String ql) throws QueryException {
        return setQuery(query, ql, "");
    }

    public final String toJSON() {
        String ser = JsonUtils.toJSON(raw());
        qllogger.info("Serialized query: " + ser);
        return ser;
    }

    public final Map build() {
        return raw();
    }

    private Map raw() {
        if (ast != null) {
            Map<String, Object> requestMap = ast.getRequestMap();
            Map meta = (Map) requestMap.get("meta");
            List errors = (List) requestMap.get("errors");
            List warnings = (List) requestMap.get("warnings");
            List messages = (List) requestMap.get("messages");
            if (collection != null)
                requestMap.put("collection", collection);
            if (this.meta != null) {
                meta.putAll(this.meta);
                requestMap.put("meta", meta);
            }
            if (this.errors != null) {
                errors.addAll(this.errors);
                requestMap.put("errors", errors);
            }
            if (this.warnings != null) {
                warnings.addAll(this.warnings);
                requestMap.put("warnings", warnings);
            }
            if (this.messages != null) {
            	messages.addAll(this.messages);
                requestMap.put("messages", messages);
            }
            

            return requestMap;
        }
        return new HashMap<>();
    }


    public QuerySerializer addMeta(
            String cli, String cri, int cls, int crs,
            int num, int pageIndex) {
        MetaQueryBuilder meta = new MetaQueryBuilder();
        meta.setSpanContext(cls, cli, crs, cri);
        meta.addEntry("startIndex", pageIndex);
        meta.addEntry("count", num);
        this.meta = meta.raw();
        return this;
    }

    public QuerySerializer addMeta(String context, int num, int pageidx) {
        MetaQueryBuilder meta = new MetaQueryBuilder();
        meta.addEntry("startIndex", pageidx);
        meta.addEntry("count", num);
        meta.setSpanContext(context);
        this.meta = meta.raw();
        return this;
    }

    public QuerySerializer addMeta(MetaQueryBuilder meta) {
        this.meta = meta.raw();
        return this;
    }

    @Deprecated
    public QuerySerializer setCollectionSimple(String collection) {
        CollectionQueryBuilder qobj = new CollectionQueryBuilder();
        qobj.addResource(collection);
        this.collection = qobj.raw();
        return this;
    }

    public QuerySerializer setCollection(String collection) throws QueryException {
        CollectionQueryTree tree = new CollectionQueryTree();
        Map collectionRequest = tree.getRequestMap();
        tree.process(collection);
        this.collection = collectionRequest.get("collection");
        this.errors = (List) collectionRequest.get("errors");
        this.warnings = (List) collectionRequest.get("warnings");
        this.messages = (List) collectionRequest.get("messages");
        return this;
    }

    public QuerySerializer setCollection(CollectionQueryBuilder2 collections) {
        this.collection = collections.raw();
        return this;
    }

    public QuerySerializer setDeprCollection(CollectionQueryBuilder collections) {
        this.collection = collections.raw();
        return this;
    }
}
