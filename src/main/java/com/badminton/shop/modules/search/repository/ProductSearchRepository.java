package com.badminton.shop.modules.search.repository;

import com.badminton.shop.modules.search.document.ProductSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductSearchDocument, Long> {
}
