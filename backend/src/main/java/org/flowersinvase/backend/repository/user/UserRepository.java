package org.flowersinvase.backend.repository.user;

import org.flowersinvase.backend.entity.user.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean updatePassword(UUID id, String newPassword);

    boolean deleteById(UUID id);
}
