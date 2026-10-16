package Ellithium.core.DB;

import Ellithium.core.logging.LogLevel;
import Ellithium.core.reporting.Reporter;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.conversions.Bson;
import java.util.Optional;

/**
 * Provides robust operations for MongoDB database access.
 * Supports document operations, queries, and health checks.
 * Queries are always executed live against the database to guarantee test integrity.
 */
public class MongoDatabaseProvider implements NoSQLDatabaseProvider {

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final String defaultCollectionName;

    /**
     * Primary constructor for production use.
     * Constructs a MongoDatabaseProvider with the specified connection details.
     *
     * @param connectionString the MongoDB connection string
     * @param databaseName     the database name
     */
    public MongoDatabaseProvider(String connectionString, String databaseName) {
        this(connectionString, databaseName, "documents");
    }

    /**
     * Primary constructor for production use with custom default collection.
     * Constructs a MongoDatabaseProvider with the specified connection details and default collection.
     *
     * @param connectionString     the MongoDB connection string
     * @param databaseName         the database name
     * @param defaultCollectionName the default collection name for NoSQL operations
     */
    public MongoDatabaseProvider(String connectionString, String databaseName, String defaultCollectionName) {
        Reporter.log("Initializing MongoDB connection to " + redactConnectionString(connectionString), LogLevel.INFO_YELLOW);
        this.mongoClient = MongoClients.create(connectionString);
        this.database = mongoClient.getDatabase(databaseName);
        this.defaultCollectionName = defaultCollectionName;
        Reporter.log("MongoDB connection initialized successfully", LogLevel.INFO_YELLOW);
    }

    /**
     * @deprecated Caching has been removed from database providers. Use {@link #MongoDatabaseProvider(String, String)} instead.
     */
    @Deprecated
    public MongoDatabaseProvider(String connectionString, String databaseName, long cacheTtlMinutes, long cacheMaxSize) {
        this(connectionString, databaseName, "documents");
    }

    /**
     * @deprecated Caching has been removed from database providers. Use {@link #MongoDatabaseProvider(String, String, String)} instead.
     */
    @Deprecated
    public MongoDatabaseProvider(String connectionString, String databaseName, long cacheTtlMinutes, long cacheMaxSize, String defaultCollectionName) {
        this(connectionString, databaseName, defaultCollectionName);
    }

    /**
     * Constructor for dependency injection, primarily for testing purposes.
     *
     * @param mongoClient the {@link MongoClient} instance to be used by this provider.
     * @param database    the {@link MongoDatabase} instance to be used for database operations.
     */
    public MongoDatabaseProvider(MongoClient mongoClient, MongoDatabase database) {
        this(mongoClient, database, "documents");
    }

    /**
     * Constructor for dependency injection with custom default collection.
     *
     * @param mongoClient           the {@link MongoClient} instance to be used by this provider.
     * @param database              the {@link MongoDatabase} instance to be used for database operations.
     * @param defaultCollectionName the default collection name for NoSQL operations.
     */
    public MongoDatabaseProvider(MongoClient mongoClient, MongoDatabase database, String defaultCollectionName) {
        if (mongoClient == null || database == null) {
            throw new IllegalArgumentException("MongoClient and MongoDatabase cannot be null when using this constructor for dependency injection.");
        }
        this.mongoClient = mongoClient;
        this.database = database;
        this.defaultCollectionName = defaultCollectionName;
        Reporter.log("MongoDB connection initialized successfully", LogLevel.INFO_YELLOW);
    }

    /**
     * Executes a MongoDB query live against the database.
     * This method implements the NoSQLDatabaseProvider interface.
     *
     * @param query the query to execute
     * @return the query result, or null if not found
     */
    @Override
    public Object executeQuery(String query) {
        Reporter.log("Executing MongoDB query: " + query, LogLevel.INFO_YELLOW);
        try {
            Document command = Document.parse(query);
            return database.runCommand(command);
        } catch (Exception e) {
            Reporter.log("Failed to execute query: " + query + ". Error: " + e.getMessage(), LogLevel.ERROR);
            return null;
        }
    }

    /**
     * Closes the MongoDB connection.
     * This method implements the NoSQLDatabaseProvider interface.
     */
    @Override
    public void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
            Reporter.log("MongoDB connection closed.", LogLevel.INFO_YELLOW);
        }
    }

    /**
     * @deprecated Caching has been removed from database providers; queries are always executed live against the database.
     */
    @Deprecated
    @Override
    public void clearCache(String key) {
        // No-op: caching is disabled; all queries execute live against the database.
    }

    /**
     * @deprecated Caching has been removed from database providers; queries are always executed live against the database.
     */
    @Deprecated
    @Override
    public void clearAllCaches() {
        // No-op: caching is disabled; all queries execute live against the database.
    }

    /**
     * @deprecated Caching has been removed from database providers; queries are always executed live against the database.
     */
    @Deprecated
    @Override
    public void addToCache(String key, Object value) {
        // No-op: caching is disabled; all queries execute live against the database.
    }

    /**
     * @deprecated Caching has been removed from database providers; queries are always executed live against the database.
     */
    @Deprecated
    @Override
    public Object getFromCache(String key) {
        return null;
    }

    /**
     * Performs a health check against the MongoDB server.
     * This method implements the NoSQLDatabaseProvider interface.
     *
     * @return true if the connection is healthy, false otherwise
     */
    @Override
    public boolean isHealthy() {
        try {
            Reporter.log("Performing health check", LogLevel.INFO_YELLOW);
            database.runCommand(new Document("ping", 1));
            Reporter.log("Health check completed. Status: Healthy", LogLevel.INFO_YELLOW);
            return true;
        } catch (Exception e) {
            Reporter.log("Health check failed! Error: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    /**
     * Checks if a document exists.
     * This method implements the NoSQLDatabaseProvider interface.
     *
     * @param key the document ID to check
     * @return true if the document exists, false otherwise
     */
    @Override
    public boolean exists(String key) {
        try {
            Reporter.log("Checking existence of document: " + key, LogLevel.INFO_YELLOW);
            boolean result = database.getCollection(defaultCollectionName).countDocuments(new Document("_id", key)) > 0;
            Reporter.log("Document existence check completed", LogLevel.INFO_YELLOW);
            return result;
        } catch (Exception e) {
            Reporter.log("Failed to check document existence: " + key + ". Error: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    /**
     * Deletes a document.
     * This method implements the NoSQLDatabaseProvider interface.
     *
     * @param key the document ID to delete
     * @return true if the document was deleted, false otherwise
     */
    @Override
    public boolean delete(String key) {
        try {
            Reporter.log("Deleting document: " + key, LogLevel.INFO_YELLOW);
            long result = database.getCollection(defaultCollectionName).deleteOne(new Document("_id", key)).getDeletedCount();
            Reporter.log("Document deleted successfully", LogLevel.INFO_YELLOW);
            return result > 0;
        } catch (Exception e) {
            Reporter.log("Failed to delete document: " + key + ". Error: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    /**
     * Sets an expiration time on a document.
     * This method implements the NoSQLDatabaseProvider interface.
     *
     * @param key     the document ID
     * @param seconds the expiration time in seconds
     * @return true if the expiration was set, false otherwise
     */
    @Override
    public boolean expire(String key, int seconds) {
        try {
            Reporter.log("Setting expiry for document: " + key + " to " + seconds + " seconds", LogLevel.INFO_YELLOW);
            database.getCollection(defaultCollectionName).updateOne(
                new Document("_id", key),
                new Document("$set", new Document("expiresAt", System.currentTimeMillis() + (seconds * 1000L)))
            );
            Reporter.log("Expiry set successfully", LogLevel.INFO_YELLOW);
            return true;
        } catch (Exception e) {
            Reporter.log("Failed to set expiry for document: " + key + ". Error: " + e.getMessage(), LogLevel.ERROR);
            return false;
        }
    }

    // MongoDB-specific methods below

    /**
     * Gets a document by ID.
     *
     * @param collectionName the collection name
     * @param docId         the document ID
     * @return an Optional containing the document if found, empty otherwise
     */
    public Optional<Document> getDocument(String collectionName, String docId) {
        try {
            Reporter.log("Getting document: " + docId + " from collection: " + collectionName, LogLevel.INFO_YELLOW);
            Document result = database.getCollection(collectionName).find(new Document("_id", docId)).first();
            Reporter.log("Document retrieved successfully", LogLevel.INFO_YELLOW);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            Reporter.log("Failed to get document: " + docId + ". Error: " + e.getMessage(), LogLevel.ERROR);
            return Optional.empty();
        }
    }

    /**
     * Inserts a new document.
     *
     * @param collectionName the collection name
     * @param document      the document to insert
     * @throws RuntimeException if the operation fails
     */
    public void insertDocument(String collectionName, Document document) {
        try {
            Reporter.log("Inserting document into collection: " + collectionName, LogLevel.INFO_YELLOW);
            database.getCollection(collectionName).insertOne(document);
            Reporter.log("Document inserted successfully", LogLevel.INFO_YELLOW);
        } catch (Exception e) {
            Reporter.log("Failed to insert document. Error: " + e.getMessage(), LogLevel.ERROR);
            throw new NoSQLRuntimeException("Failed to insert document", e);
        }
    }

    /**
     * Updates a document.
     *
     * @param collectionName the collection name
     * @param docId         the document ID
     * @param update        the update operation
     * @throws NoSQLRuntimeException if the operation fails
     */
    public void updateDocument(String collectionName, String docId, Bson update) {
        try {
            Reporter.log("Updating document: " + docId + " in collection: " + collectionName, LogLevel.INFO_YELLOW);
            database.getCollection(collectionName).updateOne(new Document("_id", docId), update);
            Reporter.log("Document updated successfully", LogLevel.INFO_YELLOW);
        } catch (Exception e) {
            Reporter.log("Failed to update document: " + docId + ". Error: " + e.getMessage(), LogLevel.ERROR);
            throw new NoSQLRuntimeException("Failed to update document: " + docId, e);
        }
    }

    /**
     * Deletes a document.
     *
     * @param collectionName the collection name
     * @param docId         the document ID
     * @throws NoSQLRuntimeException if the operation fails
     */
    public void deleteDocument(String collectionName, String docId) {
        try {
            Reporter.log("Deleting document: " + docId + " from collection: " + collectionName, LogLevel.INFO_YELLOW);
            database.getCollection(collectionName).deleteOne(new Document("_id", docId));
            Reporter.log("Document deleted successfully", LogLevel.INFO_YELLOW);
        } catch (Exception e) {
            Reporter.log("Failed to delete document: " + docId + ". Error: " + e.getMessage(), LogLevel.ERROR);
            throw new NoSQLRuntimeException("Failed to delete document: " + docId, e);
        }
    }

    /**
     * Creates an index on a collection.
     *
     * @param collectionName the collection name
     * @param field         the field to index
     * @param unique        whether the index should be unique
     * @throws NoSQLRuntimeException if the operation fails
     */
    public void createIndex(String collectionName, String field, boolean unique) {
        try {
            Reporter.log("Creating index on field: " + field + " in collection: " + collectionName, LogLevel.INFO_YELLOW);
            IndexOptions options = new IndexOptions().unique(unique);
            database.getCollection(collectionName).createIndex(Indexes.ascending(field), options);
            Reporter.log("Index created successfully", LogLevel.INFO_YELLOW);
        } catch (Exception e) {
            Reporter.log("Failed to create index. Error: " + e.getMessage(), LogLevel.ERROR);
            throw new NoSQLRuntimeException("Failed to create index", e);
        }
    }

    /**
     * Gets a collection by name.
     *
     * @param collectionName the collection name
     * @return the collection
     */
    public MongoCollection<Document> getCollection(String collectionName) {
        return database.getCollection(collectionName);
    }

    private static String redactConnectionString(String connectionString) {
        if (connectionString == null) return "null";
        return connectionString.replaceAll("://[^@]+@", "://<redacted>@");
    }
}
