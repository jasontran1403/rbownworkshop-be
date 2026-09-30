package com.rbownworkshop.server.repository;

import com.rbownworkshop.server.entity.ShopOrder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.util.List;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long>, ShopOrderRepositoryCustom {

    @Query("SELECT s FROM ShopOrder s WHERE LOWER(s.account) = LOWER(:account)")
    List<ShopOrder> findByAccountIgnoreCase(@Param("account") String account);

    @Query("SELECT s FROM ShopOrder s WHERE LOWER(s.account) = LOWER(:account) AND s.sheetType = :sheetType")
    List<ShopOrder> findByAccountAndSheetType(@Param("account") String account,
                                              @Param("sheetType") String sheetType);

    @Query("SELECT COALESCE(MAX(s.sheetSequence), 0) FROM ShopOrder s WHERE s.sheetType = :sheetType")
    Long findMaxSheetSequence(@Param("sheetType") String sheetType);

    @Query("SELECT COALESCE(MAX(s.id), 0) FROM ShopOrder s")
    Long findMaxId();

    @Query("""
        SELECT s FROM ShopOrder s
        WHERE (:keyword IS NULL OR LOWER(s.account) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:sheetType IS NULL OR s.sheetType = :sheetType)
        ORDER BY s.id DESC
    """)
    List<ShopOrder> searchForManagement(@Param("keyword") String keyword,
                                        @Param("sheetType") String sheetType);

    @Query("""
        SELECT s FROM ShopOrder s
        WHERE (:keyword IS NULL OR LOWER(s.account) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:sheetType IS NULL OR s.sheetType = :sheetType)
        ORDER BY s.id DESC
    """)
    List<ShopOrder> searchForManagementPaged(@Param("keyword") String keyword,
                                             @Param("sheetType") String sheetType,
                                             Pageable pageable);

    @Query("SELECT DISTINCT s.sheetType FROM ShopOrder s WHERE s.sheetType IS NOT NULL")
    List<String> findDistinctSheetTypes();
}