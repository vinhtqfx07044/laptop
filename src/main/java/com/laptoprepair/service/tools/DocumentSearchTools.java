package com.laptoprepair.service.tools;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * RAG-powered document search tool using Spring AI's vector store. Accessible
 * to all users (no
 * authentication required).
 */
@Service
public class DocumentSearchTools {

    private static final Logger logger = LoggerFactory.getLogger(DocumentSearchTools.class);

    private final @NonNull VectorStore vectorStore;
    private final double similarityThreshold;
    private final int topK;

    public DocumentSearchTools(@NonNull VectorStore vectorStore,
            @Value("${laptoprepair.ai.search.document.similarity-threshold:0.6}") double similarityThreshold,
            @Value("${laptoprepair.ai.search.document.top-k:5}") int topK) {

        this.vectorStore = vectorStore;
        this.similarityThreshold = similarityThreshold;
        this.topK = topK;

        logger.info("Initializing DocumentSearchTools with simplified RAG pipeline:");
        logger.info("  - similarityThreshold: {}", similarityThreshold);
        logger.info("  - topK: {}", topK);
        logger.info("DocumentSearchTools RAG pipeline ready with vector search!");
    }

    /**
     * Search document knowledge base using vector similarity search.
     *
     * @param query Search query from user
     * @return Raw document data with metadata for LLM to process
     */
    @Tool(description = "Search document knowledge base using natural language query. "
            + "Use this for questions about shop policies, procedures, warranties, and guidelines. "
            + "Returns raw document data. LLM should synthesize and summarize based on system prompt.")
    public String searchDocuments(
            @ToolParam(description = "Natural language search query") @NonNull String query) {
        try {
            long startTime = System.currentTimeMillis();
            logger.info("=== Starting RAG pipeline ===");
            logger.info("Query: \"{}\"", query);

            // Direct document retrieval with vector search
            logger.debug("Step 1: Document Retrieval");
            DocumentRetriever retriever = VectorStoreDocumentRetriever.builder().vectorStore(vectorStore)
                    .similarityThreshold(similarityThreshold).topK(topK).build();

            // Retrieve documents directly
            Query searchQuery = new Query(query);
            List<Document> finalDocuments = retriever.retrieve(searchQuery);
            logger.info("  → Retrieved {} documents", finalDocuments.size());

            long duration = System.currentTimeMillis() - startTime;
            logger.info("=== RAG pipeline completed in {}ms ===", duration);
            logger.info("  → Retrieved {} documents", finalDocuments.size());

            if (finalDocuments.isEmpty()) {
                logger.warn("No documents found for query: \"{}\"", query);
                return "Không tìm thấy tài liệu liên quan đến câu hỏi của bạn.";
            }

            // Format results for LLM to synthesize
            return formatDocuments(finalDocuments);

        } catch (Exception e) {
            logger.error("Error in RAG pipeline for query \"{}\": {}", query, e.getMessage(), e);
            return "Không thể tìm kiếm tài liệu. Vui lòng thử lại sau. Lỗi: " + e.getMessage();
        }
    }

    /**
     * Format documents for LLM to process. Returns plain text with document content
     * and relevant
     * metadata for LLM to synthesize into natural language response.
     *
     * @param documents List of documents to format
     * @return Formatted document data as string
     */
    private String formatDocuments(List<Document> documents) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tìm thấy ").append(documents.size()).append(" tài liệu liên quan:\n\n");

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);

            sb.append("=== Tài liệu ").append(i + 1).append(" ===\n");

            // Extract and display useful metadata
            Map<String, Object> metadata = doc.getMetadata();
            if (!metadata.isEmpty()) {
                // Display source information for traceability
                if (metadata.containsKey("documentId")) {
                    sb.append("Document ID: ").append(metadata.get("documentId")).append("\n");
                }

                if (metadata.containsKey("sourceFile")) {
                    String sourceFile = metadata.get("sourceFile").toString();
                    // Extract just the filename for readability
                    String fileName = sourceFile.substring(sourceFile.lastIndexOf('/') + 1);
                    sb.append("Nguồn: ").append(fileName).append("\n");
                }

                sb.append("\n");
            }

            sb.append("Nội dung:\n");
            sb.append(doc.getText()).append("\n\n");
        }

        return sb.toString();
    }
}
