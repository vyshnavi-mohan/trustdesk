package com.trustdesk.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KnowledgeDocumentRepository
        extends JpaRepository<KnowledgeDocument, String> {

    List<KnowledgeDocument> findByCategory(String category);

    @Query(value = """
        SELECT *
        FROM knowledge_documents
        WHERE to_tsvector(
                  'english',
                  COALESCE(title, '') || ' ' || COALESCE(content, '')
              ) @@ plainto_tsquery('english', :keyword)
        ORDER BY
            ts_rank(
                to_tsvector(
                    'english',
                    COALESCE(title, '') || ' ' || COALESCE(content, '')
                ),
                plainto_tsquery('english', :keyword)
            ) DESC, LIMIT 5
            document_id
        """, nativeQuery = true)
    List<KnowledgeDocument> searchByKeyword(
            @Param("keyword") String keyword);

    @Query(value = """
        SELECT *
        FROM knowledge_documents
        WHERE to_tsvector(
                  'english',
                  COALESCE(title, '') || ' ' || COALESCE(content, '')
              ) @@ websearch_to_tsquery(
                  'english',
                  replace(:ticketText, ' ', ' OR ')
              )
        ORDER BY
            ts_rank(
                to_tsvector(
                    'english',
                    COALESCE(title, '') || ' ' || COALESCE(content, '')
                ),
                websearch_to_tsquery(
                    'english',
                    replace(:ticketText, ' ', ' OR ')
                )
            ) DESC,
            document_id
        LIMIT 5
        """, nativeQuery = true)
    List<KnowledgeDocument> searchByTicketText(
            @Param("ticketText") String ticketText);
}