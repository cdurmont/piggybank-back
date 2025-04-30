package net.durmont.piggybank.service;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import net.durmont.piggybank.model.User;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class UserService {

	public Uni<List<User>> find(User filter, Sort sort, Page page) {
		return User.find(filter, sort, page);
	}

	public Uni<User> findById(Long id) {
		return User.findById(id);
	}

	public Uni<User> create(User newUser) {
		return newUser.create();
	}

	public Uni<User> update(User user) {
		return user.update();
	}

	public Uni<Boolean> delete(Long id) {
		return User.deleteById(id);
	}
}
