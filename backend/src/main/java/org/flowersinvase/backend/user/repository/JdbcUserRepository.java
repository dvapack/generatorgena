package org.flowersinvase.backend.user.repository;

import org.flowersinvase.backend.user.entity.User;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcUserRepository implements UserRepository {

    private static final RowMapper<User> ROW_MAPPER =
            (resultSet, rowNumber) -> new User(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getString("email"),
                    resultSet.getString("password_hash")
            );

    private final JdbcClient jdbcClient;

    public JdbcUserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }


    @Override
    public User save(User user) {
        return jdbcClient.sql("""
                insert into users (id, email, password_hash)
                values (:id, :email, :passwordHash)
                returning id, email, password_hash
                """)
                .param("id", user.id())
                .param("email", user.email())
                .param("passwordHash", user.passwordHash())
                .query(ROW_MAPPER)
                .single();
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jdbcClient.sql("""
                select id, email, password_hash
                from users
                where id = :id
                """)
                .param("id", id)
                .query(ROW_MAPPER)
                .optional();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jdbcClient.sql("""
                select id, email, password_hash
                from users
                where email = :email
                """)
                .param("email", email)
                .query(ROW_MAPPER)
                .optional();
    }

    @Override
    public boolean existsByEmail(String email) {
        return jdbcClient.sql("""
                select exists(
                    select 1
                    from users
                    where email = :email
                )
                """)
                .param("email", email)
                .query(Boolean.class)
                .single();
    }

    @Override
    public boolean updatePassword(UUID id, String newPassword) {
        int affectedRows = jdbcClient.sql("""
                update users
                set password_hash = :newPassword
                where id = :id
                """)
                .param("id", id)
                .param("newPassword", newPassword)
                .update();
        return affectedRows == 1;
    }

    @Override
    public boolean deleteById(UUID id) {
        int affectedRows = jdbcClient.sql("""
                delete from users
                where id = :id
                """)
                .param("id", id)
                .update();

        return affectedRows == 1;
    }
}
