package com.rbownworkshop.server.repository;

import com.rbownworkshop.server.entity.ShopOrder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ShopOrderRepositoryImpl implements ShopOrderRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public void truncate() {
        String tableName = resolveTableName();
        // MySQL: disable FK checks tạm thời phòng có FK từ bảng khác trỏ tới.
        em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate();
        em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
    }

    /**
     * Đọc tên table thật từ annotation @Table trên entity ShopOrder.
     * Nếu không có @Table, dùng tên class (lowercased) theo naming strategy mặc định.
     */
    private String resolveTableName() {
        Table tableAnno = ShopOrder.class.getAnnotation(Table.class);
        if (tableAnno != null && !tableAnno.name().isBlank()) {
            return tableAnno.name();
        }
        // Fallback: Spring physical naming strategy sẽ chuyển ShopOrder -> shop_order
        return "shop_order";
    }
}