import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.FilterOperator;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Api(name = "graphql", version = "v1", namespace = @ApiNamespace(ownerDomain = "foo", ownerName = "foo"))
public class GraphQLController {

    // Datastore service for handling database interactions
    private final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    // API method to handle GraphQL queries
    @ApiMethod(name = "graphqlEndpoint", httpMethod = ApiMethod.HttpMethod.POST)
    public String graphqlEndpoint(String query) throws UnauthorizedException {
        return executeGraphQLQuery(query);
    }

    // Main method to handle GraphQL query execution
    private String executeGraphQLQuery(String query) {
        if (query.contains("RDFTriple")) {
            List<RDFTriple> rdfTriples = fetchRDFTriples(query);
            return formatResponse(rdfTriples); // Format response as needed
        }
        return "Invalid query or no results found";
    }

    // Method to construct the query and interact with Datastore
    private List<RDFTriple> fetchRDFTriples(String query) {
        Query q = new Query("RDFTriple");
    
          // Check for query parameters and apply the corresponding filters
        String subjectValue = extractQueryParameter(query, "subject");
        if (subjectValue != null) {
            q.setFilter(new Query.FilterPredicate("subject", Query.FilterOperator.EQUAL, subjectValue));
        }

        String predicateValue = extractQueryParameter(query, "predicate");
        if (predicateValue != null) {
            q.setFilter(new Query.FilterPredicate("predicate", Query.FilterOperator.EQUAL, predicateValue));
        }

        String objectValue = extractQueryParameter(query, "object");
        if (objectValue != null) {
            q.setFilter(new Query.FilterPredicate("object", Query.FilterOperator.EQUAL, objectValue));
        }

        String graphValue = extractQueryParameter(query, "graph");
        if (graphValue != null) {
            q.setFilter(new Query.FilterPredicate("graph", Query.FilterOperator.EQUAL, graphValue));
        }
        // Execute the query and fetch the results
        return executeQuery(q);
    }

    // Utility method to extract parameter values from the query string
    private String extractQueryParameter(String query, String param) {
        String regex = param + "=(\\w+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(query);
        return matcher.find() ? matcher.group(1) : null;
    }

    // Method to execute the query and fetch results from Datastore
    private List<RDFTriple> executeQuery(Query q) {
        List<RDFTriple> rdfTriples = new ArrayList<>();
        PreparedQuery pq = datastore.prepare(q);
        for (Entity result : pq.asIterable()) {
            String subject = (String) result.getProperty("subject");
            String predicate = (String) result.getProperty("predicate");
            String object = (String) result.getProperty("object");
            String graph = (String) result.getProperty("graph");
            rdfTriples.add(new RDFTriple(subject, predicate, object, graph));
        }
        return rdfTriples;
    }

    // Method to format RDFTriple list into a response string
    private String formatResponse(List<RDFTriple> rdfTriples) {
        StringBuilder response = new StringBuilder();
        for (RDFTriple triple : rdfTriples) {
            response.append("Subject: ").append(triple.getSubject())
                    .append(", Predicate: ").append(triple.getPredicate())
                    .append(", Object: ").append(triple.getObject())
                    .append(", Graph: ").append(triple.getGraph())
                    .append("\n");
        }
        return response.toString();
    }

    // RDFTriple class for holding the data
    public static class RDFTriple {
        private String subject;
        private String predicate;
        private String object;
        private String graph;

        public RDFTriple(String subject, String predicate, String object, String graph) {
            this.subject = subject;
            this.predicate = predicate;
            this.object = object;
            this.graph = graph;
        }

        public String getSubject() {
            return subject;
        }

        public String getPredicate() {
            return predicate;
        }

        public String getObject() {
            return object;
        }

        public String getGraph() {
            return graph;
        }
    }
}
