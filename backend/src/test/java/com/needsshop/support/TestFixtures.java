package com.needsshop.support;

import com.needsshop.product.Product;
import com.needsshop.product.ProductCategory;
import com.needsshop.product.ProductRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TestFixtures {

    private final ProductRepository productRepository;
    private final JdbcTemplate jdbc;

    public TestFixtures(ProductRepository productRepository, JdbcTemplate jdbc) {
        this.productRepository = productRepository;
        this.jdbc = jdbc;
    }

    public Product product(int stock) {
        return product("테스트 한정판 키보드", 10_000, stock);
    }

    public Product product(String name, long price, int stock) {
        return productRepository.save(new Product(name, ProductCategory.KEYBOARD, price, stock, "test"));
    }

    public int stockOf(Long productId) {
        return jdbc.queryForObject("select stock from product where id = ?", Integer.class, productId);
    }

    public int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }

    /** where 절은 테스트 코드에서만 넘기는 고정 문자열이다 */
    public int countWhere(String table, String where) {
        return jdbc.queryForObject("select count(*) from " + table + " where " + where, Integer.class);
    }

    public String orderStatus(long orderId) {
        return jdbc.queryForObject("select status from orders where id = ?", String.class, orderId);
    }

    public void clean() {
        jdbc.execute("delete from payment");
        jdbc.execute("delete from order_item");
        jdbc.execute("delete from orders");
        jdbc.execute("delete from idempotency_record");
        jdbc.execute("delete from activity_log");
    }
}
