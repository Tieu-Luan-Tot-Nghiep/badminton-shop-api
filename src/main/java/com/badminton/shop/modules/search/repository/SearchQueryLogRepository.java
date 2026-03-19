package com.badminton.shop.modules.search.repository;

import com.badminton.shop.modules.search.document.SearchQueryLogDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchQueryLogRepository extends ElasticsearchRepository<SearchQueryLogDocument, String> {
}
